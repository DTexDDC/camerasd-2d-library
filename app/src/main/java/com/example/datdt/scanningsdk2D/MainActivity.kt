package com.example.datdt.scanningsdk2D

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.datdt.scanningsdk2D.ui.theme._2DScanningSDKAndroidTheme
import com.example.datdt.scanningsdk2D.CameraScreen
import com.example.datdt.scanningsdk2D.models.ModelType
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start detection using the SDK
        DetectionSdk
            .with(this)
            //.model(ModelType.OUTPUT_FLOAT32)  // Optional, defaults to DEFAULT
            .start()

        // Listen for detection results
        observeDetections()
    }

    private fun observeDetections() {
        // Collect detection results from DetectionManager
        lifecycleScope.launch {
            DetectionManager.detectionPayload.collect { payload ->

                if (payload.overviewImage == "end") {
                    // Handle SDK completion

                    // Example: Finish activity
                    finish()
                } else {
                    // Handle detection results (list of detections or image)
                    handleDetectionResults(payload)
                }
            }
        }
    }

    private fun handleDetectionResults(payload: DetectionPayload) {
        // Example: Log detections (You should process them as before)
        // for (detection in payload.detections) {
        // detection.bay
        // detection.shelf
        // detection.facing
        // detection.label
        // detection.cropString
        // }
        Log.d("Detection", "Detected objects: ${payload.detections.size}")
        Log.d("Detection", "Overview image path: ${payload.overviewImage}")
    }
}

//@Composable
//fun DemoScreen(navController: NavController) {
//    Scaffold(modifier = Modifier.fillMaxSize()) {
//        Column(
//            modifier = Modifier.fillMaxSize().padding(it),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            ModelType.entries.map {
//                Button(
//                    onClick = {
//                        navController.navigate("camera")
//                    }
//                ) {
//                    Text(it.name)
//                }
//            }
//        }
//    }
//}