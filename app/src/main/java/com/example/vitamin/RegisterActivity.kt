package com.example.vitamin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: VitaminDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = VitaminDbHelper(this)

        val etUsername = findViewById<EditText>(R.id.et_reg_username)
        val etNickname = findViewById<EditText>(R.id.et_reg_nickname)
        val etPassword = findViewById<EditText>(R.id.et_reg_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvGoToLogin = findViewById<TextView>(R.id.tv_go_to_login)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString()
            val nickname = etNickname.text.toString()
            val password = etPassword.text.toString()

            if (username.isEmpty() || nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = dbHelper.registerUser(username, nickname, password)
            if (result != -1L) {
                Toast.makeText(this, "Registrasi Berhasil! Silakan Login", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Registrasi Gagal. Username mungkin sudah ada", Toast.LENGTH_SHORT).show()
            }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}
