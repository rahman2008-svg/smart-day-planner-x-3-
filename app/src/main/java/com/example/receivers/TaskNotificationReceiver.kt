package com.example.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class TaskNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""

        val channelId = "TASK_CHANNEL_PRO"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel starting from Android 8.0 O (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smart Day Planner Pro Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications regarding task start times, pre-alerts, and overdue statuses"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap action: Open MainActivity
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle: String
        val notificationText: String
        var iconId = android.R.drawable.ic_lock_idle_alarm

        when (action) {
            "ACTION_PRE_REMINDER" -> {
                notificationTitle = "⏳ Pre-Reminder: $title"
                notificationText = "Starts in 15 minutes. $desc"
            }
            "ACTION_MAIN_REMINDER" -> {
                notificationTitle = "🔥 Task Alert: $title"
                notificationText = "It is time! Starting now. $desc"
                iconId = android.R.drawable.ic_dialog_info
            }
            "ACTION_OVERDUE_ALERT" -> {
                notificationTitle = "🚨 Overdue Alert: $title"
                notificationText = "You have missed this critical task: $title"
            }
            "ACTION_RECOVERY_ALERT" -> {
                notificationTitle = "🧠 Smart Auto Recovery"
                notificationText = "আপনি এই টাস্কটি মিস করেছেন — এখন পরবর্তী ফ্রি স্লটে রাখা হয়েছে!"
            }
            else -> {
                notificationTitle = title
                notificationText = desc
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconId)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(tapPendingIntent)

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
