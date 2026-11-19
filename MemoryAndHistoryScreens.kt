package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.SensitiveActionRequest
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.preferences.NovaPreferences
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaTopBar
import com.example.ui.components.SensitiveActionConfirmCard
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCoralError
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceGlass
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaOnAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonalMemoryScreen(
    memories: List<MemoryEntity>,
    notes: List<NoteEntity>,
    reminders: List<ReminderEntity>,
    preferences: NovaPreferences,
    pendingSensitiveAction: SensitiveActionRequest?,
    onBack: () -> Unit,
    onAddMemory: (category: String, content: String) -> Unit,
    onToggleMemoryApproval: (MemoryEntity) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onSaveNote: (title: String, body: String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onCreateReminder: (title: String, delayMinutes: Int) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onSetQuickAlarm: (hour: Int, minute: Int, label: String) -> Unit,
    onSetQuickTimer: (minutes: Int, label: String) -> Unit,
    onEvaluateMath: (String) -> String,
    onSaveEmergencyContact: (name: String, phone: String) -> Unit,
    onCallEmergencyContact: () -> Unit,
    onConfirmSensitive: () -> Unit,
    onCancelSensitive: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("AI Memory", "Notes", "Alarms & Tools")

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Personal Memory",
                subtitle = "Room local storage • Offline ready",
                onBack = onBack
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NovaSurfaceGlass,
                contentColor = NovaCyan
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingSensitiveAction != null) {
                    SensitiveActionConfirmCard(
                        action = pendingSensitiveAction,
                        onConfirm = onConfirmSensitive,
                        onCancel = onCancelSensitive
                    )
                }

                when (selectedTab) {
                    0 -> MemoryTabContent(
                        memories = memories,
                        onAddMemory = onAddMemory,
                        onToggleApproval = onToggleMemoryApproval,
                        onDeleteMemory = onDeleteMemory
                    )
                    1 -> NotesTabContent(
                        notes = notes,
                        onSaveNote = onSaveNote,
                        onDeleteNote = onDeleteNote
                    )
                    else -> ToolsAndRemindersTabContent(
                        reminders = reminders,
                        preferences = preferences,
                        onCreateReminder = onCreateReminder,
                        onDeleteReminder = onDeleteReminder,
                        onSetQuickAlarm = onSetQuickAlarm,
                        onSetQuickTimer = onSetQuickTimer,
                        onEvaluateMath = onEvaluateMath,
                        onSaveEmergencyContact = onSaveEmergencyContact,
                        onCallEmergencyContact = onCallEmergencyContact
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryTabContent(
    memories: List<MemoryEntity>,
    onAddMemory: (String, String) -> Unit,
    onToggleApproval: (MemoryEntity) -> Unit,
    onDeleteMemory: (Long) -> Unit
) {
    var category by remember { mutableStateOf("Preference") }
    var content by remember { mutableStateOf("") }

    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Psychology, contentDescription = null, tint = NovaCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add User-Approved Memory", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Preference", "Fact", "Routine", "Language").forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("e.g., My name is Rafi, I prefer Bengali replies") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NovaCyan,
                unfocusedBorderColor = NovaBorderGlow,
                focusedTextColor = NovaTextPrimary,
                unfocusedTextColor = NovaTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("memory_input")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (content.isNotBlank()) {
                    onAddMemory(category, content)
                    content = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_memory_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = NovaCyan, contentColor = NovaOnAccent)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save to NOVA Memory", fontWeight = FontWeight.Bold)
        }
    }

    if (memories.isEmpty()) {
        NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "No personal memories saved yet. Approved memories help Gemini personalize answers while staying stored locally on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaTextSecondary
            )
        }
    } else {
        memories.forEach { mem ->
            NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mem.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = NovaCyan)
                        Text(mem.content, style = MaterialTheme.typography.bodyLarge, color = NovaTextPrimary)
                    }
                    Switch(
                        checked = mem.isApproved,
                        onCheckedChange = { onToggleApproval(mem) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NovaEmerald)
                    )
                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete memory", tint = NovaCoralError)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesTabContent(
    notes: List<NoteEntity>,
    onSaveNote: (String, String) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NoteAdd, contentDescription = null, tint = NovaPurple)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Offline Saved Notes", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Note Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Note Content") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (title.isNotBlank() || body.isNotBlank()) {
                    onSaveNote(title, body)
                    title = ""
                    body = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_note_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = NovaPurple)
        ) {
            Text("Save Offline Note", fontWeight = FontWeight.Bold)
        }
    }

    notes.forEach { note ->
        NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, color = NovaCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(note.body, style = MaterialTheme.typography.bodyMedium, color = NovaTextPrimary)
                }
                IconButton(onClick = { onDeleteNote(note.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = NovaCoralError)
                }
            }
        }
    }
}

