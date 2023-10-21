package com.example.meichu

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.meichu.MyActivity.MyGestureListener
import java.util.Locale
import kotlin.math.absoluteValue


class MainActivity: AppCompatActivity(){

    lateinit var tts : TextToSpeech
    private val RQ_SPEECH_REC = 102
    companion object{
        const val MIN_DISTANCE = 150
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var btn = findViewById<Button>(R.id.button2)
        var edtxt = findViewById<EditText>(R.id.editText)
        var REC_btn = findViewById<Button>(R.id.button)
        var permit = findViewById<Button>(R.id.permit)
        var imageView = findViewById<ImageView>(R.id.screen)
        var gestureDetector = GestureDetector(this, MyGestureListener())

        val intent = Intent("com.example.meichu.Permit_h_w")
        intent.putExtra("x1", edtxt.x - edtxt.width / 2 )
        intent.putExtra("x2", edtxt.x + edtxt.width / 2)
        intent.putExtra("y1", edtxt.y - edtxt.height / 2)
        intent.putExtra("y2", edtxt.y + edtxt.height / 2)
        sendBroadcast(intent)


        // 设置 ImageView 的触摸监听器
        imageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
        imageView.alpha = 0.0f

        //val speech_content = findViewById<TextView>(R.id.textView)

        val customReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.meichu.DOUBLE_CLICKED_SPEECH") {
                    Log.d("MyAccessibilityService", "main: ask to speech!")
                    askSpeechInput()
                }
            }
        }

        val filter = IntentFilter("com.example.meichu.DOUBLE_CLICKED_SPEECH")
        registerReceiver(customReceiver, filter)




        btn.setOnClickListener{
            tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                if(it==TextToSpeech.SUCCESS){
                    tts.language = Locale.US
                    tts.setSpeechRate(1.0f)
                    tts.speak(edtxt.text.toString(), TextToSpeech.QUEUE_ADD, null)
                }
            })
        }

        REC_btn.setOnClickListener{
            askSpeechInput()
        }

        permit.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)


            //startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            //val intent = Intent()
            //intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            //intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
        //val serviceIntent = Intent(this, GlobalTouchInterceptor::class.java)
        //startService(serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var sptxt = findViewById<TextView>(R.id.textView)
        if(requestCode == RQ_SPEECH_REC && resultCode == RESULT_OK){
            var result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                val recognizedText = result[0] // 获取识别的文本
                sptxt.text = recognizedText // 将识别的文本设置到 TextView 中
                Log.d("MyAccessibilityService", "speech to text: $recognizedText")
            }
        }
    }
    private fun  askSpeechInput(){
        if (!SpeechRecognizer.isRecognitionAvailable(this)){
            Toast.makeText(this, "speech recognition is not available", Toast.LENGTH_SHORT)
        }else{
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, RQ_SPEECH_REC)

        }
    }

    //private val customReceiver = object : BroadcastReceiver() {
    //    override fun onReceive(context: Context?, intent: Intent?) {
       //     if (intent?.action == "com.example.meichu.DOUBLE_CLICKED_SPEECH") {
                //REC_btn.performClick()
          //     askSpeechInput()
           // }
        //}

    //}

    private inner class MyGestureListener : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent): Boolean {
            // 按下事件
            //Log.d("gesture", "on down")
            return true
        }

        override fun onShowPress(p0: MotionEvent) {
            //TODO("Not yet implemented")
           return
        }

        private var lastSingleTapTime: Long = 0
        private val DOUBLE_TAP_TIME_THRESHOLD = 500 // 设置双击时间阈值，单位毫秒
        private val handler = Handler()

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastSingleTap = currentTime - lastSingleTapTime

            if (timeSinceLastSingleTap < DOUBLE_TAP_TIME_THRESHOLD) {
                // 双击事件
                Log.d("gesture", "double tap, (${e.x},${e.y})")
                // 取消等待的单击事件
                val intent = Intent("com.example.meichu.DOUBLE_CLICKED")
                sendBroadcast(intent)
                handler.removeCallbacksAndMessages(null)
            } else {
                // 等待一段时间后执行单击事件
                handler.postDelayed({
                    Log.d("gesture", "single tap, (${e.x},${e.y})")
                    val intent = Intent("com.example.meichu.SINGLE_CLICKED")
                    intent.putExtra("x_val", e.x)
                    intent.putExtra("y_val", e.y)
                    sendBroadcast(intent)
                }, DOUBLE_TAP_TIME_THRESHOLD.toLong())

            }

            lastSingleTapTime = currentTime
            return true
        }



        override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
            //Log.d("gesture", "scroll")
            //TODO("Not yet implemented")
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            // 长按事件
            Log.d("gesture", "long press, (${e.x},${e.y})")
            val intent = Intent("com.example.meichu.LONG_PRESSED")
            sendBroadcast(intent)
        }




        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // 滑动事件
            val distanceX = (e2.x - e1.x).absoluteValue

            if (distanceX > MIN_DISTANCE) {
                // 左右滑动距离大于MIN_DISTANCE
                if (e2.x > e1.x) {
                    // 从左向右滑动
                    Log.d("gesture", "Right Swipe, ${e1.x} ~ ${e2.x}")
                    val intent = Intent("com.example.meichu.RIGHT_SWIPE")
                    sendBroadcast(intent)
                } else {
                    // 从右向左滑动
                    Log.d("gesture", "Left Swipe, ${e1.x} ~ ${e2.x}")
                    val intent = Intent("com.example.meichu.LEFT_SWIPE")
                    sendBroadcast(intent)
                }
            }

            return true
        }




    }




}