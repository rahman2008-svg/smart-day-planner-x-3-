package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.database.AppDatabase
import com.example.domain.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val taskDao = db.taskDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                val unfinished = taskDao.getUnfinishedTasks()
                for (task in unfinished) {
                    AlarmScheduler.scheduleAlarmsForTask(context, task)
                }
            }
        }
    }
}
