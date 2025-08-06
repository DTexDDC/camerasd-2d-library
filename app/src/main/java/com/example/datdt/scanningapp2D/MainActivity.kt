package com.example.datdt.scanningapp2D

import android.content.Context
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
import java.io.File
import java.io.IOException
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The modelFile and labelFile store the file object of .tflite and .txt.
        // The function copyAssetToInternalStorage is a custom one to test since I didn't have API access. Don't use it.
        val modelFile = copyAssetToInternalStorage(this, "tinned-food-product-on-scene-tfl-v1b_float32.tflite", "tinned-food-product-on-scene-tfl-v1b_float32.tflite")
        val labelFile = copyAssetToInternalStorage(this, "tinned_display.txt", "tinned_display.txt")

        // Pass in the absolute path of the files from above.
        val modelinfo = ModelInfo(modelPath = modelFile!!.absolutePath,
            labelPath = labelFile!!.absolutePath,
            labels_displayPath = labelFile!!.absolutePath)

//        val modelinfo = ModelInfo(modelPath = "tinned-food-product-on-scene-tfl-v1b_float32.tflite",
//            labelPath = "tinned_display.txt",
//            labels_displayPath = "tinned_display.txt")
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



fun copyAssetToInternalStorage(context: Context, assetFileName: String, outputFileName: String): File? {
    return try {
        val file = File(context.filesDir, outputFileName)
        if (!file.exists()) {
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        file
    } catch (e: IOException) {
        e.printStackTrace()
        null
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