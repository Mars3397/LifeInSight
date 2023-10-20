// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package de.yanneckreiss.mlkittutorial;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.yanneckreiss.mlkittutorial.

public abstract class AbstractCameraXActivity<R> extends BaseModuleActivity {
    private ActivityMainBinding viewBinding;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private long mLastAnalysisResultTime;

    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutId());

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                                this,
                                "You can't use object detection example without granting CAMERA permission",
                                Toast.LENGTH_LONG)
                        .show();
                finish();
            } else {
                setupCameraX();
            }
        }
    }

    private void setupCameraX() {
        PreviewView previewView = viewBinding.previewView;

        // Create a CameraController
        LifecycleCameraController cameraController = new LifecycleCameraController(this);
        cameraController.bindToLifecycle(this);
        cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);

        cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(this),
                new ImageAnalysis.Analyzer() {
                    @Override
                    @WorkerThread
                    public void analyze(@NonNull ImageProxy image) {
                        // Perform image analysis
                        int rotationDegrees = image.getImageInfo().getRotationDegrees();
                        R analysisResult = analyzeImage(image, rotationDegrees);

                        // Apply the analysis result to the UI on the main thread
                        runOnUiThread(() -> applyToUiAnalyzeImageResult(analysisResult));

                        // Close the image proxy to free up resources
                        image.close();
                    }
                }
        );

        // Connect the CameraController to the PreviewView
        previewView.setController(cameraController);

        // Connect the Preview use case to the CameraController
        cameraController.bindToLifecycle(this);
    }



//    private void setupCameraX() {
//        final TextureView textureView = getCameraPreviewTextureView();
//        final PreviewConfig previewConfig = new PreviewConfig.Builder().build();
//        final Preview preview = new Preview(previewConfig);
//        preview.setOnPreviewOutputUpdateListener(output -> textureView.setSurfaceTexture(output.getSurfaceTexture()));
//
//        final ImageAnalysisConfig imageAnalysisConfig =
//            new ImageAnalysisConfig.Builder()
//                .setTargetResolution(new Size(480, 640))
//                .setCallbackHandler(mBackgroundHandler)
//                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
//                .build();
//        final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
//        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
//            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 500) {
//                return;
//            }
//
//            final R result = analyzeImage(image, rotationDegrees);
//            if (result != null) {
//                mLastAnalysisResultTime = SystemClock.elapsedRealtime();
//                runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
//            }
//        });
//
//        CameraX.bindToLifecycle(this, preview, imageAnalysis);
//    }


    @WorkerThread
    @Nullable
    protected abstract R analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread
    protected abstract void applyToUiAnalyzeImageResult(R result);
}
