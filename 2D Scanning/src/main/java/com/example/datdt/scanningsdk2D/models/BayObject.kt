package com.example.datdt.scanningsdk2D.models

import com.google.ar.core.Anchor

data class BayObject(
    var id: Int = 0,
    var endpointLeft: Float? = null,
    var endpointRight: Float? = null
)