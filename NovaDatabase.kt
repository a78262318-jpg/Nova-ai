package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        MemoryEntity::class,
        NoteEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_ai_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class NovaLocalRepository(private val db: NovaDatabase) {
    val messages = db.chatDao().observeAllMessages()
    val memories = db.memoryDao().observeAllMemories()
    val notes = db.noteDao().observeAllNotes()
    val reminders = db.reminderDao().observeAllReminders()

    suspend fun addMessage(message: ChatMessageEntity): Long = db.chatDao().insertMessage(message)
    suspend fun getRecentMessages(limit: Int = 16): List<ChatMessageEntity> =
        db.chatDao().getRecentMessages(limit)

    suspend fun deleteMessage(id: Long) = db.chatDao().deleteMessageById(id)
    suspend fun clearHistory() = db.chatDao().clearAllMessages()

    suspend fun getApprovedMemories(): List<MemoryEntity> = db.memoryDao().getApprovedMemories()
    suspend fun addMemory(category: String, content: String, approved: Boolean = true): Long =
        db.memoryDao().insertMemory(MemoryEntity(category = category, content = content, isApproved = approved))

    suspend fun toggleMemoryApproval(memory: MemoryEntity) =
        db.memoryDao().updateMemory(memory.copy(isApproved = !memory.isApproved))

    suspend fun deleteMemory(id: Long) = db.memoryDao().deleteMemoryById(id)

    suspend fun getNotesSnapshot(): List<NoteEntity> = db.noteDao().getAllNotesSnapshot()
    suspend fun saveNote(title: String, body: String): Long =
        db.noteDao().insertNote(NoteEntity(title = title, body = body))

    suspend fun deleteNote(id: Long) = db.noteDao().deleteNoteById(id)
    suspend fun clearNotes() = db.noteDao().clearAllNotes()

    suspend fun saveReminder(title: String, triggerAtMillis: Long, type: String = "REMINDER"): Long =
        db.reminderDao().insertReminder(
            ReminderEntity(title = title, triggerAtMillis = triggerAtMillis, type = type)
        )

    suspend fun setReminderActive(id: Long, active: Boolean) =
        db.reminderDao().setReminderActive(id, active)

    suspend fun deleteReminder(id: Long) = db.reminderDao().deleteReminderById(id)

    suspend fun wipeAllPersonalData() {
        db.chatDao().clearAllMessages()
        db.memoryDao().clearAllMemories()
        db.noteDao().clearAllNotes()
        db.reminderDao().clearAllReminders()
    }
}
