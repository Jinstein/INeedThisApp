package com.example.memokeyword

import android.app.AlertDialog
import android.content.Intent
import android.database.ContentObserver
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.memokeyword.data.AppDatabase
import com.example.memokeyword.databinding.ActivityMainBinding
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.service.EdgePanelService
import com.example.memokeyword.viewmodel.MemoViewModel
import com.example.memokeyword.viewmodel.MemoViewModelFactory
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewModel: MemoViewModel
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var screenshotObserver: ContentObserver? = null

    companion object {
        const val ACTION_NEW_MEMO = "com.example.memokeyword.ACTION_NEW_MEMO"
        const val ACTION_SCREENSHOT_MEMO = "com.example.memokeyword.ACTION_SCREENSHOT_MEMO"
        const val EXTRA_SCREENSHOT_URI = "extra_screenshot_uri"

        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }

    // MediaProjection 권한 요청 런처
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            EdgePanelService.startService(this, result.resultCode, result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.memoListFragment, R.id.searchFragment)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        val db = AppDatabase.getDatabase(this)
        val factory = MemoViewModelFactory(
            MemoRepository(db.memoDao(), db.keywordDao(), db.memoPhotoDao())
        )
        viewModel = ViewModelProvider(this, factory)[MemoViewModel::class.java]

        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        registerScreenshotObserver()
        handleIncomingIntent(intent)
        checkAndRequestOverlayPermission()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            ACTION_NEW_MEMO -> navigateToNewMemo()
            ACTION_SCREENSHOT_MEMO -> {
                val uriString = intent.getStringExtra(EXTRA_SCREENSHOT_URI)
                if (uriString != null) {
                    navigateToMemoWithScreenshot(Uri.parse(uriString))
                }
            }
        }
    }

    private fun navigateToNewMemo() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController.navigate(
            R.id.memoEditFragment,
            bundleOf("memoId" to 0L)
        )
    }

    private fun navigateToMemoWithScreenshot(uri: Uri) {
        viewModel.setPendingScreenshot(uri)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController.navigate(
            R.id.memoEditFragment,
            bundleOf("memoId" to 0L)
        )
    }

    // ─── 엣지 패널 권한 처리 ────────────────────────────────────────

    private fun checkAndRequestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            requestMediaProjectionPermission()
        } else {
            showOverlayPermissionDialog()
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.overlay_permission_title))
            .setMessage(getString(R.string.overlay_permission_message))
            .setPositiveButton(getString(R.string.overlay_permission_go_settings)) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
            .setNegativeButton(getString(R.string.overlay_permission_cancel), null)
            .show()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                requestMediaProjectionPermission()
            }
        }
    }

    private fun requestMediaProjectionPermission() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }

    // ─── 기존 스크린샷 ContentObserver ────────────────────────────

    private fun registerScreenshotObserver() {
        val handler = Handler(Looper.getMainLooper())
        screenshotObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                uri ?: return
                handler.post { handleNewImage(uri) }
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }

    private fun handleNewImage(uri: Uri) {
        if (!isRecentScreenshot(uri)) return
        showScreenshotSaveBanner(uri)
    }

    private fun isRecentScreenshot(uri: Uri): Boolean {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
        } else {
            @Suppress("DEPRECATION")
            arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )
        }

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateAdded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                )
                val nowSec = System.currentTimeMillis() / 1000
                if (nowSec - dateAdded > 10) return false

                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                    ) ?: ""
                    val displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    ) ?: ""
                    relativePath.lowercase().contains("screenshot") ||
                            displayName.lowercase().contains("screenshot")
                } else {
                    @Suppress("DEPRECATION")
                    val data = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    ) ?: ""
                    data.lowercase().contains("screenshot")
                }
            }
        }
        return false
    }

    private fun showScreenshotSaveBanner(screenshotUri: Uri) {
        Snackbar.make(binding.root, "스크린샷을 메모로 저장할까요?", Snackbar.LENGTH_LONG)
            .setAction("메모로 저장") {
                navigateToMemoWithScreenshot(screenshotUri)
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }
}
