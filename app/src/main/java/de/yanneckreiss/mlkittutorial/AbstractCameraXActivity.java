// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package de.yanneckreiss.mlkittutorial;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewStub;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.CameraX;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
//import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
//import androidx.camera.core.PreviewConfig;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.yanneckreiss.cameraxtutorial.databinding.ActivityObjectDetectionBinding;

public abstract class AbstractCameraXActivity<R> extends BaseModuleActivity {
    private ActivityObjectDetectionBinding viewBinding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private long mLastAnalysisResultTime;

    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    private Bitmap bitmapBuffer;
    private Executor executor = Executors.newSingleThreadExecutor();
    private List<String> permissions = Arrays.asList(Manifest.permission.CAMERA);
    private int permissionsRequestCode = new Random().nextInt(10000);

    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isFrontFacing() {
        return lensFacing == CameraSelector.LENS_FACING_FRONT;
    }

    private boolean pauseAnalysis = false;
    private int imageRotationDegrees = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityObjectDetectionBinding.inflate(getLayoutInflater());
        setContentView(getContentViewLayoutId());
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

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

//    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
//        Preview preview = new Preview.Builder()
//                .build();
//
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build();
//
//        ImageAnalysis imageAnalysis =
//                new ImageAnalysis.Builder()
//                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                        .build();
//
//        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
//            @OptIn(markerClass = ExperimentalGetImage.class) @Override
//            public void analyze(@NonNull ImageProxy imageProxy) {
//                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
//                Image image = imageProxy.getImage();
//
//                // Perform image analysis
//                R result = analyzeImage((ImageProxy) image, rotationDegrees);
//
//                if (result != null) {
//                    runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
//                }
//                imageProxy.close();
//            }
//        });
//
//        PreviewView previewView = viewBinding.previewView;
//
//        preview.setSurfaceProvider(previewView.getSurfaceProvider());
//
//        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);
//    }
//
//    private void setupCameraX() {
//        Log.d("de.yanneckreiss.cameraxtutorial", "setupCameraX: start");
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                bindPreview(cameraProvider);
//                Log.d("de.yanneckreiss.cameraxtutorial", "Camera setup successful");
//            } catch (ExecutionException | InterruptedException e) {
//                // No errors need to be handled for this Future.
//                // This should never be reached.
//                Log.e("de.yanneckreiss.cameraxtutorial", "Error setting up camera", e);
//            }
//        }, ContextCompat.getMainExecutor(this));
//
//        Log.d("de.yanneckreiss.cameraxtutorial", "setupCameraX: down");
//    }

    private void setupCameraX() {
        Log.d("de.yanneckreiss.cameraxtutorial", "setupCameraX: start");

        PreviewView previewView = viewBinding.objectDetectionTextureViewStub;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
                    @OptIn(markerClass = ExperimentalGetImage.class) @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        Image image = imageProxy.getImage();

                        // Perform image analysis
                        R result = analyzeImage((ImageProxy) image, rotationDegrees);

                        if (result != null) {
                            runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
                        }
                        imageProxy.close();
                    }
                });

                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());

                // Camera setup was successful
                Log.d("de.yanneckreiss.cameraxtutorial", "Camera setup successful");
            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
                Log.e("de.yanneckreiss.cameraxtutorial", "Error setting up camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

        Log.d("de.yanneckreiss.cameraxtutorial", "setupCameraX: down");
    }

//    @OptIn(markerClass = ExperimentalGetImage.class)
//    private void setupCameraX() {
//        Log.d("de.yanneckreiss.cameraxtutorial", "setupCameraX: start");
//        // Create a Camera Controller
//        CameraSelector cameraSelector = new CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Or use other options
//                .build();
//
//        // Create a LifecycleCameraController
//        LifecycleCameraController cameraController = new LifecycleCameraController(this);
//
//        // Set the Image Analysis Analyzer
//        cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),
//            imageProxy -> {
//                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
//                Image image = imageProxy.getImage();
//
//                // Perform image analysis
//                R result = analyzeImage((ImageProxy) image, rotationDegrees);
//
//                if (result != null) {
//                    runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
//                }
//                imageProxy.close();
//            }
//        );
//
//        // Attach the Camera Controller to the Preview View
//        PreviewView preview = viewBinding.objectDetectionTextureViewStub;
//        preview.setController(cameraController);
//
//        // Bind the Camera Controller to the LifecycleOwner
//        cameraController.bindToLifecycle(this);
//
//        Log.d("de.yanneckreiss.cameraxtutorial", "setupCameraX: down");
//    }

//    private void setupCameraX() {
//        TextureView previewView = getCameraPreviewTextureView();
//
//        // Create a CameraController
//        LifecycleCameraController cameraController = new LifecycleCameraController(this);
//        cameraController.bindToLifecycle(this);
//        cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
//
//        cameraController.setImageAnalysisAnalyzer(
//            ContextCompat.getMainExecutor(this),
//                image -> {
//                    // Perform image analysis
//                    int rotationDegrees = image.getImageInfo().getRotationDegrees();
//                    R analysisResult = analyzeImage(image, rotationDegrees);
//
//                    // Apply the analysis result to the UI on the main thread
//                    runOnUiThread(() -> applyToUiAnalyzeImageResult(analysisResult));
//
//                    // Close the image proxy to free up resources
//                    image.close();
//                }
//        );
//
//        // Connect the CameraController to the PreviewView
//        previewView.setController(cameraController);
//
//        // Connect the Preview use case to the CameraController
//        cameraController.bindToLifecycle(this);
//    }

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
