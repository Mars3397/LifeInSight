package com.example.meichu


import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

class MyActivity : Activity() {
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gestureDetector = GestureDetector(this, MyGestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            // 用户按下屏幕时触发
            Log.d("my_act", "press")
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // 用户进行快速滑动手势时触发
            Log.d("my_act", "fling")
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            // 用户长按屏幕时触发'
            Log.d("my_act", "long press")
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // 用户单次点击屏幕时触发
            Log.d("my_act", "single click")
            return true
        }
    }
}
