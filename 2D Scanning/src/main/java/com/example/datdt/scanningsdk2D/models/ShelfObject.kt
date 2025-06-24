package com.example.datdt.scanningsdk2D.models

import android.graphics.RectF
import com.google.ar.core.Anchor

data class ShelfObject(
    val boundingBox: RectF,
    var id: Int = 0
)
