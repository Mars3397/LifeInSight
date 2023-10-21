package com.example.meichu

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager


class GlobalTouchInterceptor : Service() {
    private var windowManager: WindowManager? = null
    private var touchView: View? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        touchView = View(this)

        windowManager?.addView(touchView, params)
        touchView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 在这里处理全局点击事件
                // event.x 和 event.y 包含了点击的坐标
                Log.d("global touch", "global touch at ${event.x}, ${event.y}")
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (touchView != null) {
            windowManager?.removeView(touchView)
        }
    }
}
