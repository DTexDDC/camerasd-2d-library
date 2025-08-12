package com.example.datdt.scanningapp2D

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datdt.scanningsdk2D.DetectionManager
import com.example.datdt.scanningsdk2D.DetectionPayload
import com.example.datdt.scanningsdk2D.DetectionSdk
import com.example.datdt.scanningsdk2D.models.ModelInfo
//import com.google.firebase.appdistribution.gradle.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.FileOutputStream

import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

//interface ApiService {
//    @POST("staging/")
//    suspend fun fetchDeviceModels(@Body body: Map<String, String>): ApiWrapper2
//}
//
//// NetworkModule.kt
//object NetworkModule {
//    private const val BASE_URL = "https://330qbi4agl.execute-api.ap-southeast-2.amazonaws.com/"
//
//    fun provideApiService(): ApiService {
//        return Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ApiService::class.java)
//    }
//}
//
//data class ApiWrapper2(
//    val statusCode: Int,
//    val headers: Map<String, String>,
//    val body: String
//)
//
//class ApiResponse2 (
//    val action: String,
//    val result: List<ModelResult>
//)
//
//data class ModelResult(
//    val model_id: Int,
//    val version: String,
//    val detection_group_id: Int,
//    val active: Int,
//    val created: String,
//    val comments: String,
//    val path: String
//)
//
//private val downloadedFiles = mutableListOf<File>()
//
//class MainActivity : AppCompatActivity() {
//
//    private var sdkReady = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        iniViews()
//        // The modelFile and labelFile store the file object of .tflite and .txt.
//        // The function copyAssetToInternalStorage is a custom one to test since I didn't have API access. Don't use it.
////        val modelFile = copyAssetToInternalStorage(this, "tinned-food-product-on-scene-tfl-v1b_float32.tflite", "tinned-food-product-on-scene-tfl-v1b_float32.tflite")
////        val labelFile = copyAssetToInternalStorage(this, "tinned_display.txt", "tinned_display.txt")
////
////        // Pass in the absolute path of the files from above.
////        val modelinfo = ModelInfo(modelPath = modelFile!!.absolutePath,
////            labelPath = labelFile!!.absolutePath,
////            labels_displayPath = labelFile!!.absolutePath)
////
//////        val modelinfo = ModelInfo(modelPath = "tinned-food-product-on-scene-tfl-v1b_float32.tflite",
//////            labelPath = "tinned_display.txt",
//////            labels_displayPath = "tinned_display.txt")
////        modelinfo.let {
////            DetectionSdk.with(this)
////                .model(it)
////                .start()
////        }
////
////        sdkReady = true
////
////        observeDetections()
//    }
//
//    private fun iniViews() {
//        val TAG = "iniViews"
//        Log.d(TAG, "iniViews: called")
//
//        val modelFile = getLocalFileBySuffix(".tflite")
//        val labelFile = getLocalFileBySuffix(".yaml")
//
//        Log.d(TAG, "iniViews: Found files: model=${modelFile.absolutePath}, label=${labelFile.absolutePath}")
//        // fetchAndSetupModel()
//
//        if (modelFile.exists() && labelFile.exists() && labelFile.length() > 0) {
//            Log.d(TAG, "iniViews: Local model & label found, initializing SDK.")
//            initSdk(modelFile, labelFile)
//        } else {
////            if (isNetworkAvailable()) {
////                Log.d(TAG, "iniViews: Local files missing or label empty, fetching from API.")
////                fetchAndSetupModel()
////            } else {
////                Log.w(TAG, "iniViews: No internet and no local files.")
////                Toast.makeText(
////                    this,
////                    "Model/Label not available. Please connect to internet once.",
////                    Toast.LENGTH_LONG
////                ).show()
////            }
//            fetchAndSetupModel()
//        }
//    }
//
//    private fun getLocalFileBySuffix(suffix: String): File {
//        val files = filesDir.listFiles() ?: arrayOf()
//        return files.firstOrNull { it.name.endsWith(suffix) } ?: File("")
//    }
//
//    private fun fetchAndSetupModel() {
//        val TAG = "fetchAndSetupModel"
//        Log.d(TAG, "fetchAndSetupModel: called")
//
//        lifecycleScope.launch {
//            try {
//                val body = mapOf(
//                    "origin" to "DETECTION_GROUPS",
//                    "action" to "APP2_FETCH_DEVICE_MODELS",
//                    "amplifyUID" to "d67e7b4a-4625-4364-9cf5-288b583e5906"
//                )
//
//                val wrapper = NetworkModule.provideApiService().fetchDeviceModels(body)
//                Log.d(TAG, "fetchAndSetupModel: API response received.")
//                Log.d(TAG, "Raw API body = ${wrapper.body}")
//                val apiResponse = Gson().fromJson(wrapper.body, ApiResponse2::class.java)
//                val resultList: List<ModelResult> = apiResponse.result ?: emptyList()
//
//                if (resultList.isNotEmpty()) {
//                    val model = resultList[1]
//                    val urls = model.path.split("||")
//
//                    Log.d(TAG, "fetchAndSetupModel: URLs to download = $urls")
//
//                    downloadedFiles.clear()
//
//                    for ((index, url) in urls.withIndex()) {
//                        val fileName = url.substringAfterLast("/").substringBefore("?")
//                        val file = File(filesDir, fileName)
//
//                        Log.d(TAG, "📥 [$index/${urls.size}] Starting download for $fileName from $url")
//
//                        val success = downloadFileSync(url, file)
//                        if (success) {
//                            Log.d(TAG, "✅ [$index/${urls.size}] Downloaded $fileName successfully. size=${file.length()}")
//                            downloadedFiles.add(file)
//
//                            if (file.extension == "txt") {
//                                val textPreview = file.readText().take(200)
//                                Log.d(TAG, "TXT Content Preview (${file.name}): $textPreview")
//                            }
//                        } else {
//                            Log.w(TAG, "❌ [$index/${urls.size}] Failed to download $fileName")
//                        }
//                    }
//
//                    val modelFile = downloadedFiles.find { it.extension == "tflite" }
//                    val labelFile = downloadedFiles.find { it.extension == "txt" && it.length() > 0 }
//
//                    if (modelFile == null) {
//                        Log.e(TAG, "❌ Model file not found in downloads")
//                        Toast.makeText(this@MainActivity, "Model file not downloaded", Toast.LENGTH_LONG).show()
//                        return@launch
//                    }
//
//                    if (labelFile == null) {
//                        Log.e(TAG, "❌ Label file not found or empty")
//                        Toast.makeText(this@MainActivity, "Label file not downloaded or empty", Toast.LENGTH_LONG).show()
//                        return@launch
//                    }
//
//                    Log.d(TAG, "✅ Initializing SDK with model=${modelFile.absolutePath}, label=${labelFile.absolutePath}")
//                    initSdk(modelFile, labelFile)
//
//                } else {
//                    Toast.makeText(this@MainActivity, "No model data from server", Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "fetchAndSetupModel: API error", e)
//                Toast.makeText(this@MainActivity, "API error: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private suspend fun downloadFileSync(url: String, destFile: File): Boolean = withContext(
//        Dispatchers.IO) {
//        val TAG = "downloadFileSync"
//        try {
//            val client = OkHttpClient.Builder()
//                .connectTimeout(2, TimeUnit.MINUTES)
//                .readTimeout(60, TimeUnit.MINUTES)   // 👈
//                .writeTimeout(60, TimeUnit.MINUTES)  // 👈
//                .callTimeout(65, TimeUnit.MINUTES)   // 👈
//                .build()
//
//
//            val request = Request.Builder().url(url).build()
//            client.newCall(request).execute().use { response ->
//                if (!response.isSuccessful) {
//                    Log.e(TAG, "❌ HTTP error while downloading $url : ${response.code}")
//                    return@withContext false
//                }
//
//                val body = response.body ?: return@withContext false
//                val contentLength = body.contentLength()
//
//                body.byteStream().use { input ->
//                    FileOutputStream(destFile).use { output ->
//                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
//                        var bytesRead: Int
//                        var totalBytesRead = 0L
//                        var lastLogged = 0L
//
//                        while (input.read(buffer).also { bytesRead = it } != -1) {
//                            output.write(buffer, 0, bytesRead)
//                            totalBytesRead += bytesRead
//
//                            // progress ہر 1MB پر log کرو
//                            if (totalBytesRead - lastLogged > 1_000_000) {
//                                lastLogged = totalBytesRead
//                                if (contentLength > 0) {
//                                    val progress = (100 * totalBytesRead / contentLength).toInt()
//                                    Log.d(TAG, "⬇️ Downloading ${destFile.name}: $progress% ($totalBytesRead/$contentLength bytes)")
//                                } else {
//                                    Log.d(TAG, "⬇️ Downloading ${destFile.name}: $totalBytesRead bytes...")
//                                }
//                            }
//                        }
//                    }
//                }
//
//                Log.d(TAG, "✅ Finished downloading ${destFile.name}, size=${destFile.length()}")
//                true
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Download failed for $url", e)
//            false
//        }
//    }
//
//
//    fun initSdk(modelFile: File, labelFile: File) {
//        // Pass in the absolute path of the files from above.
//        val modelinfo = ModelInfo(modelPath = modelFile!!.absolutePath,
//            labelPath = labelFile!!.absolutePath,
//            labels_displayPath = labelFile!!.absolutePath)
//
////        val modelinfo = ModelInfo(modelPath = "tinned-food-product-on-scene-tfl-v1b_float32.tflite",
////            labelPath = "tinned_display.txt",
////            labels_displayPath = "tinned_display.txt")
//        modelinfo.let {
//            DetectionSdk.with(this)
//                .model(it)
//                .start()
//        }
//
//        sdkReady = true
//
//        observeDetections()
//    }
//
//    private var alreadyHandled = false
//    private fun observeDetections() {
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                DetectionManager.detectionPayload.collect { payload ->
//                    if (!sdkReady || payload == null || alreadyHandled) return@collect
//
//                    if (payload.detections.isEmpty() && payload.overviewImage == null) {
//                        alreadyHandled = true
////                        Log.d("Main Detections", "No detections, ending SDK")
//                        DetectionManager.clear()
//
//                        // Whatever activity that you have launch it here.
//                        startActivity(Intent(this@MainActivity, RetryActivity::class.java))
//                        finish()
//                    } else {
//                        handleDetectionResults(payload)
//                    }
//                }
//            }
//        }
//    }
//
//    private fun handleDetectionResults(payload: DetectionPayload) {
//        // Example: Log detections (You should process them as before)
//         for (detection in payload.detections) {
////             Log.d("Main Detections", "${detection.facing}, ${detection.shelf}, ${detection.label}")
//            // detection.bay
//            // detection.shelf
//            // detection.facing
//            // detection.label
//            // detection.cropString
//            // detection.score (Confidence)
//            // detection.labelDisplay (Display Label)
//            // detection.boundingBox.centerX() (X)
//            // detection.boundingBox.centerY() (Y)
//
//         }
//        Log.d("Detection", "Detected objects: ${payload.detections.size}")
//    }
//}
//
//
//
//fun copyAssetToInternalStorage(context: Context, assetFileName: String, outputFileName: String): File? {
//    return try {
//        val file = File(context.filesDir, outputFileName)
//        if (!file.exists()) {
//            context.assets.open(assetFileName).use { inputStream ->
//                FileOutputStream(file).use { outputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//        }
//        file
//    } catch (e: IOException) {
//        e.printStackTrace()
//        null
//    }
//}
//
////@Composable
////fun DemoScreen(navController: NavController) {
////    Scaffold(modifier = Modifier.fillMaxSize()) {
////        Column(
////            modifier = Modifier.fillMaxSize().padding(it),
////            verticalArrangement = Arrangement.Center,
////            horizontalAlignment = Alignment.CenterHorizontally
////        ) {
////            ModelType.entries.map {
////                Button(
////                    onClick = {
////                        navController.navigate("camera")
////                    }
////                ) {
////                    Text(it.name)
////                }
////            }
////        }
////    }
//}