package com.example.derinogrenme.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class GameResult(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val totalTime: Int = 0,
    val timestamp: Timestamp = Timestamp.now()
) 