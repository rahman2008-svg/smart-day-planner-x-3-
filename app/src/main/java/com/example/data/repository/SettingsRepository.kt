package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "user_settings_and_stats")

class SettingsRepository(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("app_theme") // LIGHT, DARK, AMOLED, SYSTEM
    private val KEY_XP = intPreferencesKey("user_xp")
    private val KEY_LEVEL = intPreferencesKey("user_level")
    private val KEY_STREAK = intPreferencesKey("user_streak")
    private val KEY_MAX_STREAK = intPreferencesKey("user_max_streak")
    private val KEY_FOCUS_SCORE = intPreferencesKey("user_focus_score")
    private val KEY_MISSED_PENALTY = intPreferencesKey("user_missed_penalty")
    private val KEY_TOTAL_FOCUS_MINUTES = intPreferencesKey("user_total_focus_minutes")

    val themeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[KEY_THEME] ?: "SYSTEM"
        }

    val xpFlow: Flow<Int> = context.dataStore.data.map { it[KEY_XP] ?: 0 }
    val levelFlow: Flow<Int> = context.dataStore.data.map { it[KEY_LEVEL] ?: 1 }
    val streakFlow: Flow<Int> = context.dataStore.data.map { it[KEY_STREAK] ?: 0 }
    val maxStreakFlow: Flow<Int> = context.dataStore.data.map { it[KEY_MAX_STREAK] ?: 0 }
    val focusScoreFlow: Flow<Int> = context.dataStore.data.map { it[KEY_FOCUS_SCORE] ?: 100 }
    val missedPenaltyFlow: Flow<Int> = context.dataStore.data.map { it[KEY_MISSED_PENALTY] ?: 0 }
    val totalFocusMinutesFlow: Flow<Int> = context.dataStore.data.map { it[KEY_TOTAL_FOCUS_MINUTES] ?: 0 }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun addXp(amount: Int) {
        context.dataStore.edit { preferences ->
            val currentXp = preferences[KEY_XP] ?: 0
            val newXp = currentXp + amount
            preferences[KEY_XP] = newXp

            // Level formula: Level = (XP / 100) + 1
            val newLevel = (newXp / 100) + 1
            preferences[KEY_LEVEL] = newLevel
        }
    }

    suspend fun incrementStreak() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_STREAK] ?: 0
            val updated = current + 1
            preferences[KEY_STREAK] = updated
            val max = preferences[KEY_MAX_STREAK] ?: 0
            if (updated > max) {
                preferences[KEY_MAX_STREAK] = updated
            }
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { preferences ->
            preferences[KEY_STREAK] = 0
        }
    }

    suspend fun addFocusMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            val curMin = preferences[KEY_TOTAL_FOCUS_MINUTES] ?: 0
            preferences[KEY_TOTAL_FOCUS_MINUTES] = curMin + minutes

            // Add focus score
            val currentScore = preferences[KEY_FOCUS_SCORE] ?: 100
            preferences[KEY_FOCUS_SCORE] = (currentScore + minutes / 5).coerceAtMost(100)
        }
    }

    suspend fun applyMissedPenalty() {
        context.dataStore.edit { preferences ->
            val curPen = preferences[KEY_MISSED_PENALTY] ?: 0
            preferences[KEY_MISSED_PENALTY] = curPen + 1

            // Penalty reduces focus score
            val currentScore = preferences[KEY_FOCUS_SCORE] ?: 100
            preferences[KEY_FOCUS_SCORE] = (currentScore - 10).coerceAtLeast(0)
            
            // Missed task resets current task streak!
            preferences[KEY_STREAK] = 0
        }
    }

    suspend fun resetStats() {
        context.dataStore.edit { preferences ->
            preferences[KEY_XP] = 0
            preferences[KEY_LEVEL] = 1
            preferences[KEY_STREAK] = 0
            preferences[KEY_MAX_STREAK] = 0
            preferences[KEY_FOCUS_SCORE] = 100
            preferences[KEY_MISSED_PENALTY] = 0
            preferences[KEY_TOTAL_FOCUS_MINUTES] = 0
        }
    }
}
