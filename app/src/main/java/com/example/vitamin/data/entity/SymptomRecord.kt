package com.example.vitamin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_records")
data class SymptomRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val symptomName: String,
    val severity: Int, // 1-10
    val notes: String? = null
)
