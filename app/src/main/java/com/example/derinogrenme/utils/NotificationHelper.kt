package com.example.derinogrenme.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.derinogrenme.MainActivity
import com.example.derinogrenme.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_DAILY_SUMMARY = "daily_summary"
        const val CHANNEL_ID_REMINDER = "reminder"
        const val NOTIFICATION_ID_DAILY_SUMMARY = 1
        const val NOTIFICATION_ID_REMINDER = 2
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Günlük özet bildirimi kanalı
            val dailySummaryChannel = NotificationChannel(
                CHANNEL_ID_DAILY_SUMMARY,
                "Günlük Özet",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Günlük tahmin özeti bildirimleri"
            }

            // Hatırlatma bildirimi kanalı
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Hatırlatmalar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tahmin yapma hatırlatmaları"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(dailySummaryChannel, reminderChannel))
        }
    }

    fun showDailySummaryNotification(predictionCount: Int, confidenceRate: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Günün Özeti")
            .setContentText("Bugün $predictionCount tahmin yaptın. Güven oranı: %$confidenceRate")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_DAILY_SUMMARY, notification)
    }

    fun showReminderNotification() {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Bugün tahmin yaptın mı?")
            .setContentText("Bir tahmin yapmak için şimdi uygulamaya gel!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }
} 