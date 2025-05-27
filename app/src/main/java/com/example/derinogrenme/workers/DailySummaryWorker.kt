package com.example.derinogrenme.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.derinogrenme.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.util.Calendar
import android.util.Log

class DailySummaryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return Result.failure()
            
            val db = FirebaseFirestore.getInstance()
            
            // Bugünün başlangıcını hesapla
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = Timestamp(calendar.time)
            
            Log.d("DailySummaryWorker", "Sorgu başlatılıyor - userId: $userId, startOfDay: $startOfDay")
            
            // Bugünkü tahminleri getir
            val predictions = db.collection("predictions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val predictionCount = predictions.size()
            if (predictionCount > 0) {
                // Güven oranlarının ortalamasını hesapla
                val totalConfidence = predictions.sumOf { doc ->
                    val confidence = doc.getDouble("confidence") ?: 0.0
                    Log.d("DailySummaryWorker", "Döküman confidence değeri: $confidence")
                    confidence
                }
                val averageConfidence = (totalConfidence / predictionCount * 100).toInt()
                
                Log.d("DailySummaryWorker", "Tahmin istatistikleri - Toplam: $predictionCount, Toplam Güven: $totalConfidence, Ortalama Güven: $averageConfidence")

                // Bildirimi göster
                NotificationHelper(context).showDailySummaryNotification(predictionCount, averageConfidence)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("DailySummaryWorker", "Hata oluştu", e)
            return Result.failure()
        }
    }
} 