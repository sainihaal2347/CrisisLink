package com.example.crisislink.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isEmergency = 1 ORDER BY timestamp DESC")
    fun getEmergencyMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 10")
    fun getRecentMessages(): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
