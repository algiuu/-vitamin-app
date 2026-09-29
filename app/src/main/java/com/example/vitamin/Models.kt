package com.example.vitamin

data class User(
    val id: Int = 0,
    val username: String,
    val nickname: String,
    val passwordHash: String,
    val gender: String = "Perempuan"
)

data class BiometricRecord(
    val id: Int = 0,
    val userId: Int,
    val weight: Double,
    val height: Double,
    val bmiValue: Double,
    val category: String,
    val date: String
)

data class CalorieLog(
    val id: Int = 0,
    val userId: Int,
    val foodName: String,
    val calories: Int,
    val date: String
)

data class MenstrualRecord(
    val id: Int = 0,
    val userId: Int,
    val startDate: String,
    val duration: Int,
    val cycleLength: Int,
    val notes: String
)
