package com.example.vitamin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = VitaminDbHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        val userId = sessionManager.getUserId()
        val user = dbHelper.getUser(userId)

        val tvGreeting = view.findViewById<TextView>(R.id.tv_home_greeting)
        if (user != null) {
            tvGreeting.text = "Halo, ${user.nickname}!"
        }

        view.findViewById<View>(R.id.card_kalori).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, KaloriFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_diagnose).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DiagnoseFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_menstruasi).setOnClickListener {
            val currentUser = dbHelper.getUser(userId)
            if (currentUser != null && currentUser.gender == "Laki-laki") {
                Toast.makeText(requireContext(), "Akses Ditolak: Fitur Menstruasi khusus untuk pengguna Perempuan.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenstruasiFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_bmi).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BMIFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
