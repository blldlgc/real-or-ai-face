package com.example.derinogrenme.services

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.derinogrenme.models.Prediction
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import com.google.firebase.firestore.Query

class FirestoreService {
    private val db: FirebaseFirestore = Firebase.firestore
    private val predictionsCollection = "predictions"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("tr"))

    suspend fun savePrediction(userId: String, prediction: Prediction) {
        try {
            val predictionData = hashMapOf(
                "userId" to userId,
                "result" to prediction.result,
                "confidence" to prediction.confidence,
                "date" to prediction.date,
                "imageUrl" to prediction.imageUrl,
                "timestamp" to Date()
            )

            db.collection(predictionsCollection)
                .add(predictionData)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreService", "Tahmin kaydedilirken hata oluştu", e)
            throw e
        }
    }

    suspend fun getRecentPredictions(userId: String, limit: Int): List<Prediction> {
        return try {
            Log.d("FirestoreService", "Tahminler getiriliyor... UserId: $userId, Limit: $limit")
            
            val result = db.collection(predictionsCollection)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            Log.d("FirestoreService", "Firestore'dan ${result.documents.size} tahmin alındı")

            result.documents.mapNotNull { doc ->
                try {
                    val prediction = doc.toObject(Prediction::class.java)
                    if (prediction == null) {
                        Log.e("FirestoreService", "Döküman Prediction nesnesine dönüştürülemedi: ${doc.id}")
                        null
                    } else {
                        Log.d("FirestoreService", "Tahmin başarıyla dönüştürüldü: ${doc.id}")
                        prediction
                    }
                } catch (e: Exception) {
                    Log.e("FirestoreService", "Döküman dönüştürme hatası: ${doc.id}", e)
                    null
                }
            }.also { predictions ->
                Log.d("FirestoreService", "Toplam ${predictions.size} tahmin başarıyla işlendi")
            }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Tahminler getirilirken hata oluştu", e)
            emptyList()
        }
    }

    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }
} 