package de.yanneckreiss.mlkittutorial

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.camera.core.AspectRatio
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.yanneckreiss.cameraxtutorial.R
import de.yanneckreiss.mlkittutorial.ui.theme.JetpackComposeMLKitTutorialTheme
import androidx.compose.ui.graphics.Color as backGroundColor

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetpackComposeMLKitTutorialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val cameraPermissionState: PermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

                    if (cameraPermissionState.status.isGranted) {
                        CameraContent()
                    } else {
                        // add error message
                    }
                }
            }
        }
    }

    @Composable
    private fun CameraContent() {
        val context: Context = LocalContext.current
        val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
        val cameraController: LifecycleCameraController = remember { LifecycleCameraController(context) }
        var detectedText: String by remember { mutableStateOf("No text detected yet..") }

        fun onTextUpdated(updatedText: String) {
            detectedText = updatedText
        }

        fun releaseCamera() {
            cameraController.unbind()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues: PaddingValues ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(Color.BLACK)
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_START
                        }.also { previewView ->
                            startTextRecognition(
                                context = context,
                                cameraController = cameraController,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                onDetectedTextUpdated = ::onTextUpdated
                            )
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(top = 10.dp)
                        .align(Alignment.TopCenter)
                        .alpha(0.7f)
                        .background(backGroundColor.Black, shape = RoundedCornerShape(50.dp)),
                ) {
                    Text(
                        text = "文字放大鏡",
                        color = backGroundColor.White,
                        textAlign = TextAlign.Center,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .alpha(0.7f)
                        .background(backGroundColor.Black),
                ) {
                    Box(
                        modifier = Modifier
                            .size(135.dp)
                            .padding(20.dp)
                            .background(backGroundColor.White, CircleShape)
                            .align(Alignment.BottomCenter)
                            .clickable {
                                val intent = Intent(context, ChatRoom::class.java)
                                if (detectedText != "") {
                                    intent.putExtra("detectText", detectedText)
                                } else {
                                    intent.putExtra("detectText", "No text detected yet..")
                                }

                                startActivity(intent)
                            }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_capture),
                            contentDescription = "Take a photo",
                            modifier = Modifier
                                .size(65.dp)
                                .align(Alignment.Center)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(135.dp)
                            .padding(20.dp)
                            .background(backGroundColor.White, CircleShape)
                            .align(Alignment.BottomEnd)
                            .clickable {
                                releaseCamera()
                                val intent = Intent(context, ObjectDetectionActivity::class.java)
                                startActivity(intent)
                            }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search Item",
                            modifier = Modifier
                                .size(65.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    private fun startTextRecognition(
        context: Context,
        cameraController: LifecycleCameraController,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onDetectedTextUpdated: (String) -> Unit
    ) {

        cameraController.imageAnalysisTargetSize = CameraController.OutputSize(AspectRatio.RATIO_16_9)
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context),
            TextRecognitionAnalyzer(onDetectedTextUpdated = onDetectedTextUpdated)
        )

        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
    }
}
