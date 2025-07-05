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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DetectionSdk.with(this).start()

        sdkReady = true
        observeDetections()
    }

//    override fun onResume() {
//        super.onResume()
//
//        if (CameraSdk.sdkActivity == null) {
//            DetectionSdk.with(this).start()
//        }
//    }

//    override fun onResume() {
//        super.onResume()
//
//        if (CameraSdk.sdkActivity == null && detectionStarted) {
//            detectionStarted = false // Reset flag to allow retry logic later if needed
//
//            // Send empty payload after SDK closes
//            DetectionManager.updateDetections(DetectionPayload(emptyList(), null))
//        }
//    }
    private var alreadyHandled = false
    private fun observeDetections() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DetectionManager.detectionPayload.collect { payload ->
                    if (!sdkReady || payload == null || alreadyHandled) return@collect

                    if (payload.detections.isEmpty() && payload.overviewImage == null) {
                        alreadyHandled = true
                        Log.d("MainActivity", "No detections, ending SDK")
//                        DetectionSdk.with(this@MainActivity).end()
                        DetectionManager.clear()
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
             Log.d("Main", "${detection.facing}, ${detection.shelf}, ${detection.label}")
        // detection.bay
        // detection.shelf
        // detection.facing
        // detection.label
        // detection.cropString
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