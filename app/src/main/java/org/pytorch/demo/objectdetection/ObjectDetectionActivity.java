package org.pytorch.demo.objectdetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Locale;


public class ObjectDetectionActivity extends AbstractCameraXActivity<ObjectDetectionActivity.AnalysisResult> {
    private static final int RQ_SPEECH_REC = 102;
    private Module mModule = null;
    private ResultView mResultView;
    private TextView ObjectGuide;
    private Button REC_btn;
    private View screen;
    private String targetObject = "";
    private int targetID = -1;
    private final Handler handler = new Handler();

    private TextToSpeech tts;

    private boolean first = true;

    static class AnalysisResult {
        private final ArrayList<Result> mResults;

        public AnalysisResult(ArrayList<Result> results) {
            mResults = results;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        REC_btn = findViewById(R.id.recButton);
        REC_btn.setOnClickListener(view -> askSpeechInput());
//        REC_btn.performClick();
        ObjectGuide = (TextView) findViewById(R.id.OjbectGuide);
        handler.postDelayed(updateTextTask, 10);
        screen = findViewById(R.id.screen);
        screen.setAlpha(0.0F);
        MyGestureListener gestureListener = new MyGestureListener();
        GestureDetector gestureDetector = new GestureDetector(this, gestureListener);

        screen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }


    public void speak(String text) {
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale chineseLocale = new Locale("zh", "CN");
                tts.setLanguage(chineseLocale);
                tts.setSpeechRate(1.5f);
                tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
            }
        });
    }
    
     private void askSpeechInput() {
         if (!SpeechRecognizer.isRecognitionAvailable(this)) {
             Toast.makeText(this, "Speech recognition is not available", Toast.LENGTH_SHORT).show();
         } else {
             speak("要尋找什麼呢？");
             first = false;
             Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
             intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
             intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW");
             intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "要尋找什麼呢？");
             startActivityForResult(intent, RQ_SPEECH_REC);


         }
     }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQ_SPEECH_REC && resultCode == RESULT_OK && data != null) {
            ArrayList<String> recResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            targetID = -1;
            mResultView.location = "";
            if (recResults != null && !recResults.isEmpty()) {
                targetObject = recResults.get(0);
                REC_btn.setText("再找一次");
                ObjectGuide.setText("正在尋找" + targetObject + "，請緩慢移動鏡頭");
                speak("正在尋找" + targetObject + "，請緩慢移動鏡頭");
                int lineCount = 0;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes.txt")))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (targetObject.contains(line)) {
                            Log.d("hit", "id" + lineCount);
                            targetObject = line;
                            targetID = lineCount;
                            break;
                        }
                        lineCount++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Runnable updateTextTask = new Runnable() {
        String last = "-1";
        public void run() {
            last = updateObjectGuideText(last);
            handler.postDelayed(this, 10);

        }
    };


    private String updateObjectGuideText(String last) {
        String cur;
        if (targetID != -1) {
            if (mResultView.location == "") {
                ObjectGuide.setText("正在尋找" + targetObject + "，請緩慢移動鏡頭");
                cur = "正在尋找" + targetObject + "，請緩慢移動鏡頭";

            } else if (mResultView.location != "找到") {
                ObjectGuide.setText("正在尋找" + targetObject + mResultView.location);
                cur = mResultView.location;
            } else {
                ObjectGuide.setText("已找到" + targetObject);
                cur = "已找到" + targetObject+"叮!叮!叮!";
            }
            mResultView.last_location = mResultView.location;
        } else {
            ObjectGuide.setText("要尋找什麼呢？");
            cur = "要尋找什麼呢？";
        }
       if(!cur.equals(last) ) {
            speak(cur);
           Log.d("msg", cur+"->"+last);
       }
        return cur;
    }


    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_object_detection;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        mResultView = findViewById(R.id.resultView);
        return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                .inflate()
                .findViewById(R.id.object_detection_texture_view);
    }

    @Override
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        mResultView.setResults(result.mResults);
        mResultView.invalidate();
    }

    private Bitmap imgToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Override
    @WorkerThread
    @Nullable
    protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        try {
            if (mModule == null) {
                mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "custom3.ptl"));
            }
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            return null;
        }
        Bitmap bitmap = imgToBitmap(image.getImage());
        Matrix matrix = new Matrix();
        matrix.postRotate(90.0f);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float imgScaleX = (float) bitmap.getWidth() / PrePostProcessor.mInputWidth;
        float imgScaleY = (float) bitmap.getHeight() / PrePostProcessor.mInputHeight;
        float ivScaleX = (float) mResultView.getWidth() / bitmap.getWidth();
        float ivScaleY = (float) mResultView.getHeight() / bitmap.getHeight();

        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, targetID, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0, 0);
        return new AnalysisResult(results);
    }
    public class MyGestureListener implements GestureDetector.OnGestureListener {
        private long lastSingleTapTime = 0;
        private static final int DOUBLE_TAP_TIME_THRESHOLD = 500; // 设置双击时间阈值，单位毫秒
        private Handler handler = new Handler();

        @Override
        public boolean onDown(MotionEvent e) {
            // 按下事件
            //Log.d("gesture", "on down");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            //TODO: Not yet implemented
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastSingleTap = currentTime - lastSingleTapTime;

            if (timeSinceLastSingleTap < DOUBLE_TAP_TIME_THRESHOLD) {
                // 双击事件
                Log.d("gesture", "double tap, (" + e.getX() + "," + e.getY() + ")");
                // 取消等待的单击事件
                Intent intent = new Intent("org.pytorch.demo.objectdetection.DOUBLE_CLICKED");
                sendBroadcast(intent);
                handler.removeCallbacksAndMessages(null);
            } else {
                handler.postDelayed(() -> {
                    Log.d("gesture", "single tap, (" + e.getX() + "," + e.getY() + ")");
                    Intent intent = new Intent("org.pytorch.demo.objectdetection.SINGLE_CLICKED");
                    intent.putExtra("x_val", e.getX());
                    intent.putExtra("y_val", e.getY());
                    sendBroadcast(intent);
                }, DOUBLE_TAP_TIME_THRESHOLD);

            }

            lastSingleTapTime = currentTime;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //Log.d("gesture", "scroll");
            //TODO: Not yet implemented
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // 长按事件
            Log.d("gesture", "long press, (" + e.getX() + "," + e.getY() + ")");
            Intent intent = new Intent("org.pytorch.demo.objectdetection.LONG_PRESSED");
            sendBroadcast(intent);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // 滑动事件
            float distanceX = Math.abs(e2.getX() - e1.getX());

            if (distanceX > 150) {
                // 左右滑动距离大于MIN_DISTANCE
                if (e2.getX() > e1.getX()) {
                    // 从左向右滑动
                    Log.d("gesture", "Right Swipe, " + e1.getX() + " ~ " + e2.getX());
                    Intent intent = new Intent("org.pytorch.demo.objectdetection.RIGHT_SWIPE");
                    sendBroadcast(intent);
                } else {
                    // 从右向左滑动
                    Log.d("gesture", "Left Swipe, " + e1.getX() + " ~ " + e2.getX());
                    Intent intent = new Intent("org.pytorch.demo.objectdetection.LEFT_SWIPE");
                    sendBroadcast(intent);
                }
            }

            return true;
        }
    }
}
