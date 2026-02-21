package com.example.memokeyword

import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.core.os.bundleOf
import com.example.memokeyword.data.AppDatabase
import com.example.memokeyword.databinding.ActivityMainBinding
import com.example.memokeyword.repository.MemoRepository
import com.example.memokeyword.viewmodel.MemoViewModel
import com.example.memokeyword.viewmodel.MemoViewModelFactory
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewModel: MemoViewModel

    private var screenshotObserver: ContentObserver? = null

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

        // ViewModel 초기화 (Fragment들과 동일한 인스턴스 공유)
        val db = AppDatabase.getDatabase(this)
        val factory = MemoViewModelFactory(
            MemoRepository(db.memoDao(), db.keywordDao(), db.memoPhotoDao())
        )
        viewModel = ViewModelProvider(this, factory)[MemoViewModel::class.java]

        registerScreenshotObserver()
    }

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
                if (nowSec - dateAdded > 10) return false // 10초 이내에 추가된 이미지만

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
                viewModel.setPendingScreenshot(screenshotUri)

                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                // 새 메모 작성 화면으로 이동 (memoId = 0 → 신규 메모)
                navController.navigate(
                    R.id.memoEditFragment,
                    bundleOf("memoId" to 0L)
                )
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
