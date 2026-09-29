package com.example.vitamin

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var landingContainer: View
    private lateinit var fragmentContainer: View
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Setup Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        landingContainer = findViewById(R.id.landing_container)
        fragmentContainer = findViewById(R.id.fragment_container)
        bottomNav = findViewById(R.id.main_bottom_navigation)

        // Seed the default user if not exists to avoid empty database crashes
        val dbHelper = VitaminDbHelper(this)
        if (dbHelper.getUser(1) == null) {
            dbHelper.registerUser("offline_user", "Pengguna VITaMIN", "1234")
        }

        // Landing Screen Button
        findViewById<View>(R.id.bottom_home).setOnClickListener {
            enterApp()
        }

        // Bottom Navigation Setup
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_you -> UserFragment()
                R.id.nav_notification -> NotificationFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            showFragment(selectedFragment)
            true
        }
    }

    private fun enterApp() {
        landingContainer.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
        
        // Set default fragment to Home
        bottomNav.selectedItemId = R.id.nav_home
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
