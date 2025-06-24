@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.datdt.scanningsdk2D

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.datdt.scanningsdk2D.ObjectDetectorHelper
import com.example.datdt.scanningsdk2D.DisplayRotationHelper
import com.example.datdt.scanningsdk2D.models.DetectionObject
import com.example.datdt.scanningsdk2D.models.ModelInfo
import com.example.datdt.scanningsdk2D.models.ModelType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.doOnLayout
import com.example.datdt.scanningsdk2D.models.BayObject
import com.example.datdt.scanningsdk2D.models.LabelObject
import com.example.datdt.scanningsdk2D.models.ShelfObject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

object CameraSdk {
    @Composable
    fun StartDetection(modifier: Modifier = Modifier.fillMaxSize(), modelType: ModelType = ModelType.DEFAULT) {
        CameraScreen(modifier = modifier, modelType = modelType)
    }

    fun launchCamera(context: Context, modelType: ModelType = ModelType.DEFAULT) {
        CameraActivity.start(context, modelType)
    }
}

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelType = intent?.getSerializableExtra(EXTRA_MODEL_TYPE) as? ModelType ?: ModelType.DEFAULT

        setContent {
            CameraSdk.StartDetection(modelType = modelType)
        }
    }

    companion object {
        private const val EXTRA_MODEL_TYPE = "extra_model_type"

        fun start(context: Context, modelType: ModelType = ModelType.DEFAULT) {
            val intent = Intent(context, CameraActivity::class.java)
            intent.putExtra(EXTRA_MODEL_TYPE, modelType)
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(modifier: Modifier = Modifier.fillMaxSize(), modelType: ModelType) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold { padding ->
        TopAppBar(
            modifier = modifier.padding(padding),
            title = {
                Text(modelType.name)
            }
        )
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                context,
                lifecycleOwner,
                cameraExecutor = Executors.newSingleThreadExecutor(),
                modelInfo = modelType.getModelInfo(),
            )
        } else {
            Permission(cameraPermissionState)
        }
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("ContextCastToActivity")
@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    modelInfo: ModelInfo,
) {
    val coroutineScope = MainScope()
    val activity = LocalContext.current as? Activity
    var isScanning by remember { mutableStateOf(false) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var preview by remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
    val executor = ContextCompat.getMainExecutor(context)
    val displayRotationHelper = DisplayRotationHelper(activity)

    val _objectResultsTemp = MutableStateFlow<List<DetectionObject>>(emptyList())
//    val objectResultsTemp: StateFlow<List<DetectionObject>> = _objectResultsTemp.asStateFlow()
    val objectResultsAdd = remember { mutableStateListOf<DetectionObject>() }
    val objectResultsAll = remember { mutableStateListOf<DetectionObject>() }
    val shelfResultsAll = remember { mutableStateListOf<ShelfObject>() }
    val bayResultsAll = remember { mutableStateListOf<BayObject>() }
    val labelResultsAll = remember { mutableStateListOf<LabelObject>() }

    val paint = Paint()
    val pathColorList = listOf(
        Color.Black,  // 0.0 white
        Color.DarkGray,  // 0.333 white
        Color.LightGray,  // 0.667 white
        Color.White,  // 1.0 white
        Color.Gray,  // 0.5 white
        Color.Red,  // 1.0, 0.0, 0.0 RGB
        Color.Green,  // 0.0, 1.0, 0.0 RGB
        Color.Blue,  // 0.0, 0.0, 1.0 RGB
        Color.Cyan,  // 0.0, 1.0, 1.0 RGB
        Color.Yellow,  // 1.0, 1.0, 0.0 RGB
        Color.Magenta,  // 1.0, 0.0, 1.0 RGB
        Color(1f, 0.5f, 0f),  // 1.0, 0.5, 0.0 RGB
        Color(0.5f, 0.0f, 0.5f),  // 0.5, 0.0, 0.5 RGB
        Color(0.6f, 0.4f, 0.2f),  // 0.6, 0.4, 0.2 RGB
    )
    val pathColorListInt = listOf(
                android.graphics.Color.BLACK,  // 0.0 white
        android.graphics.Color.DKGRAY,  // 0.333 white
        android.graphics.Color.LTGRAY,  // 0.667 white
        android.graphics.Color.WHITE,  // 1.0 white
        android.graphics.Color.GRAY,  // 0.5 white
        android.graphics.Color.RED,  // 1.0, 0.0, 0.0 RGB
        android.graphics.Color.GREEN,  // 0.0, 1.0, 0.0 RGB
        android.graphics.Color.BLUE,  // 0.0, 0.0, 1.0 RGB
        android.graphics.Color.CYAN,  // 0.0, 1.0, 1.0 RGB
        android.graphics.Color.YELLOW,  // 1.0, 1.0, 0.0 RGB
        android.graphics.Color.MAGENTA,  // 1.0, 0.0, 1.0 RGB
        android.graphics.Color.rgb(1f, 0.5f, 0f),  // 1.0, 0.5, 0.0 RGB
        android.graphics.Color.rgb(0.5f, 0.0f, 0.5f),  // 0.5, 0.0, 0.5 RGB
        android.graphics.Color.rgb(0.6f, 0.4f, 0.2f),  // 0.6, 0.4, 0.2 RGB
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val boxConstraint = this
        val sizeWidth = with(LocalDensity.current) { boxConstraint.maxWidth.toPx() }
        val sizeHeight = with(LocalDensity.current) { boxConstraint.maxHeight.toPx() }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                _objectResultsTemp.value = emptyList()
                Log.d("herehere", "${isScanning}")
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetRotation(Surface.ROTATION_0)
                            .build()

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setTargetRotation(Surface.ROTATION_0)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        previewView.doOnLayout {
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalyzer
                            )

                            val cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
                            val imageRotation =
                                displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
                            imageAnalyzer.setAnalyzer(
                                cameraExecutor, ObjectDetectorHelper(
                                    context = context,
                                    modelInfo = modelInfo,
                                    resultViewSize = Size(640, 640),
                                    activity = activity,
                                    imageRotation = imageRotation, // Set correctly for your use case
                                    screenWidth = sizeWidth.toInt(),
                                    screenHeight = sizeHeight.toInt()
                                ) { detectionObjects ->
                                    // handle detection results here
                                    _objectResultsTemp.value = detectionObjects
                                })
                        }
                    }, executor)
                preview = androidx.camera.core.Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                previewView
            }
        )
        val objectResultsTemp by _objectResultsTemp.collectAsState()
        LaunchedEffect(objectResultsTemp) {
                delay(50)
            if (isScanning && objectResultsTemp.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    for (obj in objectResultsTemp) {
                        var overlap = false
                        var overlap_shelf = false
                        val obj_bbox = obj.boundingBox

                        if (obj.label == "bay") {
                            // Bay logic here...
                        } else if (obj.label == "label") {
                            synchronized(labelResultsAll) {
                                for (item in labelResultsAll) {
                                    val item_bbox = item.boundingBox
                                    if (item_bbox != null) {
                                        val distance = calculateIOU(obj_bbox, item_bbox)
                                        if (distance >= 0.15f) overlap = true
                                        if (abs(item_bbox.centerY() - obj_bbox.centerY()) < 150f) overlap_shelf =
                                            true
                                    }
                                }
                                if (!overlap) {
                                    labelResultsAll.add(LabelObject(obj.boundingBox))
                                }
                            }
                            synchronized(shelfResultsAll) {
                                if (!overlap_shelf) {
                                    shelfResultsAll.add(ShelfObject(obj.boundingBox))
                                }
                            }
                        } else if (obj.label == "shelf stripping") {
                            overlap = true
                        } else {
                            synchronized(objectResultsAll) {
                                for (i in objectResultsAll.indices) {
                                    val item_bbox = objectResultsAll[i].boundingBox
                                    if (item_bbox != null) {
                                        val distance = calculateIOU(obj_bbox, item_bbox)
                                        if (distance >= 0.15f) {
                                            overlap = true
                                        }
                                    }
                                }
                                if (!overlap) {
                                    objectResultsAll.add(obj)
                                }
                            }
                        }
                    }
                    Log.d("ObjectDetectorHelper", "Detections processed: ${objectResultsAll.size}")
                }
            }
        }
