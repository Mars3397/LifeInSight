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
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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


public class ObjectDetectionActivity extends AbstractCameraXActivity<ObjectDetectionActivity.AnalysisResult> {
    private static final int RQ_SPEECH_REC = 102;
    private Module mModule = null;
    private ResultView mResultView;
    private TextView ObjectGuide;
    private String targetObject = "";
    private int targetID = -1;
    private final Handler handler = new Handler();

    static class AnalysisResult {
        private final ArrayList<Result> mResults;

        public AnalysisResult(ArrayList<Result> results) {
            mResults = results;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Button buttonBack = findViewById(R.id.back2Text);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              final Intent intent = new Intent(ObjectDetectionActivity.this, MainActivity.class);
              startActivity(intent);
            }
        });

        Button REC_btn = findViewById(R.id.recButton);
        REC_btn.setOnClickListener(view -> askSpeechInput());
        REC_btn.performClick();
        ObjectGuide = (TextView) findViewById(R.id.OjbectGuide);
        handler.postDelayed(updateTextTask, 10);
    }
    
     private void askSpeechInput() {
         if (!SpeechRecognizer.isRecognitionAvailable(this)) {
             Toast.makeText(this, "Speech recognition is not available", Toast.LENGTH_SHORT).show();
         } else {
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
                ObjectGuide.setText("正在尋找" + targetObject + "，請緩慢移動鏡頭");
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
        public void run() {
            updateObjectGuideText();
            handler.postDelayed(this, 10);
        }
    };


    private void updateObjectGuideText() {
        if (targetID != -1) {
            if (mResultView.location == "") {
                ObjectGuide.setText("正在尋找" + targetObject + "，請緩慢移動鏡頭");
            } else if (mResultView.location != "找到") {
                ObjectGuide.setText("正在尋找" + targetObject + mResultView.location);
            } else {
                ObjectGuide.setText("已找到" + targetObject);
            }
        } else {
            ObjectGuide.setText("要尋找什麼呢？");
        }
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
}
