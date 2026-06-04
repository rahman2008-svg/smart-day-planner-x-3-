package com.example.workers

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.domain.notification.AlarmScheduler
import com.example.receivers.TaskNotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val db = AppDatabase.getDatabase(context)
        val taskRepository = TaskRepository(db.taskDao())
        val settingsRepository = SettingsRepository(context)

        val tasks = taskRepository.getUnfinishedTasks()
        val currentTimeMs = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (task in tasks) {
            val date = try {
                sdf.parse(task.dateString)
            } catch (e: Exception) {
                null
            } ?: continue

            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, task.startHour)
            calendar.set(Calendar.MINUTE, task.startMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val taskStartTimeMs = calendar.timeInMillis
            val oneHourPassedMs = taskStartTimeMs + (60 * 60 * 1000)

            if (currentTimeMs > oneHourPassedMs && !task.isCompleted && !task.isMissed) {
                // Mark original task as missed
                val updatedTask = task.copy(isMissed = true)
                taskRepository.updateTask(updatedTask)
                settingsRepository.applyMissedPenalty()

                // Trigger Missed/Overdue notification
                val overdueIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
                    action = "ACTION_OVERDUE_ALERT"
                    putExtra("TASK_ID", task.id)
                    putExtra("TASK_TITLE", task.title)
                    putExtra("TASK_DESC", task.description)
                }
                context.sendBroadcast(overdueIntent)

                // AUTO RESCHEDULE to 7 PM evening slot tomorrow as per requirements
                val tomorrowCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val tomorrowString = sdf.format(tomorrowCal.time)
                val relocatedTask = task.copy(
                    id = 0,
                    dateString = tomorrowString,
                    startHour = 19, // 7 PM slot
                    startMinute = 0,
                    isCompleted = false,
                    isMissed = false,
                    isRescheduled = true,
                    originalDateString = task.dateString,
                    originalStartHour = task.startHour,
                    originalStartMinute = task.startMinute
                )
                
                val newId = taskRepository.insertTask(relocatedTask)
                val finalRelocated = relocatedTask.copy(id = newId)
                AlarmScheduler.scheduleAlarmsForTask(context, finalRelocated)

                // Trigger recovery alert popup in English with Bangla helper text
                val recoveryIntent = Intent(context, TaskNotificationReceiver::class.java).apply {
                    action = "ACTION_RECOVERY_ALERT"
                    putExtra("TASK_ID", newId)
                    putExtra("TASK_TITLE", task.title)
                }
                context.sendBroadcast(recoveryIntent)
            }
        }

        return Result.success()
    }
}
