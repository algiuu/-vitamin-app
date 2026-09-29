package com.example.vitamin

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VitaminDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "vitamin.db"
        private const val DATABASE_VERSION = 2

        // Tables
        private const val TABLE_USERS = "users"
        private const val TABLE_BIOMETRICS = "biometrics"
        private const val TABLE_CALORIES = "calories"
        private const val TABLE_MENSTRUAL = "menstrual"

        // Common column
        private const val KEY_ID = "id"

        // Users Columns
        private const val KEY_USER_NAME = "username"
        private const val KEY_USER_NICKNAME = "nickname"
        private const val KEY_USER_PASSWORD = "password"
        private const val KEY_USER_GENDER = "gender"

        // Biometrics Columns
        private const val KEY_BIO_USER_ID = "user_id"
        private const val KEY_BIO_WEIGHT = "weight"
        private const val KEY_BIO_HEIGHT = "height"
        private const val KEY_BIO_BMI = "bmi_value"
        private const val KEY_BIO_CATEGORY = "category"
        private const val KEY_BIO_DATE = "date"

        // Calories Columns
        private const val KEY_CAL_USER_ID = "user_id"
        private const val KEY_CAL_FOOD = "food_name"
        private const val KEY_CAL_CALORIES = "calories"
        private const val KEY_CAL_DATE = "date"

        // Menstrual Columns
        private const val KEY_MEN_USER_ID = "user_id"
        private const val KEY_MEN_START_DATE = "start_date"
        private const val KEY_MEN_DURATION = "duration"
        private const val KEY_MEN_CYCLE = "cycle_length"
        private const val KEY_MEN_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = ("CREATE TABLE $TABLE_USERS("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_USER_NAME TEXT UNIQUE,"
                + "$KEY_USER_NICKNAME TEXT,"
                + "$KEY_USER_PASSWORD TEXT,"
                + "$KEY_USER_GENDER TEXT DEFAULT 'Perempuan')")

        val createBiometricsTable = ("CREATE TABLE $TABLE_BIOMETRICS("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_BIO_USER_ID INTEGER,"
                + "$KEY_BIO_WEIGHT REAL,"
                + "$KEY_BIO_HEIGHT REAL,"
                + "$KEY_BIO_BMI REAL,"
                + "$KEY_BIO_CATEGORY TEXT,"
                + "$KEY_BIO_DATE TEXT)")

        val createCaloriesTable = ("CREATE TABLE $TABLE_CALORIES("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_CAL_USER_ID INTEGER,"
                + "$KEY_CAL_FOOD TEXT,"
                + "$KEY_CAL_CALORIES INTEGER,"
                + "$KEY_CAL_DATE TEXT)")

        val createMenstrualTable = ("CREATE TABLE $TABLE_MENSTRUAL("
                + "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$KEY_MEN_USER_ID INTEGER,"
                + "$KEY_MEN_START_DATE TEXT,"
                + "$KEY_MEN_DURATION INTEGER,"
                + "$KEY_MEN_CYCLE INTEGER,"
                + "$KEY_MEN_NOTES TEXT)")

        db.execSQL(createUsersTable)
        db.execSQL(createBiometricsTable)
        db.execSQL(createCaloriesTable)
        db.execSQL(createMenstrualTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $KEY_USER_GENDER TEXT DEFAULT 'Perempuan'")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- User DAO Methods ---
    fun registerUser(username: String, nickname: String, passwordText: String, gender: String = "Perempuan"): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_NAME, username.trim())
            put(KEY_USER_NICKNAME, nickname.trim())
            put(KEY_USER_PASSWORD, passwordText)
            put(KEY_USER_GENDER, gender)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun authenticateUser(username: String, passwordText: String): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS, null,
            "$KEY_USER_NAME=? AND $KEY_USER_PASSWORD=?",
            arrayOf(username.trim(), passwordText),
            null, null, null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME))
            val nick = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NICKNAME))
            val pwd = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD))
            val genIdx = cursor.getColumnIndex(KEY_USER_GENDER)
            val gen = if (genIdx >= 0) cursor.getString(genIdx) ?: "Perempuan" else "Perempuan"
            user = User(id, name, nick, pwd, gen)
        }
        cursor.close()
        return user
    }

    fun getUser(userId: Int): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS, null, "$KEY_ID=?",
            arrayOf(userId.toString()), null, null, null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NAME))
            val nick = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_NICKNAME))
            val pwd = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_PASSWORD))
            val genIdx = cursor.getColumnIndex(KEY_USER_GENDER)
            val gen = if (genIdx >= 0) cursor.getString(genIdx) ?: "Perempuan" else "Perempuan"
            user = User(id, name, nick, pwd, gen)
        }
        cursor.close()
        return user
    }

    fun updateNickname(userId: Int, newNickname: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_NICKNAME, newNickname.trim())
        }
        return db.update(TABLE_USERS, values, "$KEY_ID=?", arrayOf(userId.toString()))
    }

    fun updateGender(userId: Int, gender: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_GENDER, gender)
        }
        return db.update(TABLE_USERS, values, "$KEY_ID=?", arrayOf(userId.toString()))
    }

    fun deleteUser(userId: Int): Int {
        val db = this.writableDatabase
        // Cascade delete records
        db.delete(TABLE_BIOMETRICS, "$KEY_BIO_USER_ID=?", arrayOf(userId.toString()))
        db.delete(TABLE_CALORIES, "$KEY_CAL_USER_ID=?", arrayOf(userId.toString()))
        db.delete(TABLE_MENSTRUAL, "$KEY_MEN_USER_ID=?", arrayOf(userId.toString()))
        return db.delete(TABLE_USERS, "$KEY_ID=?", arrayOf(userId.toString()))
    }

    // --- Biometrics DAO Methods ---
    fun insertBiometric(userId: Int, weight: Double, height: Double, bmi: Double, category: String): Long {
        val db = this.writableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put(KEY_BIO_USER_ID, userId)
            put(KEY_BIO_WEIGHT, weight)
            put(KEY_BIO_HEIGHT, height)
            put(KEY_BIO_BMI, bmi)
            put(KEY_BIO_CATEGORY, category)
            put(KEY_BIO_DATE, currentDate)
        }
        return db.insert(TABLE_BIOMETRICS, null, values)
    }

    fun getBiometricsForUser(userId: Int): List<BiometricRecord> {
        val list = mutableListOf<BiometricRecord>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_BIOMETRICS, null, "$KEY_BIO_USER_ID=?",
            arrayOf(userId.toString()), null, null, "$KEY_ID DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                val w = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_BIO_WEIGHT))
                val h = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_BIO_HEIGHT))
                val bmi = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_BIO_BMI))
                val cat = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BIO_CATEGORY))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BIO_DATE))
                list.add(BiometricRecord(id, userId, w, h, bmi, cat, date))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- Calorie Logs DAO Methods ---
    fun insertCalorieLog(userId: Int, foodName: String, calories: Int): Long {
        val db = this.writableDatabase
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            put(KEY_CAL_USER_ID, userId)
            put(KEY_CAL_FOOD, foodName.trim())
            put(KEY_CAL_CALORIES, calories)
            put(KEY_CAL_DATE, currentDate)
        }
        return db.insert(TABLE_CALORIES, null, values)
    }

    fun getCalorieLogsForUser(userId: Int): List<CalorieLog> {
        val list = mutableListOf<CalorieLog>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_CALORIES, null, "$KEY_CAL_USER_ID=?",
            arrayOf(userId.toString()), null, null, "$KEY_ID DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                val food = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAL_FOOD))
                val cal = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_CAL_CALORIES))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CAL_DATE))
                list.add(CalorieLog(id, userId, food, cal, date))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getDailyCalorieTotal(userId: Int, dateStr: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($KEY_CAL_CALORIES) FROM $TABLE_CALORIES WHERE $KEY_CAL_USER_ID=? AND $KEY_CAL_DATE=?",
            arrayOf(userId.toString(), dateStr)
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        return total
    }

    // --- Menstrual Records DAO Methods ---
    fun insertMenstrualRecord(userId: Int, startDate: String, duration: Int, cycleLength: Int, notes: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_MEN_USER_ID, userId)
            put(KEY_MEN_START_DATE, startDate)
            put(KEY_MEN_DURATION, duration)
            put(KEY_MEN_CYCLE, cycleLength)
            put(KEY_MEN_NOTES, notes.trim())
        }
        return db.insert(TABLE_MENSTRUAL, null, values)
    }

    fun getMenstrualRecordsForUser(userId: Int): List<MenstrualRecord> {
        val list = mutableListOf<MenstrualRecord>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_MENSTRUAL, null, "$KEY_MEN_USER_ID=?",
            arrayOf(userId.toString()), null, null, "$KEY_MEN_START_DATE DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                val start = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEN_START_DATE))
                val dur = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MEN_DURATION))
                val cyc = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MEN_CYCLE))
                val note = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEN_NOTES))
                list.add(MenstrualRecord(id, userId, start, dur, cyc, note))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
