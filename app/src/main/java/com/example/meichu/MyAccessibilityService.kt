package com.example.meichu

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.speech.RecognizerIntent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.util.LinkedList
import kotlin.math.log

class MyAccessibilityService : AccessibilityService() {


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {


        if (event == null) return
        val rootNode = rootInActiveWindow ?: return
        Log.d("MyAccessibilityService", "Event Type: ${event.eventType}")

        if (rootNode != null) {
            // 输出根节点信息
           Log.d("node_info", "Root Node Info: $rootNode")
            // 遍历根节点的子节点
            for (i in 0 until rootNode.childCount) {
                val childNode = rootNode.getChild(i)
                // 输出子节点信息
                Log.d("node_info", "Child Node $i Info: $childNode")
            }
        }
        Log.d("node_info", "Root Node Info: $rootNode")
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode != null) {
            // 获取焦点 View 的相关信息
            val viewIdResourceName = focusedNode.viewIdResourceName
            val className = focusedNode.className
            val text = focusedNode.text
            val contentDescription = focusedNode.contentDescription

            // 输出到日志
            Log.d("FocusInfo", "View ID: $viewIdResourceName, Class Name: $className, Text: $text, Content Description: $contentDescription")
        }
        else{
            Log.d("FocusInfo", "focus node null")
           // val speakButton = rootInActiveWindow.findAccessibilityNodeInfosByViewId("button2")
            //if (speakButton.isNotEmpty()) {
            //    val speakNode = speakButton[0]
                // 设置焦点到 "Speak" 按钮
               // speakNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
               // val viewIdResourceName = rootInActiveWindow.viewIdResourceName
               // if (viewIdResourceName != null) {
                    //Log.d("AccessibilityFocus", "Current focus ID: $viewIdResourceName")
              //  }
           // }
        }


        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val packageName = event.packageName?.toString()
            val className = event.className?.toString()

