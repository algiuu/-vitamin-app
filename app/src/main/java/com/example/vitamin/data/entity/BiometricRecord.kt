package com.example.vitamin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biometric_records")
data class BiometricRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val weight: Double,
    val bmi: Double,
    val heartRate: Int? = null,
    val bloodPressure: String? = null
)
