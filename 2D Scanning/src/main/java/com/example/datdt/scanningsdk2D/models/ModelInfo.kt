package com.example.datdt.scanningsdk2D.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelInfo(
    val modelPath: String,
    val labelPath: String,
    val labels_displayPath: String
) : Parcelable
