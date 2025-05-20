package com.example.derinogrenme.models

import com.google.firebase.firestore.PropertyName

data class Prediction(
    @PropertyName("result")
    val result: String = "", // "REAL" veya "FAKE"
    
    @PropertyName("confidence")
    val confidence: Float = 0f,
    
    @PropertyName("date")
    val date: String = "",
    
    @PropertyName("imageUrl")
    val imageUrl: String? = null
) 