package com.example.memokeyword.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.memokeyword.MainActivity
import com.example.memokeyword.R

class EdgePanelService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var tabView: View? = null
    private var panelView: View? = null
    private var tabParams: WindowManager.LayoutParams? = null
    private var isPanelOpen = false
    private var mediaProjection: MediaProjection? = null

    companion object {
        const val CHANNEL_ID = "EdgePanelChannel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        fun startService(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, EdgePanelService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, EdgePanelService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val resultCode = it.getIntExtra(EXTRA_RESULT_CODE, -1)
            val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra(EXTRA_RESULT_DATA)
            }
            if (resultCode != -1 && resultData != null) {
                mediaProjection?.stop()
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            }
        }
        if (tabView == null) {
            setupEdgeTab()
        }
        return START_STICKY
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun setupEdgeTab() {
        val inflater = LayoutInflater.from(this)
        val tab = inflater.inflate(R.layout.edge_panel_tab, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 0
            y = 0
        }

        var initialY = 0
        var initialTouchY = 0f
        var dragged = false

        tab.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    dragged = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!dragged && Math.abs(dy) > 8) dragged = true
                    if (dragged) {
                        params.y = initialY + dy
                        windowManager.updateViewLayout(tab, params)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) {
                        if (isPanelOpen) closePanel() else openPanel()
                    }
                    false
                }
                else -> false
            }
        }

        tabView = tab
        tabParams = params
        windowManager.addView(tab, params)
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        val inflater = LayoutInflater.from(this)
        val panel = inflater.inflate(R.layout.edge_panel, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        panel.findViewById<View>(R.id.btn_new_memo).setOnClickListener {
            closePanel()
            openMemoEdit()
        }

        panel.findViewById<View>(R.id.btn_screenshot).setOnClickListener {
            closePanel()
            // 패널이 닫힌 뒤 화면 캡처
            Handler(Looper.getMainLooper()).postDelayed({ takeScreenshot() }, 400)
        }

        panel.findViewById<View>(R.id.btn_close_panel).setOnClickListener {
            closePanel()
        }

        panelView = panel
        windowManager.addView(panel, params)
    }

    private fun closePanel() {
        isPanelOpen = false
        panelView?.let {
            windowManager.removeView(it)
            panelView = null
        }
    }

    private fun openMemoEdit() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_NEW_MEMO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun takeScreenshot() {
        val projection = mediaProjection
        if (projection == null) {
            Toast.makeText(this, getString(R.string.edge_panel_no_permission), Toast.LENGTH_SHORT).show()
            return
        }

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = projection.createVirtualDisplay(
            "EdgePanelCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val rawBitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
                    )
                    rawBitmap.copyPixelsFromBuffer(buffer)
                    val bitmap = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                    rawBitmap.recycle()
                    image.close()

                    saveBitmapAndOpenMemo(bitmap)
                } catch (e: Exception) {
                    image.close()
                    Toast.makeText(this, getString(R.string.edge_panel_screenshot_failed), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.edge_panel_screenshot_failed), Toast.LENGTH_SHORT).show()
            }
            virtualDisplay.release()
            imageReader.close()
        }, 300)
    }

    private fun saveBitmapAndOpenMemo(bitmap: Bitmap) {
        val filename = "memo_screenshot_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MemoKeyword")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            bitmap.recycle()
            Toast.makeText(this, getString(R.string.edge_panel_screenshot_failed), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
        } finally {
            bitmap.recycle()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SCREENSHOT_MEMO
            putExtra(MainActivity.EXTRA_SCREENSHOT_URI, uri.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.edge_panel_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.edge_panel_notification_desc)
            }
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.edge_panel_notification_text))
            .setSmallIcon(R.drawable.ic_edge_memo)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        closePanel()
        tabView?.let { windowManager.removeView(it) }
        tabView = null
        mediaProjection?.stop()
        mediaProjection = null
    }
}
