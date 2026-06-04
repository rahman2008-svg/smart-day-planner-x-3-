package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Task
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.domain.smartengine.SmartEngine
import com.example.domain.notification.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(db.taskDao())
    private val settingsRepository = SettingsRepository(application)

    private val _errorLogs = MutableStateFlow<List<String>>(emptyList())
    val errorLogs = _errorLogs.asStateFlow()

    val allTasks: StateFlow<List<Task>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val theme = settingsRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val xp = settingsRepository.xpFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val level = settingsRepository.levelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val streak = settingsRepository.streakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val maxStreak = settingsRepository.maxStreakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val focusScore = settingsRepository.focusScoreFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val missedPenalties = settingsRepository.missedPenaltyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFocusMinutes = settingsRepository.totalFocusMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Global Pomodoro Timer State
    private val _timerSecondsLeft = MutableStateFlow(25 * 60)
    val timerSecondsLeft = _timerSecondsLeft.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _activeTimerMode = MutableStateFlow("FOCUS") // FOCUS, BREAK
    val activeTimerMode = _activeTimerMode.asStateFlow()

    fun setTimerSecondsLeft(seconds: Int) {
        _timerSecondsLeft.value = seconds
    }

    fun setIsTimerRunning(running: Boolean) {
        _isTimerRunning.value = running
    }

    fun setActiveTimerMode(mode: String) {
        _activeTimerMode.value = mode
    }

    fun decrementTimer() {
        if (_timerSecondsLeft.value > 0) {
            _timerSecondsLeft.value -= 1
        }
    }

    private fun logCrashPrevention(e: Exception, context: String) {
        val message = "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)}] Error at $context: ${e.localizedMessage}"
        val logs = _errorLogs.value.toMutableList()
        logs.add(0, message)
        _errorLogs.value = logs
    }

    fun addTask(
        title: String,
        description: String,
        dateString: String,
        startHour: Int,
        startMinute: Int,
        duration: Int,
        recurrence: String,
        category: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val kwPriority = SmartEngine.getKeywordPriority(title)
                var tempTask = Task(
                    title = title,
                    description = description,
                    priority = kwPriority,
                    dateString = dateString,
                    startHour = startHour,
                    startMinute = startMinute,
                    durationMinutes = duration,
                    recurrence = recurrence,
                    category = category
                )
                val finalScore = SmartEngine.calculateScore(tempTask)
                tempTask = tempTask.copy(score = finalScore)

                val id = taskRepository.insertTask(tempTask)
                val scheduledTask = tempTask.copy(id = id)
                AlarmScheduler.scheduleAlarmsForTask(getApplication(), scheduledTask)
            } catch (e: Exception) {
                logCrashPrevention(e, "addTask")
            }
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            try {
                if (task.isCompleted) {
                    val updated = task.copy(isCompleted = false)
                    taskRepository.updateTask(updated)
                    AlarmScheduler.scheduleAlarmsForTask(getApplication(), updated)
                } else {
                    val xpBonus = if (task.priority == 3) 20 else 10
                    val updated = task.copy(isCompleted = true, isMissed = false)
                    taskRepository.updateTask(updated)
                    AlarmScheduler.cancelAlarmsForTask(getApplication(), updated)
                    
                    settingsRepository.addXp(xpBonus)
                    settingsRepository.incrementStreak()
                }
            } catch (e: Exception) {
                logCrashPrevention(e, "toggleTaskCompleted")
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                AlarmScheduler.cancelAlarmsForTask(getApplication(), task)
                taskRepository.deleteTaskById(task.id)
            } catch (e: Exception) {
                logCrashPrevention(e, "deleteTask")
            }
        }
    }

    fun triggerSmartDailyAutoPlanner(dateString: String) {
        viewModelScope.launch {
            try {
                val currentTasks = allTasks.value.filter { it.dateString == dateString && !it.isCompleted }
                if (currentTasks.isEmpty()) return@launch

                val planned = SmartEngine.autoSchedule(currentTasks, dateString)
                for (task in planned) {
                    taskRepository.updateTask(task)
                    AlarmScheduler.scheduleAlarmsForTask(getApplication(), task)
                }
            } catch (e: Exception) {
                logCrashPrevention(e, "triggerSmartDailyAutoPlanner")
            }
        }
    }

    fun resolveDayConflicts(dateString: String) {
        viewModelScope.launch {
            try {
                val currentTasks = allTasks.value.filter { it.dateString == dateString && !it.isCompleted }
                if (currentTasks.isEmpty()) return@launch

                val resolved = SmartEngine.resolveConflicts(currentTasks)
                for (task in resolved) {
                    taskRepository.updateTask(task)
                    AlarmScheduler.scheduleAlarmsForTask(getApplication(), task)
                }
            } catch (e: Exception) {
                logCrashPrevention(e, "resolveDayConflicts")
            }
        }
    }

    fun updateTheme(newTheme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(newTheme)
        }
    }

    fun completeFocusSession(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.addFocusMinutes(minutes)
                settingsRepository.addXp(minutes)
            } catch (e: Exception) {
                logCrashPrevention(e, "completeFocusSession")
            }
        }
    }

    fun processVoiceInput(voiceText: String, dateString: String) {
        viewModelScope.launch {
            try {
                val text = voiceText.lowercase()
                
                var title = voiceText
                var hour = 9
                var minute = 0
                var duration = 60
                
                val timeMatch = Regex("at\\s*(\\d{1,2})(:?(\\d{2}))?").find(text)
                if (timeMatch != null) {
                    hour = timeMatch.groupValues[1].toInt().coerceIn(0, 23)
                    val minStr = timeMatch.groupValues[3]
                    minute = if (minStr.isNotEmpty()) minStr.toInt().coerceIn(0, 59) else 0
                    title = voiceText.substring(0, timeMatch.range.first).trim()
                }

                val durationMatch = Regex("for\\s*(\\d+)\\s*(mins|minutes|m)?").find(text)
                if (durationMatch != null) {
                    duration = durationMatch.groupValues[1].toInt().coerceAtLeast(5)
                    title = title.replace(durationMatch.value, "").trim()
                }

                val cleanTitle = title.replace(Regex("(?i)^(create task|add task|schedule)\\s*"), "").trim()

                addTask(
                    title = if (cleanTitle.isBlank()) "Voice Task Draft" else cleanTitle,
                    description = "Created offline via Voice Prompt",
                    dateString = dateString,
                    startHour = hour,
                    startMinute = minute,
                    duration = duration,
                    recurrence = "NONE",
                    category = "Voice Input"
                )
            } catch (e: Exception) {
                logCrashPrevention(e, "processVoiceInput")
            }
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            settingsRepository.resetStats()
        }
    }

    fun exportTasksAsJson(): String {
        return try {
            val list = allTasks.value
            val array = JSONArray()
            for (t in list) {
                val obj = JSONObject().apply {
                    put("title", t.title)
                    put("description", t.description)
                    put("priority", t.priority)
                    put("score", t.score)
                    put("dateString", t.dateString)
                    put("startHour", t.startHour)
                    put("startMinute", t.startMinute)
                    put("durationMinutes", t.durationMinutes)
                    put("isCompleted", t.isCompleted)
                    put("isMissed", t.isMissed)
                    put("isRescheduled", t.isRescheduled)
                    put("recurrence", t.recurrence)
                    put("category", t.category)
                }
                array.put(obj)
            }
            array.toString(2)
        } catch (e: Exception) {
            logCrashPrevention(e, "exportTasksAsJson")
            ""
        }
    }

    fun importTasksFromJson(jsonString: String): Boolean {
        return try {
            val array = JSONArray(jsonString)
            viewModelScope.launch {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    addTask(
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        dateString = obj.getString("dateString"),
                        startHour = obj.getInt("startHour"),
                        startMinute = obj.getInt("startMinute"),
                        duration = obj.getInt("durationMinutes"),
                        recurrence = obj.optString("recurrence", "NONE"),
                        category = obj.optString("category", "Imported")
                    )
                }
            }
            true
        } catch (e: Exception) {
            logCrashPrevention(e, "importTasksFromJson")
            false
        }
    }
}
