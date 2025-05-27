package com.example.derinogrenme.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.derinogrenme.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return Result.failure()
            
            val db = FirebaseFirestore.getInstance()
            val now = LocalDateTime.now(ZoneId.systemDefault())
            val dayAgo = now.minusDays(1)
            
            // Son 24 saatteki tahminleri kontrol et
            val lastPrediction = db.collection("predictions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", dayAgo)
                .orderBy("timestamp")
                .limit(1)
                .get()
                .await()

            // Eğer son 24 saatte tahmin yapılmamışsa bildirim gönder
            if (lastPrediction.isEmpty) {
                NotificationHelper(context).showReminderNotification()
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
} 