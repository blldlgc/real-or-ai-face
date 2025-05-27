package com.example.derinogrenme.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.derinogrenme.MainActivity
import com.example.derinogrenme.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Yeni FCM token: $token")
        // Token'ı Firestore'a kaydet
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCMService", "Bildirim alındı: ${message.data}")

        // Bildirim verilerini al
        val title = message.notification?.title ?: "Yeni Bildirim"
        val body = message.notification?.body ?: ""
        val data = message.data

        // Bildirimi göster
        showNotification(title, body, data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = "fcm_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Bildirim kanalını oluştur (Android 8.0 ve üzeri için)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FCM Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Firebase Cloud Messaging bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Ana aktiviteye yönlendirme için intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Ekstra verileri intent'e ekle
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Bildirimi oluştur
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Bildirimi göster
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun saveTokenToFirestore(token: String) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCMService", "FCM token başarıyla kaydedildi")
            }
            .addOnFailureListener { e ->
                Log.e("FCMService", "FCM token kaydedilirken hata oluştu", e)
            }
    }
} 