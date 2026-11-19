package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String, // "user" or "assistant"
    val content: String,
    val languageCode: String = "en", // "bn", "hi", "en"
    val source: String = "gemini", // "gemini", "offline", "device", "voice"
    val isVoiceInput: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "personal_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String, // "Preference", "Fact", "Contact", "Routine"
    val content: String,
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val triggerAtMillis: Long,
    val type: String = "REMINDER", // "REMINDER", "ALARM", "TIMER"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
