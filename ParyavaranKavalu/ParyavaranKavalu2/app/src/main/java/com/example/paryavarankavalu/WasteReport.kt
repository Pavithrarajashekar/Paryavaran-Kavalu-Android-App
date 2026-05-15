package com.example.paryavarankavalu

import android.net.Uri

data class WasteReport(
    val id: String,
    val type: String,
    val description: String,
    val status: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val reporterName: String = "Guardian User",
    val imageUri: Uri? = null
)