@Composable
private fun ToolsAndRemindersTabContent(
    reminders: List<ReminderEntity>,
    preferences: NovaPreferences,
    onCreateReminder: (String, Int) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onSetQuickAlarm: (Int, Int, String) -> Unit,
    onSetQuickTimer: (Int, String) -> Unit,
    onEvaluateMath: (String) -> String,
    onSaveEmergencyContact: (String, String) -> Unit,
    onCallEmergencyContact: () -> Unit
) {
    var reminderTitle by remember { mutableStateOf("") }
    var reminderMins by remember { mutableStateOf("10") }
    var calcExpr by remember { mutableStateOf("25 * 16 + 40") }
    var calcAnswer by remember { mutableStateOf("") }
    var emergName by remember(preferences.emergencyContactName) { mutableStateOf(preferences.emergencyContactName) }
    var emergPhone by remember(preferences.emergencyContactPhone) { mutableStateOf(preferences.emergencyContactPhone) }

    // Alarms & Timers
    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Alarm, contentDescription = null, tint = NovaCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Alarm & Timer Manager", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onSetQuickTimer(5, "NOVA 5m Timer") },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("5m Timer", color = NovaTextPrimary)
            }
            OutlinedButton(
                onClick = { onSetQuickAlarm(7, 0, "NOVA Morning Alarm") },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = NovaAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("7:00 AM", color = NovaTextPrimary)
            }
        }
    }

    // Reminder System
    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = NovaEmerald)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Schedule Reminder", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reminderTitle,
                onValueChange = { reminderTitle = it },
                label = { Text("Reminder") },
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = reminderMins,
                onValueChange = { reminderMins = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mins") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val mins = reminderMins.toIntOrNull() ?: 10
                if (reminderTitle.isNotBlank()) {
                    onCreateReminder(reminderTitle, mins)
                    reminderTitle = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NovaEmerald, contentColor = NovaOnAccent)
        ) {
            Text("Set Reminder Notification", fontWeight = FontWeight.Bold)
        }
        reminders.forEach { rem ->
            val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(rem.triggerAtMillis))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${rem.title} • $fmt", color = NovaTextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDeleteReminder(rem.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete reminder", tint = NovaCoralError)
                }
            }
        }
    }

    // Offline Calculator
    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Calculate, contentDescription = null, tint = NovaPurple)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Offline Calculator", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = calcExpr,
            onValueChange = { calcExpr = it },
            label = { Text("Expression (e.g., (45 + 15) * 3)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { calcAnswer = onEvaluateMath(calcExpr) },
                colors = ButtonDefaults.buttonColors(containerColor = NovaElectricBlue),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Calculate")
            }
            if (calcAnswer.isNotBlank()) {
                Text("= $calcAnswer", style = MaterialTheme.typography.titleLarge, color = NovaCyan)
            }
        }
    }

    // Emergency Contact Shortcut
    NovaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NovaCoralError.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Emergency, contentDescription = null, tint = NovaCoralError)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Emergency Contact Shortcut", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = emergName,
            onValueChange = { emergName = it },
            label = { Text("Contact Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = emergPhone,
            onValueChange = { emergPhone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onSaveEmergencyContact(emergName, emergPhone) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Save", color = NovaCyan)
            }
            Button(
                onClick = onCallEmergencyContact,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NovaCoralError)
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Emergency Call")
            }
        }
    }
}

@Composable
fun ConversationHistoryScreen(
    messages: List<ChatMessageEntity>,
    onBack: () -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") }

    val filtered = remember(messages, searchQuery, filterMode) {
        messages.reversed().filter { m ->
            val matchesQuery = searchQuery.isBlank() || m.content.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                "VOICE" -> m.isVoiceInput || m.source == "wake"
                "GEMINI" -> m.source.startsWith("gemini")
                "OFFLINE" -> m.source == "offline" || m.source == "device"
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Conversation History",
                subtitle = "${messages.size} stored messages",
                onBack = onBack,
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearAll, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear All History", tint = NovaCoralError)
                        }
                    }
                }
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search conversations...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "VOICE", "GEMINI", "OFFLINE").forEach { mode ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { filterMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_chat),
                        contentDescription = "No history",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, NovaBorderGlow, RoundedCornerShape(22.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No matching history yet", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { msg ->
                        val time = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                        NovaGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (msg.role == "user") NovaElectricBlue.copy(alpha = 0.5f) else NovaPurple.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${msg.role.uppercase()} • ${msg.languageCode.uppercase()} • ${msg.source} • $time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NovaCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(msg.content, style = MaterialTheme.typography.bodyMedium, color = NovaTextPrimary)
                                }
                                IconButton(onClick = { onDeleteMessage(msg.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete message", tint = NovaTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
