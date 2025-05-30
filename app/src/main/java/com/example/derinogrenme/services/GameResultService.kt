package com.example.derinogrenme.services

import android.util.Log
import com.example.derinogrenme.models.GameResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GameResultService {
    private val TAG = "GameResultService"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gameResultsCollection = db.collection("game_results")

    suspend fun saveGameResult(score: Int, correctAnswers: Int, wrongAnswers: Int, totalTime: Int): Result<GameResult> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "Kullanıcı giriş yapmamış")
                return Result.failure(Exception("Kullanıcı giriş yapmamış"))
            }
            
            Log.d(TAG, "Oyun sonucu kaydediliyor... UserId: $userId, Score: $score")
            
            val gameResult = GameResult(
                userId = userId,
                score = score,
                correctAnswers = correctAnswers,
                wrongAnswers = wrongAnswers,
                totalTime = totalTime
            )

            val docRef = gameResultsCollection.add(gameResult).await()
            val savedResult = gameResult.copy(id = docRef.id)
            
            Log.d(TAG, "Oyun sonucu başarıyla kaydedildi. DocId: ${docRef.id}")
            Result.success(savedResult)
        } catch (e: Exception) {
            Log.e(TAG, "Oyun sonucu kaydedilirken hata oluştu", e)
            Result.failure(e)
        }
    }

    suspend fun getUserGameResults(): Result<List<GameResult>> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "Kullanıcı giriş yapmamış")
                return Result.failure(Exception("Kullanıcı giriş yapmamış"))
            }
            
            Log.d(TAG, "Kullanıcı oyun sonuçları getiriliyor... UserId: $userId")
            
            val snapshot = gameResultsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GameResult::class.java)
            }
            
            Log.d(TAG, "${results.size} adet oyun sonucu bulundu")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Oyun sonuçları getirilirken hata oluştu", e)
            Result.failure(e)
        }
    }

    suspend fun getTopScores(limit: Int = 10): Result<List<GameResult>> {
        return try {
            Log.d(TAG, "En yüksek skorlar getiriliyor... Limit: $limit")
            
            val snapshot = gameResultsCollection
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                doc.toObject(GameResult::class.java)
            }
            
            Log.d(TAG, "${results.size} adet yüksek skor bulundu")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Yüksek skorlar getirilirken hata oluştu", e)
            Result.failure(e)
        }
    }
} 