package com.example.domain.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Task
import com.example.receivers.TaskNotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarmsForTask(context: Context, task: Task) {
        if (task.isCompleted || task.isMissed) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            sdf.parse(task.dateString)
        } catch (e: Exception) {
            null
        } ?: return

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, task.startHour)
        calendar.set(Calendar.MINUTE, task.startMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val mainTimeMs = calendar.timeInMillis
        val preTimeMs = mainTimeMs - (15 * 60 * 1000)

        val currentTimeMs = System.currentTimeMillis()

        // 1. Schedule Main Reminder
        if (mainTimeMs > currentTimeMs) {
            val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
                action = "ACTION_MAIN_REMINDER"
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_DESC", task.description)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id * 2).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, mainTimeMs, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, mainTimeMs, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, mainTimeMs, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, mainTimeMs, pendingIntent)
            }
        }

        // 2. Schedule Pre-Reminder (15 min early)
        if (preTimeMs > currentTimeMs) {
            val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
                action = "ACTION_PRE_REMINDER"
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_DESC", task.description)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (task.id * 2).toInt() + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTimeMs, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, preTimeMs, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTimeMs, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, preTimeMs, pendingIntent)
            }
        }
    }

    fun cancelAlarmsForTask(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel Main
        val mainIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = "ACTION_MAIN_REMINDER"
        }
        val mainPi = PendingIntent.getBroadcast(
            context,
            (task.id * 2).toInt(),
            mainIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (mainPi != null) {
            alarmManager.cancel(mainPi)
        }

        // Cancel Pre
        val preIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
            action = "ACTION_PRE_REMINDER"
        }
        val prePi = PendingIntent.getBroadcast(
            context,
            (task.id * 2).toInt() + 1,
            preIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (prePi != null) {
            alarmManager.cancel(prePi)
        }
    }
}
