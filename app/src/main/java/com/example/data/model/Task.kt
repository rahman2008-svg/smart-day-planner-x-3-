package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: Int = 0, // 3: High, 2: Medium, 1: Low, 0: Flexible/None
    val score: Int = 0, // calculated priority score based on complex rules
    val dateString: String, // yyyy-MM-dd
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val durationMinutes: Int = 60,
    val isCompleted: Boolean = false,
    val isMissed: Boolean = false,
    val isRescheduled: Boolean = false,
    val originalDateString: String? = null,
    val originalStartHour: Int? = null,
    val originalStartMinute: Int? = null,
    val recurrence: String = "NONE", // NONE, DAILY, WEEKLY_MON, WEEKLY_TUE, etc.
    val category: String = "General",
    val xpRewarded: Int = 10
)