//            Log.d("Filtering", "Filtering detections")
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    if (isScanning) {
                        objectResultsAll.toList().mapIndexed { i, detectionObject ->
//                            Log.d(
//                                "ObjectDetections",
//                                detectionObject.label + " --- " + detectionObject.score
//                            )

//                        if (detectionObject.categories[0].score > 0.65f) {
//                            if (!objectList.contains(detectionObject.label)) {
//                                objectList.add(detectionObject.label)
//                            }
//                        }

                            paint.apply {
                                color = pathColorListInt[5]
                                style = Paint.Style.FILL
                                isAntiAlias = true
                                textSize = 25f
                            }

                            drawRect(
                                color = pathColorList[5],
                                topLeft = Offset(
                                    x = detectionObject.boundingBox.left,
                                    y = detectionObject.boundingBox.top
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    width = detectionObject.boundingBox.width(),
                                    height = detectionObject.boundingBox.height()
                                ),
                                style = Stroke(width = 3.dp.toPx())
                            )

                            drawIntoCanvas {
                                it.nativeCanvas.drawText(
                                    detectionObject.labelDisplay,
                                    detectionObject.boundingBox.left,            // x-coordinates of the origin (top left)
                                    detectionObject.boundingBox.top + 1f, // y-coordinates of the origin (top left)
                                    paint
                                )
                            }
                        }
                    }
                }
            )
        // --- Overlay Buttons ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(70.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { isScanning = !isScanning }, modifier = Modifier.widthIn(min = 100.dp)) {
                Text(if (isScanning) "Stop" else "Start")
            }

            Button(
                onClick = {
                    Log.d("Button", "Reset button pressed")
                    synchronized(objectResultsAll) { objectResultsAll.clear() }
                    synchronized(shelfResultsAll) { shelfResultsAll.clear() }
                    synchronized(labelResultsAll) { labelResultsAll.clear() }
                }, modifier = Modifier.widthIn(min = 100.dp)
            ) {
                Text("Reset")
            }
        }
    }
}

fun dynamicThreshold(boundingBox: RectF, baseThreshold: Float = 67f): Float {
    val avgSize = (boundingBox.width() + boundingBox.height()) / 2f
    return baseThreshold * (avgSize / 100f)  // You can tune the divisor (e.g., 100f) experimentally
}

private fun calculateIOU(box1: RectF, box2: RectF): Float {
    val x1 = max(box1.left, box2.left)
    val y1 = max(box1.top, box2.top)
    val x2 = min(box1.right, box2.right)
    val y2 = min(box1.bottom, box2.bottom)

    val intersectionArea = max(0f, x2-x1) * max(0f, y2-y1)
    val box1Area = box1.width() * box1.height()
    val box2Area = box2.width() * box2.height()

    return intersectionArea / (box1Area + box2Area - intersectionArea)
}


//----------------------------- PERMISSION --------------------------------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Permission(
    cameraPermissionState: PermissionState
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!cameraPermissionState.status.isGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
                    "The camera is important for this app.\n Please grant the permission."
                } else {
                    "Camera not available"
                }
                Text(
                    textToShow,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    shape = CircleShape,
                    onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                    Icon(
                        painterResource(id = R.drawable.ic_baseline_camera_24),
                        contentDescription = "Icon camera",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
