package com.example.vitamin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
