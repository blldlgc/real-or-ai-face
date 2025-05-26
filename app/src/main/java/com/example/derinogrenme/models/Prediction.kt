package com.example.derinogrenme.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Prediction(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val result: String = "",
    val confidence: Float = 0f,
    val timestamp: Timestamp = Timestamp.now()
) 