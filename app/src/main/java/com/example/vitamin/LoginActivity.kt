package com.example.vitamin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = VitaminDbHelper(this)
        sessionManager = SessionManager(this)

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvGoToRegister = findViewById<TextView>(R.id.tv_go_to_register)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = dbHelper.authenticateUser(username, password)
            if (user != null) {
                sessionManager.saveLoginSession(user.id)
                Toast.makeText(this, "Login Berhasil! Selamat datang, ${user.nickname}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Username atau password salah", Toast.LENGTH_SHORT).show()
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
