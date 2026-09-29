package com.example.vitamin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calorie_logs")
data class CalorieLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val foodName: String,
    val calories: Int,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null
)
