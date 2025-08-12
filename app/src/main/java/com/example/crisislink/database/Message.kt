package com.example.crisislink.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val content: String,
    val senderName: String,
    val senderAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergency: Boolean = false,
    val isReceived: Boolean = false // true if received, false if sent
)
