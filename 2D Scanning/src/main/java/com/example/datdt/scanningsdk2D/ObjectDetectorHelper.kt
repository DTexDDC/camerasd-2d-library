package com.example.datdt.scanningsdk2D

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.media.Image
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.MatrixExt.postRotate
import androidx.core.graphics.createBitmap
import com.example.datdt.scanningsdk2D.models.DetectionObject
import com.example.datdt.scanningsdk2D.models.ModelInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

typealias ObjectDetectorCallback = (image: List<DetectionObject>) -> Unit

class ObjectDetectorHelper (
    private val context: Context,
    private val modelInfo: ModelInfo,
    private val resultViewSize: Size,
    private val activity: Activity?,
    private val imageRotation: Int,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val callback: ObjectDetectorCallback,
    ) : ImageAnalysis.Analyzer {
    val SCORE_THRESHOLD: Float = 0.55f;
    val NUM_CHANNELS: Int = 34;
    val NUM_ELEMENTS: Int = 8400

    private var interpreter: Interpreter? = null
    private var detectedProducts = mutableListOf<DetectionObject>()
    private val labels = mutableListOf<String>()
    private val labelsDisplay = mutableListOf<String>()
    private var inputBitmap: Bitmap? = null
//    private var shrunkBitmap: Bitmap? = null
    private var RGBByteBuffer: ByteBuffer? = null

   private var rgbConverter: YuvToRgbConverter? = null

    //For bay model.
    private var bayInterpreter: Interpreter? = null

    init {
        setup()
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (image.image == null) return
        val cameraRGB: Bitmap = createBitmap(image.image!!.width, image.image!!.height)
        rgbConverter?.yuvToRgb(image.image!!, cameraRGB)
        val shrunkBitmap = shrinkBitmap(cameraRGB, imageRotation) //A bitmap of RGBA 640x640 size.
        RGBByteBuffer =
            RGBATORGBArray(shrunkBitmap) //A bitmap of RGB 640x640 size. THIS SHOULD NORMALISE AS WELL.
        overviewImage = shrunkBitmap
        detect(RGBByteBuffer!!, screenWidth, screenHeight, shrunkBitmap)

        image.close()
    }

    public fun clearDetectedProducts() {
        detectedProducts.clear()
    }

    private fun setup() {
//        val tfLiteOptions = Interpreter.Options()
////            addDelegate(NnApiDelegate())
////            if (CompatibilityList().isDelegateSupportedOnThisDevice) {
////                addDelegate(GpuDelegate(CompatibilityList().bestOptionsForThisDevice))
////            } else {
////                Log.w("MyApp", "GPU Delegate is not supported on this device.")
////            }
//            .setNumThreads(7)
//        interpreter =
//            Interpreter(FileUtil.loadMappedFile(context, modelInfo.modelPath), tfLiteOptions)
//        interpreter?.allocateTensors()
        labels.clear()
        labelsDisplay.clear()
        labels.addAll(FileUtil.loadLabels(context, modelInfo.labelPath))
        labelsDisplay.addAll(FileUtil.loadLabels(context, modelInfo.labels_displayPath))

        rgbConverter = activity?.let { YuvToRgbConverter(it) }
        val options1 = Interpreter.Options().setNumThreads(7)
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelInfo.modelPath), options1)
        interpreter?.allocateTensors()
        val options2 = Interpreter.Options().setNumThreads(3)
        bayInterpreter = Interpreter(FileUtil.loadMappedFile(context, "mixers-endpoints-tfl-v2_float32.tflite"), options2)
        bayInterpreter?.allocateTensors()
        //Setup the bay detector.
