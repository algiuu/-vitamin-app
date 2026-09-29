package com.example.vitamin

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vitamin_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = true
    fun getUserId(): Int = 1
    fun saveLoginSession(userId: Int) {}
    fun logout() {}

    fun saveProvider(provider: String) {
        prefs.edit().putString("ai_provider", provider).apply()
    }

    fun getProvider(): String {
        return prefs.getString("ai_provider", "Google Gemini") ?: "Google Gemini"
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
    }

    fun getApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }

    fun saveModel(model: String) {
        prefs.edit().putString("ai_model", model.trim()).apply()
    }

    fun getModel(): String {
        val model = prefs.getString("ai_model", "")
        if (!model.isNullOrEmpty()) return model
        return if (getProvider() == "OpenRouter") "google/gemini-2.5-flash" else "gemini-2.5-flash"
    }

    fun saveNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notification_enabled", enabled).apply()
    }

    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean("notification_enabled", true)
    }

    fun saveCalorieTarget(target: Int) {
        prefs.edit().putInt("calorie_target", target).apply()
    }

    fun getCalorieTarget(): Int {
        return prefs.getInt("calorie_target", 2000)
    }
}