            // 判断是否进入了目标应用程序的页面
            if ("com.example.meichu" == packageName && "MainActivity" == className) {
                val nodeInfo = rootInActiveWindow
                if (nodeInfo != null) {
                    // 查找 "Speak" 按钮
                    val speakButton = nodeInfo.findAccessibilityNodeInfosByViewId("button2")
                    if (speakButton.isNotEmpty()) {
                        val speakNode = speakButton[0]
                        // 设置焦点到 "Speak" 按钮
                        speakNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val viewIdResourceName = nodeInfo.viewIdResourceName
                        if (viewIdResourceName != null) {
                            Log.d("AccessibilityFocus", "Current focus ID: $viewIdResourceName")
                        }
                    }
                }
            }
        }
    }







    override fun onInterrupt() {
        // 在服务被中断时执行清理操作
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val doubleClickFilter = IntentFilter("com.example.meichu.DOUBLE_CLICKED")
        val singleClickFilter = IntentFilter("com.example.meichu.SINGLE_CLICKED")
        val longPressedFilter = IntentFilter("com.example.meichu.LONG_PRESSED")
        val rightSwipeFilter = IntentFilter("com.example.meichu.RIGHT_SWIPE")
        val leftSwipeFilter = IntentFilter("com.example.meichu.LEFT_SWIPE")

        registerReceiver(customReceiver, doubleClickFilter)
        registerReceiver(customReceiver, singleClickFilter)
        registerReceiver(customReceiver, longPressedFilter)
        registerReceiver(customReceiver, leftSwipeFilter)
        registerReceiver(customReceiver, rightSwipeFilter)
        // 在服务连接时执行初始化操作
    }
    //var cur_focus = "com.example.meichu:id/editText"
    var last_focus: String  = "com.example.meichu:id/editText"
    var cur_focus: String  = "com.example.meichu:id/button2"
    var next_focus: String  = "com.example.meichu:id/button"

    var permit_margins = floatArrayOf(0.0F, 0.0F, 0.0F, 0.0F)



    private val customReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val any = when (intent?.action) {
                "com.example.meichu.DOUBLE_CLICKED" -> {
                    Log.d("screen_action", "Received DOUBLE_CLICKED Intent")

                    val rootNode = rootInActiveWindow
                    val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    //Log.d("node_info", "Double CLICK, Root Node Info: $rootNode")
                    val cur_but = rootNode.findAccessibilityNodeInfosByViewId(cur_focus)
                    if (cur_but.isNotEmpty()) {
                        val cur = cur_but[0]
                        Log.d("checkkkkkkkkkkkk", "Current focus : $cur")
                        // 设置焦点到 "permit" 按钮
                        val focusSuccess = cur.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val viewIdResourceName = rootNode.viewIdResourceName

                        if (viewIdResourceName != null) {
                            Log.d("AccessibilityFocus", "Current focus ID: $viewIdResourceName")
                        }
                        if (focusSuccess) {
                            Log.d("Focus!", "焦点成功设置在 ${cur_focus}上")
                            val clickSuccess = cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                            if (clickSuccess) {
                                Log.d("AccessibilityClick", "点击操作成功")
                            } else {
                                Log.d("AccessibilityClick", "点击操作失败")
                            }
                        } else {
                            Log.d("Focus!", "焦点设置失败")
                        }
                    }
                    else{
                        Log.d("..", "fail")
                    }

                }

                "com.example.meichu.SINGLE_CLICKED" -> {
                    Log.d("screen_action", "Received SINGLE_CLICKED Intent")
                    //val childNode1 = rootNode.getChild(1)


                    val tmp_x = intent.getFloatExtra("x_val", 0.0F)
                    val tmp_y = intent.getFloatExtra("y_val", 0.0F)
                    val rootNode = rootInActiveWindow
                    val buttonID = "com.example.meichu:id/editText"  // 替换为实际按钮文本
                    val buttonNode = findButtonNodeById(rootNode, buttonID)
                    //Log.d("margin on enter text", "$tmp_x,$tmp_y -> ${permit_margins[0]},${permit_margins[1]},${permit_margins[2]},${permit_margins[3]},")
                   // if(tmp_x<permit_margins[1] && tmp_x>permit_margins[0] && tmp_y>permit_margins[2] && tmp_x<permit_margins[3]){
                        // click permit
                      //  next_focus = "com.example.meichu:id/editText"
                    //}
                    //else{
                    //    next_focus = decide_next(cur_focus)
                   // }
                    if (buttonNode != null) {
                        Log.d("margin!", "innnnnnnnnn")
                        val boundsInScreen = Rect()
                        buttonNode.getBoundsInScreen(boundsInScreen)
                        val Left = boundsInScreen.left
                        val Top = boundsInScreen.top
                        val Right = boundsInScreen.right
                        val Bottom = boundsInScreen.bottom
                        Log.d("margin on enter text", "$tmp_x,$tmp_y -> ${Left},${Right},${Top},${Bottom},")
                         if(tmp_x<Right && tmp_x>Left && tmp_y<Bottom && tmp_y>Top){
                        // click permit
                          next_focus = "com.example.meichu:id/editText"
                        }
                         else{
                             next_focus = decide_next(cur_focus)


                         }


                        // 现在，buttonX 和 buttonY 包含按钮的坐标，
                        // buttonWidth 和 buttonHeight 包含按钮的宽度和高度。
                    }
                    else if(tmp_x<800 && tmp_x>0 && tmp_y<800 && tmp_y>0){
                        // click permit
                        next_focus = "com.example.meichu:id/editText"
                        Log.d("margin!", "force success!")
                    }
                    else{
                        next_focus = decide_next(cur_focus)
                        Log.d("margin!", "click焦点设置失败")
                    }

                    Log.d("Focus!", "single click, cur = ${cur_focus}, next = ${next_focus}")
                    val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    //Log.d("node_info", "CLICK, Root Node Info: $rootNode")
                    val permit_but = rootNode.findAccessibilityNodeInfosByViewId(next_focus)
                    if (permit_but.isNotEmpty()) {
                        val permit = permit_but[0]
                        // 设置焦点到 "permit" 按钮
                        val focusSuccess = permit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val viewIdResourceName = rootNode.viewIdResourceName

                        if (viewIdResourceName != null) {
                            //Log.d("AccessibilityFocus", "Current focus ID: $viewIdResourceName")
                        }
                        if (focusSuccess) {
                            Log.d("Focus!", "click焦点成功设置在${next_focus} 上")
                            cur_focus = next_focus
                        } else {
                            Log.d("Focus!", "click焦点设置失败")
                        }

                    }else{Log.d(".....", "fail")}


                }

                "com.example.meichu.LONG_PRESSED" -> {
                    Log.d("screen_action", "Received LONG_PRESSED Intent")
                }

                "com.example.meichu.RIGHT_SWIPE" -> {
                    Log.d("screen_action", "Received RIGHT_SWIPE Intent")

                    next_focus = decide_next(cur_focus)
                    Log.d("Focus!", "right swipe, cur = ${cur_focus}, next = ${next_focus}")
                    val rootNode = rootInActiveWindow
                    val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    //Log.d("node_info", "CLICK, Root Node Info: $rootNode")
                    val permit_but = rootNode.findAccessibilityNodeInfosByViewId(next_focus)
                    if (permit_but.isNotEmpty()) {
                        val permit = permit_but[0]
                        // 设置焦点到 "permit" 按钮
                        val focusSuccess = permit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val viewIdResourceName = rootNode.viewIdResourceName

                        if (viewIdResourceName != null) {
                            //Log.d("AccessibilityFocus", "Current focus ID: $viewIdResourceName")
                        }
                        if (focusSuccess) {
                            Log.d("Focus!", "click焦点成功设置在${next_focus} 上")
                            cur_focus = next_focus
                        } else {
                            Log.d("Focus!", "click焦点设置失败")
                        }

                    }else{
                        Log.d("....", "fail")
                    }

                }

                "com.example.meichu.LEFT_SWIPE" -> {
                    Log.d("screen_action", "Received LEFT_SWIPE Intent")
                    last_focus = decide_last(cur_focus)
                    Log.d("Focus!", "left swipe, cur = ${cur_focus}, last = ${last_focus}")
                    val rootNode = rootInActiveWindow
                    val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    //Log.d("node_info", "CLICK, Root Node Info: $rootNode")
                    val permit_but = rootNode.findAccessibilityNodeInfosByViewId(last_focus)
                    if (permit_but.isNotEmpty()) {
                        val permit = permit_but[0]
                        // 设置焦点到 "permit" 按钮
                        val focusSuccess = permit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val viewIdResourceName = rootNode.viewIdResourceName

                        if (viewIdResourceName != null) {
                            //Log.d("AccessibilityFocus", "Current focus ID: $viewIdResourceName")
                        }
                        if (focusSuccess) {
                            Log.d("Focus!", "click焦点成功设置在${last_focus} 上")
                            cur_focus = last_focus
                        } else {
                            Log.d("Focus!", "click焦点设置失败")
                        }

                    }else{
                        Log.d("...", "fail")
                    }
                }

                "com.example.meichu.Permit_h_w" -> {
                    Log.d("h w setting", "Received setting Intent")
                    val x1= intent.getFloatExtra("x1", 0.1F) // 默认值可自定义
                    val x2 = intent.getFloatExtra("x2", 0.1F)
                    val y1 = intent.getFloatExtra("y1", 0.1F)
                    val y2 = intent.getFloatExtra("y2", 0.1F)
                    permit_margins = floatArrayOf(x1, x2, y1, y2)

                }

                else -> {Log.d("Focus!", "click焦点设置失败")}
            }

        }
    }

    fun decide_next(currentViewId: String?): String {
        return when (currentViewId) {
            "com.example.meichu:id/editText" -> "com.example.meichu:id/button2"
            "com.example.meichu:id/button2" -> "com.example.meichu:id/button"
            "com.example.meichu:id/button" -> "com.example.meichu:id/textView"
            "com.example.meichu:id/textView" -> "com.example.meichu:id/permit"
            "com.example.meichu:id/permit" -> "com.example.meichu:id/editText"
            else -> "null"
        }
    }
    fun decide_last(currentViewId: String?): String {
        return when (currentViewId) {
            "com.example.meichu:id/button2" -> "com.example.meichu:id/editText"
            "com.example.meichu:id/button" -> "com.example.meichu:id/button2"
            "com.example.meichu:id/textView" -> "com.example.meichu:id/button"
            "com.example.meichu:id/permit" -> "com.example.meichu:id/textView"
            "com.example.meichu:id/editText" -> "com.example.meichu:id/permit"
            else -> "null"
        }
    }
    private fun findButtonNodeById(nodeInfo: AccessibilityNodeInfo, buttonId: String): AccessibilityNodeInfo? {
        Log.d("findddddddd", "finding $buttonId")
        val queue = LinkedList<AccessibilityNodeInfo>()
        queue.add(nodeInfo)
        while (queue.isNotEmpty()) {
            val currentNode = queue.poll()
            if (currentNode.viewIdResourceName == buttonId) {
                return currentNode
            }
            for (i in 0 until currentNode.childCount) {
                queue.add(currentNode.getChild(i))
            }
        }
        return null
    }










}


