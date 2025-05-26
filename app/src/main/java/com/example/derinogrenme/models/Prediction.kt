package com.example.derinogrenme.models

import com.google.firebase.firestore.PropertyName

data class Prediction(
    @PropertyName("result")
    val result: String, // REAL veya FAKE
    
    @PropertyName("confidence")
    val confidence: Float, // 0.0 - 1.0 arası güven oranı
    
    @PropertyName("date")
    val date: String, // dd/MM/yyyy HH:mm formatında tarih
    
    @PropertyName("imageUrl")
    val imageUrl: String? = null // Resim URL'i (opsiyonel)
) 