//        tfLiteOptions.setNumThreads(3)
//        bayInterpreter = Interpreter(FileUtil.loadMappedFile(context, "mixers-endpoints-tfl-v2_float32.tflite"), tfLiteOptions)
    }

    private fun detect(imageArray: ByteBuffer, screenWidth: Int, screenHeight: Int, bitmap: Bitmap) {
        val outputBuffer = ByteBuffer.allocateDirect(NUM_ELEMENTS* NUM_CHANNELS * 4).order(ByteOrder.nativeOrder())

        //Output buffer for the bay endpoints.
        val outputBayBuffer = ByteBuffer.allocateDirect(5*8400*4).order(ByteOrder.nativeOrder())
        try {
            interpreter?.run(imageArray, outputBuffer)
            bayInterpreter?.run(imageArray, outputBayBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        outputBuffer.rewind()
        outputBayBuffer.rewind()
        val outputArray = FloatArray(NUM_CHANNELS* NUM_ELEMENTS)
        val outputBayArray = FloatArray(5*8400)
        outputBuffer.asFloatBuffer().get(outputArray)
        outputBayBuffer.asFloatBuffer().get(outputBayArray)

        if (outputArray.isEmpty() || outputArray.size == 1) {
            callback(mutableListOf())
        }

        val filteredBoxes = filterBox(outputArray, screenWidth, screenHeight, bitmap)
//        val filteredBays = filterBays(outputBayArray, screenWidth, screenHeight, 90)
//        for (obj in filteredBays) {
//            filteredBoxes.add(obj)
////            Log.d("Bay Update", "${obj.boundingBox.left}")
//        }
//        Log.d("Detection Number", "${filteredBoxes.size}")
        callback(filteredBoxes)
//        return filteredBoxes
    }

    private fun filterBays(outputBayArray: FloatArray, screenWidth: Int, screenHeight: Int, imageRotation: Int): MutableList<DetectionObject> {
        val boxes: MutableList<DetectionObject> = mutableListOf()
        val transposedArray: MutableList<Float> = MutableList(5*8400) { 0F }
//        val result = inferenceResult.flatten().flatMap { it.toList() }
        for (row in 0 until 5) {
            for (col in 0 until 8400) {
                val oldIndex = row * 8400 + col
                val newIndex = col * 5 + row
                transposedArray[newIndex] = outputBayArray[oldIndex]
            }
        }

        val outputRow = 8400
        val outputColumn = 5
        var val_count = 0
        var one_cx: Float = 0F
        for (i in 0 until outputRow) {
            var maxConfidence = SCORE_THRESHOLD
            var labelIndex = 0
            for (j in 0 until outputColumn - 4) { //Fix this up
                if (transposedArray[i * outputColumn + 4 + j] > maxConfidence) {
                    maxConfidence = transposedArray[i * outputColumn + 4 + j]
                    labelIndex = j
                }
            }
//            val x = result[i]
//            val y = result[i+8400]
            if (maxConfidence >= SCORE_THRESHOLD) {
//                Log.d("TAG", "BayBox: ${maxConfidence}")
                var cx = transposedArray[i * outputColumn]* 640f* (screenWidth / 640f)
                var cy = transposedArray[i * outputColumn + 1]* 640f* (screenWidth / 640f)
                var w = transposedArray[i * outputColumn + 2]* 640f* (screenWidth / 640f)
                var h = transposedArray[i * outputColumn + 3]* 640f* (screenWidth / 640f)
                var x1 = cx - w/2
                var y1 = cy - h/2
                var x2 = cx + w/2
                var y2 = cy + h/2
                val boundingBox = RectF(
                    x1,
                    y1,
                    x2,
                    y2
                )
//                val coords = Point(
//                    boundingBox.centerY().toInt(),
//                    boundingBox.centerX().toInt()
//                )
//                val rotatedCoordinates =
//                    coords.rotateCoordinates(screenWidth, screenHeight, imageRotation)
                val left = boundingBox.left
                val top = boundingBox.top
//                Log.d("TAG", "filterBox: $left, $cy, $h, $top")
                val detection = DetectionObject(
                    maxConfidence,
                    "bay",
                    "bay",
                    boundingBox,
                    null
                )
                if (one_cx == 0F) {
                    one_cx = cx
                    boxes.add(detection)
                    val_count += 1
                } else {
                    if (cx <= 0.8*one_cx || cx >= 1.2*one_cx) {
                        val_count += 1
                        boxes.add(detection)
                        Log.d("Bay Update", "Updated")
                    }
                }
                if (val_count == 2) {
                    break
                }
            }
        }
        return boxes
    }

//    private fun runInference(bitmap: Bitmap): Array<Array<FloatArray>> {
//        val outputArr = Array(1) {
//            Array(NUM_CHANNELS) {
//                FloatArray(NUM_ELEMENTS)
//            }
//        }
//        val byteBuffer = convertBitmapToByteBuffer(bitmap, 640, 640)
//
////        val probabilityTenBuffer =
////            TensorBuffer.createFixedSize(intArrayOf(1, 34, 8400), DataType.FLOAT32)
//
//        interpreter?.run(byteBuffer, outputArr)
//
//        val outputTensor = interpreter?.getOutputTensor(0)
//
////        Log.d("TAG", "runInference: ${outputTensor?.dataType()}")
//        return outputArr
//    }

    private fun createString(map: Bitmap, x: Float, y: Float, height: Float, width: Float): Bitmap {
        val crop = Bitmap.createBitmap(map, x.toInt(), y.toInt(), (width).toInt(), (height).toInt())
//        val stream = ByteArrayOutputStream()
//        crop.compress(Bitmap.CompressFormat.JPEG, 75, stream)
//        val cropCompressedArray = stream.toByteArray()
//        return Base64.encodeToString(cropCompressedArray, Base64.NO_WRAP)
        return crop
    }

    private fun shrinkBitmap(map: Bitmap, rotation: Int): Bitmap {
        val resizedBitmap = map.scale(640, 640, true)
        val matrix = Matrix().apply {
            postRotate(90f)
        }
        val newBitMap = Bitmap.createBitmap(
            resizedBitmap,
            0,
            0,
            resizedBitmap.width,
            resizedBitmap.height,
            matrix,
            true  // 'true' applies a filtering (smoothing) while rotating
        )
//        saveBitmapToFile(context, newBitMap, "processed_for_model.jpg")
        return newBitMap
    }

    private fun RGBATORGBArray(bitmap: Bitmap): ByteBuffer {
        val IMAGE_SIZE = 640;

        val intValues = IntArray(IMAGE_SIZE*IMAGE_SIZE)
        val floatValues = FloatArray(IMAGE_SIZE * IMAGE_SIZE * 3)

//        if (bitmap.getWidth() != IMAGE_SIZE || bitmap.getHeight() != IMAGE_SIZE) {
//            // rescale the bitmap if needed
//            bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_SIZE, IMAGE_SIZE);
//        }

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (i in 0 until intValues.size) {
            val pix = intValues[i];
            // bitwise shifting - without our image is shaped [1, 168, 168, 1] but we need [1, 168, 168, 3]
            floatValues[i * 3] = Color.red(pix).toFloat()/255
            floatValues[i * 3 + 1] = Color.green(pix).toFloat()/255
            floatValues[i * 3 + 2] = Color.blue(pix).toFloat()/255
//            floatValues[i * 3] = (Color.red(pix).toFloat() / 127.5f) - 1f
//            floatValues[i * 3 + 1] = (Color.green(pix).toFloat() / 127.5f) - 1f
//            floatValues[i * 3 + 2] = (Color.blue(pix).toFloat() / 127.5f) - 1f
        }

        // Calculate the total number of elements in your float array
        val numElements = floatValues.size

// Allocate a direct ByteBuffer to hold all floats (each float is 4 bytes)
        val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(numElements * 4).apply {
            order(ByteOrder.nativeOrder())
        }

// Put each float from floatValues into the ByteBuffer
        for (value in floatValues) {
            inputBuffer.putFloat(value)
        }

// Reset the position of the ByteBuffer to the beginning
        inputBuffer.rewind()

        return inputBuffer
    }

//    private fun convertBitmapToByteBuffer(bitmapIn: Bitmap, width: Int, height: Int): ByteBuffer {
//        val bitmap = Bitmap.createScaledBitmap(
//            bitmapIn,
//            width,
//            height,
//            false
//        ) // convert bitmap into required size
//        // these value can be different for each channel if they are not then you may have single value instead of an array
//        val mean = arrayOf(127.5f, 127.5f, 127.5f)
//        val standard = arrayOf(127.5f, 127.5f, 127.5f)
//        val inputImage = getInputImage(width, height)
//        val intValues = IntArray(width * height)
//        bitmap.getPixels(intValues, 0, width, 0, 0, width, height)
//        for (y in 0 until width) {
//            for (x in 0 until height) {
//                val px = bitmap.getPixel(x, y)
//                // Get channel values from the pixel value.
//                val r = Color.red(px)
//                val g = Color.green(px)
//                val b = Color.blue(px)
//                // Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
//                // For example, some models might require values to be normalized to the range
//                // [0.0, 1.0] instead.
//                val rf = (r - mean[0]) / standard[0]
//                val gf = (g - mean[0]) / standard[0]
//                val bf = (b - mean[0]) / standard[0]
//                //putting in BRG order because this model demands input in this order
//                inputImage.putFloat(bf)
//                inputImage.putFloat(rf)
//                inputImage.putFloat(gf)
//            }
//        }
//        return inputImage
//    }

//    private fun getInputImage(width: Int, height: Int): ByteBuffer {
//        val inputImage = ByteBuffer.allocateDirect(1 * width * height * 3 * 4)
//        inputImage.order(ByteOrder.nativeOrder())
//        inputImage.rewind()
//        return inputImage
//    }
private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String = "processed_image.jpg") {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)  // Saves to Pictures folder
    }

    val contentResolver = context.contentResolver
    val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    imageUri?.let { uri ->
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        println("✅ Image saved to Gallery at URI: $uri")
    } ?: run {
        println("❌ Failed to create new MediaStore record.")
    }
}

    private fun filterBox(result: FloatArray, screenWidth: Int, screenHeight: Int, bitmap: Bitmap): MutableList<DetectionObject> {
        val boxes: MutableList<DetectionObject> = mutableListOf()
        val transposedArray: MutableList<Float> = MutableList(NUM_CHANNELS* NUM_ELEMENTS) { 0F }
//        val result = inferenceResult.flatten().flatMap { it.toList() }
        for (row in 0 until 34) {
            for (col in 0 until 8400) {
                val oldIndex = row * 8400 + col
                val newIndex = col * 34 + row
                transposedArray[newIndex] = result[oldIndex]
            }
        }

        val outputRow = 8400
        val outputColumn = 34
        for (i in 0 until outputRow) {
            var maxConfidence = SCORE_THRESHOLD
            var labelIndex = 0
            for (j in 0 until outputColumn-4) { //Fix this up
                if (transposedArray[i*outputColumn+4+j] > maxConfidence) {
                    maxConfidence = transposedArray[i*outputColumn+4+j]
                    labelIndex = j
                }
            }
//            if ( transposedArray[i*outputColumn]*640f *(screenWidth/640f) < 300f) {
//                Log.d("filtered out", "${transposedArray[i*outputColumn]*640f *(screenWidth/640f) }, ${maxConfidence}")
////                    continue
//            }
//            val x = result[i]
//            val y = result[i+8400]
//            Log.d("TAG", "filterBox: $x, $y")
            if (maxConfidence > SCORE_THRESHOLD) {
                var cx = transposedArray[i*outputColumn]*640f
                var cy = transposedArray[i*outputColumn+1]*640f
                var w = transposedArray[i*outputColumn+2]*640f
                var h = transposedArray[i*outputColumn+3]*640f
                var x1 = cx - w/2
                var y1 = cy - h/2
                var x2 = cx + w/2
                var y2 = cy + h/2
                val boundingBox = RectF(
                    x1*(screenWidth/640f),
                    y1*(screenHeight/640f),
                    x2*(screenWidth/640f),
                    y2*(screenHeight/640f)
                )
//                Log.d("TAG", "filterBox: $left, $cy, $h, $top")
                if (x1 < 0 || y1 < 0 || w < 0 || h < 0) {
                    Log.d("filtered out", "${x1}, ${y1}, ${x2}, ${y2}")
                    continue
                }
                val detection = DetectionObject(
                    maxConfidence,
                    labels[labelIndex],
                    labelsDisplay[labelIndex],
                    boundingBox,
                    createString(bitmap, x1, y1, h/2, w/2)
                )
                boxes.add(detection)
            }
        }


//        callback.invoke(applyNMS(boxes))
        return applyNMS(boxes);
    }

    private fun applyNMS(boxes: List<DetectionObject>): MutableList<DetectionObject> {
        val sortedBoxes = boxes.sortedByDescending { it.score }
        var sortedMutableBoxes = sortedBoxes.toMutableList()
        var selectedBoxes: MutableList<DetectionObject> = mutableListOf<DetectionObject>()

        while (sortedMutableBoxes.isNotEmpty()) {
            val first = sortedMutableBoxes.removeAt(0)
            selectedBoxes.add(first)

            var index = 0
            while (index < sortedMutableBoxes.size) {
                val box = sortedMutableBoxes.get(index)
                val iou = calculateIOU(first, box)
                if (iou >= 0.25f) {
                    sortedMutableBoxes.removeAt(index)
                } else {
                    index += 1
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIOU(box1: DetectionObject, box2: DetectionObject): Float {
        val x1 = max(box1.boundingBox.left, box2.boundingBox.left)
        val y1 = max(box1.boundingBox.top, box2.boundingBox.top)
        val x2 = min(box1.boundingBox.right, box2.boundingBox.right)
        val y2 = min(box1.boundingBox.bottom, box2.boundingBox.bottom)

        val intersectionArea = max(0f, x2-x1) * max(0f, y2-y1)
        val box1Area = box1.boundingBox.width() * box1.boundingBox.height()
        val box2Area = box2.boundingBox.width() * box2.boundingBox.height()

        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }
}