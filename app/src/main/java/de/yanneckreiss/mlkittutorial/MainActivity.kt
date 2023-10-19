package de.yanneckreiss.mlkittutorial

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.AspectRatio
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import de.yanneckreiss.mlkittutorial.ui.theme.JetpackComposeMLKitTutorialTheme
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import androidx.compose.ui.graphics.Color as backGroundColor
import android.content.Intent
import de.yanneckreiss.mlkittutorial.ChatRoom

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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopAppBar(title = { Text("Access Pro") }) },
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
                        .size(80.dp)
                        .padding(16.dp)
                        .background(backGroundColor.White, CircleShape)
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
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Take a photo",
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
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
