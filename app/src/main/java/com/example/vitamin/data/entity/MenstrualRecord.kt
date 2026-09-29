package com.example.vitamin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menstrual_records")
data class MenstrualRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long,
    val endDate: Long? = null,
    val flowIntensity: String? = null, // Light, Medium, Heavy
    val notes: String? = null
)
