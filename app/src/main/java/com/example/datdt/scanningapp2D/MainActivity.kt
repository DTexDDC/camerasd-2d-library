package com.example.datdt.scanningapp2D

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datdt.scanningsdk2D.DetectionManager
import com.example.datdt.scanningsdk2D.DetectionPayload
import com.example.datdt.scanningsdk2D.DetectionSdk
import com.example.datdt.scanningsdk2D.models.ModelInfo
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modelinfo = ModelInfo(modelPath = "tinned-food-product-on-scene-tfl-v1b_float32.tflite",
            labelPath = "tinned_display.txt",
            labels_displayPath = "tinned_display.txt")
        modelinfo.let {
            DetectionSdk.with(this)
                .model(it)
                .start()
        }

        sdkReady = true

        observeDetections()
    }

    private var alreadyHandled = false
    private fun observeDetections() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                DetectionManager.detectionPayload.collect { payload ->
                    if (!sdkReady || payload == null || alreadyHandled) return@collect

                    if (payload.detections.isEmpty() && payload.overviewImage == null) {
                        alreadyHandled = true
//                        Log.d("Main Detections", "No detections, ending SDK")
                        DetectionManager.clear()

                        // Whatever activity that you have launch it here.
                        startActivity(Intent(this@MainActivity, RetryActivity::class.java))
                        finish()
                    } else {
                        handleDetectionResults(payload)
                    }
                }
            }
        }
    }

    private fun handleDetectionResults(payload: DetectionPayload) {
        // Example: Log detections (You should process them as before)
         for (detection in payload.detections) {
//             Log.d("Main Detections", "${detection.facing}, ${detection.shelf}, ${detection.label}")
            // detection.bay
            // detection.shelf
            // detection.facing
            // detection.label
            // detection.cropString
            // detection.score (Confidence)
            // detection.labelDisplay (Display Label)
            // detection.boundingBox.centerX() (X)
            // detection.boundingBox.centerY() (Y)

         }
        Log.d("Detection", "Detected objects: ${payload.detections.size}")
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