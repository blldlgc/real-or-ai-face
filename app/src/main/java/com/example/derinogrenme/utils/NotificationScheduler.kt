package com.example.derinogrenme.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.derinogrenme.workers.DailySummaryWorker
import com.example.derinogrenme.workers.ReminderWorker
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    companion object {
        private const val DAILY_SUMMARY_WORK = "daily_summary_work"
        private const val REMINDER_WORK = "reminder_work"
    }

    fun scheduleNotifications() {
        scheduleDailySummary()
        scheduleReminder()
    }

    private fun scheduleDailySummary() {
        // Her gün saat 20:00'de günlük özet bildirimi
        val dailySummaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_SUMMARY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailySummaryRequest
        )
    }

    private fun scheduleReminder() {
        // Her 24 saatte bir hatırlatma bildirimi
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    fun cancelNotifications() {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(DAILY_SUMMARY_WORK)
            cancelUniqueWork(REMINDER_WORK)
        }
    }
} 