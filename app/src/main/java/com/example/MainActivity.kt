package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.Task
import com.example.domain.smartengine.SmartEngine
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.workers.ReminderWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule backup checks and background intelligence scans with WorkManager
        try {
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(2, TimeUnit.HOURS).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "SMART_BACKUP_CHECK",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeState by viewModel.theme.collectAsState()

            MyApplicationTheme(themeName = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigationContainer(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainNavigationContainer(viewModel: MainViewModel) {
    var activeTab by rememberSaveable { mutableStateOf(0) }
    val errorLogs by viewModel.errorLogs.collectAsState()
    val context = LocalContext.current

    val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val activeTimerMode by viewModel.activeTimerMode.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("app_navigation_bar")
                    .navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Task, contentDescription = "Task Board") },
                    label = { Text("Task Board") },
                    modifier = Modifier.testTag("nav_tab_tasks")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    modifier = Modifier.testTag("nav_tab_calendar")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Focus") },
                    label = { Text("Focus") },
                    modifier = Modifier.testTag("nav_tab_focus")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error overlay for Crash Prevention logs
            if (errorLogs.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "System Status",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorLogs.first(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tabs Router
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> HomeDashboardScreen(viewModel)
                    1 -> TaskBoardScreen(viewModel)
                    2 -> CalendarIntelligenceScreen(viewModel)
                    3 -> FocusTimerScreen(viewModel)
                    4 -> SettingsAndAboutScreen(viewModel)
                }
            }

            // Bottom Focus Banner matching the design mockup when timer is running!
            if (isTimerRunning) {
                val min = timerSecondsLeft / 60
                val sec = timerSecondsLeft % 60
                val formattedTime = String.format("%02d:%02d", min, sec)
                
                Surface(
                    color = Color(0xFF1C1B1F),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                var currentAngle by remember { mutableStateOf(0f) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        currentAngle = (currentAngle + 10f) % 360f
                                        delay(150)
                                    }
                                }
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color(0xFFEA580C).copy(alpha = 0.2f),
                                        radius = size.minDimension / 2,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    drawArc(
                                        color = Color(0xFFEA580C),
                                        startAngle = currentAngle,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = formattedTime,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "FOCUS MODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFB923C),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (activeTimerMode == "FOCUS") "Pomodoro Timer Active" else "Relax Rest Phase Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                viewModel.setIsTimerRunning(false)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop Timer",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 1. HOME DASHBOARD SCREEN
// ----------------------------------------------------
@Composable
fun HomeDashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val xpState by viewModel.xp.collectAsState()
    val levelState by viewModel.level.collectAsState()
    val streakState by viewModel.streak.collectAsState()
    val focusScoreState by viewModel.focusScore.collectAsState()
    val penaltyState by viewModel.missedPenalties.collectAsState()

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Calendar.getInstance().time)

    val todayTasks = tasks.filter { it.dateString == today }
    val completedCount = todayTasks.count { it.isCompleted }
    val totalCount = todayTasks.size
    val completionPercent = if (totalCount > 0) (completedCount * 100 / totalCount) else 0

    // Voice assistant / smart entry state
    var voiceInputText by remember { mutableStateOf("") }
    val activeSelection = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Top App Bar matching the Professional Polish HTML
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Day Planner X Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Level $levelState • Master Planner",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Pill badge representing Streak and XP
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$streakState🔥",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFFCAC4D0))
                        )
                        Text(
                            text = "$xpState XP",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Productivity Metrics Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Daily Completion",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$completionPercent%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Completed $completedCount of $totalCount tasks today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                    
                    // Circular Progress Canvas
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        val progressColor = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = trackColor,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = (completionPercent.toFloat() / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Focus",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$focusScoreState%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Productivity Scores & KPIs Matrix
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus Score
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Focus Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$focusScoreState%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (focusScoreState >= 75) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Target: 100%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Completion status & Penalties
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Missed Penalties",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (penaltyState == 0) "Perfect! 0" else "-$penaltyState",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (penaltyState == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Resets Task Streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Offline Natural Language / Voice input parser panel
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Offline Voice Parsing",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Intelligent Voice Input",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Type or dictate. Rules: 'Study at 10 for 45 mins' will automatically resolve and book.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = voiceInputText,
                        onValueChange = { voiceInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_input_box"),
                        placeholder = { Text("e.g. Gym workout at 18:00 for 90") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (voiceInputText.isNotBlank()) {
                                    viewModel.processVoiceInput(voiceInputText, today)
                                    voiceInputText = ""
                                    Toast.makeText(context, "Voice input processed!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Process Text")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (voiceInputText.isNotBlank()) {
                                viewModel.processVoiceInput(voiceInputText, today)
                                voiceInputText = ""
                                Toast.makeText(context, "Voice input processed!", Toast.LENGTH_SHORT).show()
                            }
                        }),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick voice voice presets
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap Quick Dictation Simulation Preset:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val samples = listOf(
                            "Study Math at 6 for 60 mins",
                            "Ecosystem Meeting with Developer at 14",
                            "Emergency Gym session at 17 for 90"
                        )
                        items(samples) { s ->
                            SuggestionChip(
                                onClick = {
                                    voiceInputText = s
                                },
                                label = { Text(s) }
                            )
                        }
                    }
                }
            }
        }

        // Auto schedule & Intelligence Action Trigger
        item {
            val nextTask = todayTasks.firstOrNull { !it.isCompleted }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Engine Active banner
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Text(
                            text = "SMART ENGINE ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Next Optimized Slot:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = nextTask?.let { "${it.title} • ${String.format("%02d:%02d AM", it.startHour, it.startMinute)}" } ?: "All tasks scheduled & complete!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.triggerSmartDailyAutoPlanner(today)
                                Toast.makeText(context, "Schedule Generated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("auto_scheduler_btn")
                        ) {
                            Text(
                                text = "RE-PLAN",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Today's Smart Agenda list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's OS Agenda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount/$totalCount Completed ($completionPercent%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (todayTasks.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventNote,
                            contentDescription = "Empty",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks yet today!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add tasks in Task Board or tap Smart Auto-Planner.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        items(todayTasks) { task ->
            TaskItemCard(task = task, onChecked = {
                viewModel.toggleTaskCompleted(task)
            }, onDelete = {
                viewModel.deleteTask(task)
            })
        }
    }
}

// ----------------------------------------------------
// 2. TASK BOARD SCREEN
// ----------------------------------------------------
@Composable
fun TaskBoardScreen(viewModel: MainViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Add dialog fields
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(60) }
    var recurrence by remember { mutableStateOf("NONE") }
    var category by remember { mutableStateOf("Study") }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(sdf.format(Calendar.getInstance().time)) }

    var selectedFilterCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Study", "Gym", "Work", "Life", "Voice Input")

    val filteredTasks = if (selectedFilterCategory == "All") {
        tasks
    } else {
        tasks.filter { it.category == selectedFilterCategory }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(16.dp)
        ) {
            Text(
                text = "Task Organizer Board",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Categories horizontal filter row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedFilterCategory == cat,
                        onClick = { selectedFilterCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Task,
                            contentDescription = "Empty Board",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks found here",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskItemCard(task = task, onChecked = {
                            viewModel.toggleTaskCompleted(task)
                        }, onDelete = {
                            viewModel.deleteTask(task)
                        })
                    }
                }
            }
        }
    }

    // Modal Sheet or Dialog for Adding Tasks
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Compile Smart Task") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Title *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_field")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = selectedDate,
                            onValueChange = { selectedDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startHour.toString(),
                                onValueChange = { startHour = it.toIntOrNull() ?: 9 },
                                label = { Text("Hour (0-23)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = startMinute.toString(),
                                onValueChange = { startMinute = it.toIntOrNull() ?: 0 },
                                label = { Text("Minute (0-59)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = duration.toString(),
                            onValueChange = { duration = it.toIntOrNull() ?: 60 },
                            label = { Text("Duration (Minutes)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Category selector:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Study", "Gym", "Work", "Life").forEach { cat ->
                                InputChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                    item {
                        Text("Smart Recurrence:")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("NONE", "DAILY").forEach { rec ->
                                InputChip(
                                    selected = recurrence == rec,
                                    onClick = { recurrence = rec },
                                    label = { Text(rec) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(
                                title = title,
                                description = desc,
                                dateString = selectedDate,
                                startHour = startHour,
                                startMinute = startMinute,
                                duration = duration,
                                recurrence = recurrence,
                                category = category
                            )
                            title = ""
                            desc = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------
// CARD DESIGN PATTERNS
// ----------------------------------------------------
@Composable
fun TaskItemCard(task: Task, onChecked: () -> Unit, onDelete: () -> Unit) {
    val levelColor = when (task.priority) {
        3 -> Color(0xFFD32F2F) // High red
        2 -> Color(0xFFF57C00) // Med orange
        1 -> Color(0xFF1976D2) // Low blue
        else -> Color(0xFF757575) // Flex gray
    }

    val itemXp = if (task.priority == 3) 20 else 10

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                    else MaterialTheme.colorScheme.surface
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task priority left sidebar line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(levelColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onChecked() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Core Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (task.isRescheduled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "Recovered",
                                fontSize = 8.sp,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Time Indicator and Category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format("%02d:%02d (%d min)", task.startHour, task.startMinute, task.durationMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // XP and Actions
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "+$itemXp XP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.70f)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. CALENDAR INTELLIGENCE SCREEN
// ----------------------------------------------------
@Composable
fun CalendarIntelligenceScreen(viewModel: MainViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calInstance = Calendar.getInstance()

    var selectedDateStr by remember { mutableStateOf(sdf.format(calInstance.time)) }
    val daysOfWeek = remember {
        val list = mutableListOf<Date>()
        val resetCal = Calendar.getInstance()
        // Start from beginning of active week
        resetCal.set(Calendar.DAY_OF_WEEK, resetCal.firstDayOfWeek)
        for (i in 0..6) {
            list.add(resetCal.time)
            resetCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedDayTasks = tasks.filter { it.dateString == selectedDateStr }

    // Check for overlapping conflicts in selected day's schedule
    var hasConflict = false
    for (i in selectedDayTasks.indices) {
        for (j in i + 1 until selectedDayTasks.size) {
            if (SmartEngine.hasConflict(selectedDayTasks[i], selectedDayTasks[j])) {
                hasConflict = true
                break
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Calendar Intelligence OS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Resolves overlaps and auto schedules daily items smoothly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Week Calendar Strip view
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEachIndexed { index, d ->
                val dateStr = sdf.format(d)
                val isSelected = dateStr == selectedDateStr
                val dayCal = Calendar.getInstance().apply { time = d }
                val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                val dayText = dayNames[dayCal.get(Calendar.DAY_OF_WEEK) - 1]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { selectedDateStr = dateStr }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = dayText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Conflicts Banner Alert and Resolver
        if (hasConflict) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Conflict",
                            tint = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Time overlapping match found!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text(
                        text = "Multiple tasks reside at identical slots. Tap to resolve automatically and shift to non-conflicting hours.",
                        fontSize = 12.sp,
                        color = Color(0xFFE65100).copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.resolveDayConflicts(selectedDateStr)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Auto Reschedule Overlaps")
                    }
                }
            }
        } else if (selectedDayTasks.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Clean",
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule perfectly optimized. No overlaps!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Task agenda items on date
        Text(
            text = "Details: $selectedDateStr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No schedules for this date.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayTasks) { t ->
                    TaskItemCard(task = t, onChecked = {
                        viewModel.toggleTaskCompleted(t)
                    }, onDelete = {
                        viewModel.deleteTask(t)
                    })
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. FOCUS TIMER SCREEN (POMODORO)
// ----------------------------------------------------
@Composable
fun FocusTimerScreen(viewModel: MainViewModel) {
    val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val activeTimerMode by viewModel.activeTimerMode.collectAsState()
    var focusMultiplierDnd by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Immersive distraction lock state
    var dndLockActive by remember { mutableStateOf(false) }

    val formattedTime = String.format("%02d:%02d", timerSecondsLeft / 60, timerSecondsLeft % 60)

    // Countdown logic tied to ViewModel global state
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (viewModel.timerSecondsLeft.value > 0 && viewModel.isTimerRunning.value) {
                delay(1000L)
                viewModel.decrementTimer()
            }
            if (viewModel.timerSecondsLeft.value <= 0 && viewModel.isTimerRunning.value) {
                // Finished!
                viewModel.setIsTimerRunning(false)
                if (activeTimerMode == "FOCUS") {
                    val focusMinutesEarned = 25
                    val bonusFactor = if (focusMultiplierDnd) 2 else 1
                    viewModel.completeFocusSession(focusMinutesEarned * bonusFactor)

                    Toast.makeText(context, "Great job! +${focusMinutesEarned * bonusFactor} XP rewarded!", Toast.LENGTH_LONG).show()
                    // Shift to break
                    viewModel.setActiveTimerMode("BREAK")
                    viewModel.setTimerSecondsLeft(5 * 60)
                } else {
                    Toast.makeText(context, "Break completed! Start focusing again.", Toast.LENGTH_SHORT).show()
                    viewModel.setActiveTimerMode("FOCUS")
                    viewModel.setTimerSecondsLeft(25 * 60)
                }
                dndLockActive = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Intelligence Focus Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (activeTimerMode == "FOCUS") "Deep Work Pomodoro Phase" else "Auto Relax Rest Phase",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Beautiful Circular Canvas Timer View
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxS = if (activeTimerMode == "FOCUS") 25 * 60 else 5 * 60
                    val sweepAngle = (timerSecondsLeft.toFloat() / maxS) * 360f

                    // Background ring
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 8.dp.toPx())
                    )

                    // Foreground countdown ring
                    drawArc(
                        color = if (activeTimerMode == "FOCUS") Color(0xFF6200EE) else Color(0xFF03DAC5),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isTimerRunning) "RUNNING" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Multiplier DND Selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .clickable { focusMultiplierDnd = !focusMultiplierDnd }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Checkbox(
                    checked = focusMultiplierDnd,
                    onCheckedChange = { focusMultiplierDnd = it },
                    modifier = Modifier.testTag("dnd_checkbox")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Immersive Lock Task Mode",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Locks app clicks. Rewards Double (2x) XP",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Triggers
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.setIsTimerRunning(false)
                        viewModel.setTimerSecondsLeft(if (activeTimerMode == "FOCUS") 25 * 60 else 5 * 60)
                    }
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = {
                        val nextRunning = !isTimerRunning
                        viewModel.setIsTimerRunning(nextRunning)
                        if (nextRunning && focusMultiplierDnd) {
                            dndLockActive = true
                        }
                    },
                    modifier = Modifier.testTag("focus_start_btn")
                ) {
                    Text(if (isTimerRunning) "Pause Focus" else "Start Focus")
                }
            }
        }

        // 6. Immersive Do Not Disturb Screen Lock Overlay (Section 6)
        if (dndLockActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.98f))
                    .clickable(enabled = false) {}, // blocks all clickable underlays
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.DoNotDisturbOn,
                        contentDescription = "Locked",
                        tint = Color.Red,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Do Not Disturb App Lock Active",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minimize distractions. Focusing on your current calendar schedule now. Multitasking triggers streaks penalty.",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    TextButton(
                        onClick = {
                            dndLockActive = false
                            viewModel.setIsTimerRunning(false)
                        }
                    ) {
                        Text(
                            text = "Quit Mode (Lose XP & Reset Streak)",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. SETTINGS, BACKUP & ABOUT SCREEN (NexVora Ecosystem)
// ----------------------------------------------------
@Composable
fun SettingsAndAboutScreen(viewModel: MainViewModel) {
    val currentTheme by viewModel.theme.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // JSON Dialog Backup state
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "OS Control Center & Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Theme management (Light Theme, Dark Theme, AMOLED Theme)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Display Style Theme",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val themes = listOf("LIGHT", "DARK", "AMOLED", "SYSTEM")
                        themes.forEach { t ->
                            val isSelected = currentTheme == t
                            Button(
                                onClick = { viewModel.updateTheme(t) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = t, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Export/Import JSON systems (Section 27: JSON safety backup)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Offline JSON Backup Core",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Safety export and restore. Essential for offline privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val exported = viewModel.exportTasksAsJson()
                                clipboardManager.setText(AnnotatedString(exported))
                                Toast.makeText(context, "Backup copied to Clipboard!", Toast.LENGTH_LONG).show()
                                showBackupDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Import")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Backup", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Interactive "ABOUT DESIGNER SECTION" (Section: AR Abdur Rahman developer)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏢 NexVora Lab's Ofc",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Independent Android System Developer:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Prince AR Abdur Rahman",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mission: Build modern, lightning-fast, offline security-first productivity platforms without unneeded tracking APIs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "📦 NexVora Ecosystem Core Apps",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val apps = listOf(
                        "NexPlay X", "LifeSphere OS", "Smart Day Planner X Pro",
                        "Study AI", "Offline AI", "CalcVerse", "Lensora Studio", "NexVoice OS"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        apps.forEach { appName ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• $appName",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Ecosystem Sync Ready",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Factory reset stats
        item {
            OutlinedButton(
                onClick = {
                    viewModel.resetStats()
                    Toast.makeText(context, "Gamification Stats Form Reset Successful!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Full reset active stats")
            }
        }
    }

    // Export safety show dialog
    if (showBackupDialog) {
        val exported = viewModel.exportTasksAsJson()
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Compiled Successfully") },
            text = {
                OutlinedTextField(
                    value = exported,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(onClick = { showBackupDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Import Safety verify dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Paste JSON Backup") },
            text = {
                OutlinedTextField(
                    value = restoreJsonText,
                    onValueChange = { restoreJsonText = it },
                    placeholder = { Text("Paste exported backup here...") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            val success = viewModel.importTasksFromJson(restoreJsonText)
                            restoreJsonText = ""
                            showRestoreDialog = false
                            if (success) {
                                Toast.makeText(context, "Data backup restored successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid data structure format!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Verify and Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Discard")
                }
            }
        )
    }
}
