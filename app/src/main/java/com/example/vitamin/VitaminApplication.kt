package com.example.vitamin

import android.app.Application

class VitaminApplication : Application() {
    val dbHelper: VitaminDbHelper by lazy { VitaminDbHelper(this) }
    val sessionManager: SessionManager by lazy { SessionManager(this) }
}
