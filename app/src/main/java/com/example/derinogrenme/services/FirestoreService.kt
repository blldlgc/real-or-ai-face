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
import com.google.firebase.Timestamp

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
                "timestamp" to Timestamp.now(),
                "imageUrl" to prediction.imageUrl
            )

            Log.d("FirestoreService", "Tahmin kaydediliyor... UserId: $userId")
            val docRef = db.collection(predictionsCollection)
                .add(predictionData)
                .await()
            Log.d("FirestoreService", "Tahmin başarıyla kaydedildi. DocId: ${docRef.id}")
        } catch (e: Exception) {
            Log.e("FirestoreService", "Tahmin kaydedilirken hata oluştu", e)
            throw e
        }
    }

    suspend fun getRecentPredictions(userId: String, limit: Int): List<Prediction> {
        return try {
            Log.d("FirestoreService", "Tahminler getiriliyor... UserId: $userId, Limit: $limit")
            
            val query = db.collection(predictionsCollection)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            Log.d("FirestoreService", "Firestore sorgusu oluşturuldu: $query")
            
            val result = query.get().await()
            Log.d("FirestoreService", "Firestore'dan ${result.documents.size} tahmin alındı")

            result.documents.mapNotNull { doc ->
                try {
                    Log.d("FirestoreService", "Döküman işleniyor: ${doc.id}")
                    Log.d("FirestoreService", "Döküman verileri: ${doc.data}")
                    
                    val result = doc.getString("result")
                    val confidence = doc.getDouble("confidence")?.toFloat()
                    val timestamp = doc.getTimestamp("timestamp")
                    val imageUrl = doc.getString("imageUrl") ?: ""

                    if (result == null || confidence == null || timestamp == null) {
                        Log.e("FirestoreService", "Döküman gerekli alanları içermiyor: ${doc.id}")
                        null
                    } else {
                        Prediction(
                            result = result,
                            confidence = confidence,
                            timestamp = timestamp,
                            imageUrl = imageUrl
                        ).also {
                            Log.d("FirestoreService", "Tahmin başarıyla oluşturuldu: $it")
                        }
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