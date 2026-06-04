package com.example.domain.smartengine

import com.example.data.model.Task

object SmartEngine {

    // Rule-based heuristic priority
    fun getKeywordPriority(title: String): Int {
        val t = title.lowercase()
        return when {
            t.contains("exam") || t.contains("assignment") || t.contains("test") || t.contains("presentation") -> 3
            t.contains("study") || t.contains("important") || t.contains("work") || t.contains("project") || t.contains("learn") -> 2
            t.contains("gym") || t.contains("shopping") || t.contains("exercise") || t.contains("buy") || t.contains("workout") -> 1
            else -> 0
        }
    }

    // Smart Time Suggestion based on Priority
    fun suggestTime(priority: Int): Pair<Int, Int> {
        return when (priority) {
            3 -> Pair(6, 0)   // Morning 6-9
            2 -> Pair(17, 30) // Evening 5-8
            else -> Pair(12, 0) // Flexible Afternoon
        }
    }

    fun suggestTimeLabel(priority: Int): String {
        return when (priority) {
            3 -> "Morning (6:00 AM - 9:00 AM)"
            2 -> "Evening (5:00 PM - 8:00 PM)"
            else -> "Flexible (Afternoon/Free)"
        }
    }

    // Priority Score formula: Keyword Score (0-30) + Deadline Score (0-10) + Reschedule Score (20) + Recurrence Weight (15)
    fun calculateScore(task: Task): Int {
        var score = getKeywordPriority(task.title) * 10
        
        // deadline weight (closer to 6 AM morning gets slightly higher score as it's start of day)
        score += (24 - task.startHour).coerceIn(0, 10)

        // recurrence bonus
        if (task.recurrence != "NONE") {
            score += 15
        }

        // if escalated/rescheduled, gets higher priority score to ensure it is not missed again
        if (task.isRescheduled) {
            score += 20
        }
        return score
    }

    // Helper to check standard overlap between two tasks on the same day
    fun hasConflict(t1: Task, t2: Task): Boolean {
        if (t1.dateString != t2.dateString) return false
        val s1 = t1.startHour * 60 + t1.startMinute
        val e1 = s1 + t1.durationMinutes

        val s2 = t2.startHour * 60 + t2.startMinute
        val e2 = s2 + t2.durationMinutes

        return !(e1 <= s2 || s1 >= e2)
    }

    // Intelligent auto-rescheduler & conflict resolver
    // Given tasks for a single date, it checks for conflicts sequentially.
    // If a conflict is found, it automatically moves the lower priority or later task to a free slot!
    fun resolveConflicts(tasks: List<Task>): List<Task> {
        if (tasks.size <= 1) return tasks
        
        // Sort by start time, then priority score descending
        val sorted = tasks.sortedWith(compareBy({ it.startHour * 60 + it.startMinute }, { -it.score }))
        val resolved = mutableListOf<Task>()

        for (task in sorted) {
            var currentTask = task
            var tries = 0
            while (tries < 48) { // search within 24h limit (30min increments)
                var conflictFound = false
                for (other in resolved) {
                    if (hasConflict(currentTask, other)) {
                        conflictFound = true
                        break
                    }
                }
                
                if (conflictFound) {
                    // Resolve: move candidate task forward by 30 mins
                    var h = currentTask.startHour
                    var m = currentTask.startMinute + 30
                    if (m >= 60) {
                        h += m / 60
                        m = m % 60
                    }
                    if (h >= 24) {
                        h = 6 // wrap to next morning slot 6AM
                    }
                    currentTask = currentTask.copy(
                        startHour = h,
                        startMinute = m,
                        isRescheduled = true
                    )
                    tries++
                } else {
                    break
                }
            }
            resolved.add(currentTask)
        }
        return resolved
    }

    // Auto Planner: takes unassigned tasks, and auto schedules them into non-overlapping morning/evening/flexible slots
    fun autoSchedule(userTasks: List<Task>, dateString: String): List<Task> {
        val planned = mutableListOf<Task>()
        
        // Separate tasks by priority to award the best slots first
        val highPriority = userTasks.filter { it.priority == 3 }
        val medPriority = userTasks.filter { it.priority == 2 }
        val lowPriority = userTasks.filter { it.priority <= 1 }

        fun findNextFreeSlot(candidateHour: Int, candidateMinute: Int, duration: Int): Pair<Int, Int> {
            var currH = candidateHour
            var currM = candidateMinute
            var attempts = 0
            while (attempts < 48) {
                var overlaps = false
                val start = currH * 60 + currM
                val end = start + duration
                
                for (p in planned) {
                    val ps = p.startHour * 60 + p.startMinute
                    val pe = ps + p.durationMinutes
                    if (!(end <= ps || start >= pe)) {
                        overlaps = true
                        break
                    }
                }

                if (!overlaps) {
                    return Pair(currH, currM)
                }

                // Move by 30 minutes
                currM += 30
                if (currM >= 60) {
                    currH += currM / 60
                    currM = currM % 60
                }
                if (currH >= 24) {
                    currH = 6 // reset to early morning 6 am next day slot if it gets too late
                }
                attempts++
            }
            return Pair(candidateHour, candidateMinute) // fallback
        }

        // Schedule High Priorities (Targeting morning 6:00 AM)
        for (t in highPriority) {
            val slot = findNextFreeSlot(6, 0, t.durationMinutes)
            planned.add(t.copy(dateString = dateString, startHour = slot.first, startMinute = slot.second, score = calculateScore(t)))
        }

        // Schedule Medium Priorities (Targeting evening 5:30 PM (17:30))
        for (t in medPriority) {
            val slot = findNextFreeSlot(17, 30, t.durationMinutes)
            planned.add(t.copy(dateString = dateString, startHour = slot.first, startMinute = slot.second, score = calculateScore(t)))
        }

        // Schedule Low/Flexible (Targeting lunch slots or afternoon 12:00 PM)
        for (t in lowPriority) {
            val slot = findNextFreeSlot(12, 0, t.durationMinutes)
            planned.add(t.copy(dateString = dateString, startHour = slot.first, startMinute = slot.second, score = calculateScore(t)))
        }

        return planned
    }
}
