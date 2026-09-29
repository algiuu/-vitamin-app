package com.example.vitamin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "default_user",
    val name: String,
    val age: Int,
    val height: Double,
    val weight: Double,
    val gender: String
)
