package com.example.datdt.scanningsdk2D.models

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import com.google.ar.core.Anchor

data class DetectionObject(
    val score: Float,
    val label: String,
    val labelDisplay: String,
    val boundingBox: RectF,
    val cropString: Bitmap?,
    var shelf: Int = 0,
    var facing: Int = 0,
    var bay: Int = 0
//    val centerCoordinate: Point
)
