package com.example.vitamin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class BMIFragment : Fragment() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bmi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = VitaminDbHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        val userId = sessionManager.getUserId()

        val etWeight = view.findViewById<EditText>(R.id.et_weight)
        val etHeight = view.findViewById<EditText>(R.id.et_height)
        val btnCalc = view.findViewById<Button>(R.id.btn_calc_bmi)
        val tvScore = view.findViewById<TextView>(R.id.tv_bmi_score)
        val tvStatus = view.findViewById<TextView>(R.id.tv_bmi_status)
        val llHistoryList = view.findViewById<LinearLayout>(R.id.ll_bmi_history_list)

        fun loadHistory() {
            llHistoryList.removeAllViews()
            val records = dbHelper.getBiometricsForUser(userId)
            if (records.isEmpty()) {
                                val tvEmpty = TextView(requireContext()).apply {
                    text = "Belum ada riwayat BMI."
                    setTextColor(resources.getColor(R.color.text_gray, null))
                    setPadding(16, 16, 16, 16)
                }
                llHistoryList.addView(tvEmpty)
            } else {
                for (rec in records) {
                    val tvItem = TextView(requireContext()).apply {
                        text = "Tanggal: ${rec.date}\nBerat: ${rec.weight} kg, Tinggi: ${rec.height} cm\nBMI: ${String.format("%.2f", rec.bmiValue)} (${rec.category})"
                        textSize = 15f
                        setTextColor(resources.getColor(R.color.black, null))
                        setPadding(12, 12, 12, 12)
                        
                        // Add bottom border effect or divider via layout parameters
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 16)
                        layoutParams = params
                        setBackgroundColor(resources.getColor(R.color.white, null))
                    }
                    llHistoryList.addView(tvItem)
                }
            }
        }

        // Initial load
        loadHistory()

        btnCalc.setOnClickListener {
            val weightStr = etWeight.text.toString()
            val heightStr = etHeight.text.toString()

            if (weightStr.isEmpty() || heightStr.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi tinggi dan berat badan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weight = weightStr.toDoubleOrNull()
            val heightCm = heightStr.toDoubleOrNull()

            if (weight == null || heightCm == null || weight <= 0.0 || heightCm <= 0.0) {
                Toast.makeText(requireContext(), "Nilai tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val heightM = heightCm / 100.0
            val bmi = weight / (heightM * heightM)

            val category = when {
                bmi < 18.5 -> "Kurus (Underweight)"
                bmi < 25.0 -> "Normal"
                bmi < 30.0 -> "Kelebihan Berat Badan (Overweight)"
                else -> "Obesitas"
            }

            tvScore.text = "BMI Score: ${String.format("%.2f", bmi)}"
            tvStatus.text = "Kategori: $category"

            // Save to DB
            dbHelper.insertBiometric(userId, weight, heightCm, bmi, category)
            Toast.makeText(requireContext(), "BMI berhasil disimpan!", Toast.LENGTH_SHORT).show()

            // Refresh list
            loadHistory()
        }
    }
}
