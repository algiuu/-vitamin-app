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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MenstruasiFragment : Fragment() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menstruasi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = VitaminDbHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        val userId = sessionManager.getUserId()
        val currentUser = dbHelper.getUser(userId)
        if (currentUser != null && currentUser.gender == "Laki-laki") {
            Toast.makeText(requireContext(), "Akses Ditolak: Fitur Menstruasi khusus untuk pengguna Perempuan.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        val tvPredictionResult = view.findViewById<TextView>(R.id.tv_prediction_result)
        val etStartDate = view.findViewById<EditText>(R.id.et_start_date)
        val etDuration = view.findViewById<EditText>(R.id.et_duration)
        val etCycleLength = view.findViewById<EditText>(R.id.et_cycle_length)
        val etNotes = view.findViewById<EditText>(R.id.et_menstrual_notes)
        val btnSave = view.findViewById<Button>(R.id.btn_save_menstrual)
        val llHistoryList = view.findViewById<LinearLayout>(R.id.ll_menstrual_history_list)

        // AI components
        val tvAiResponse = view.findViewById<TextView>(R.id.tv_ai_menstrual_response)
        val etAiQuery = view.findViewById<EditText>(R.id.et_ai_menstrual_query)
        val btnAskAi = view.findViewById<Button>(R.id.btn_ask_menstrual_ai)

        fun loadRecordsAndPredictions() {
            llHistoryList.removeAllViews()
            val records = dbHelper.getMenstrualRecordsForUser(userId)
            
            if (records.isEmpty()) {
                tvPredictionResult.text = "Prediksi Siklus Berikutnya: Belum ada data"
                val tvEmpty = TextView(requireContext()).apply {
                    text = "Belum ada riwayat siklus."
                    setTextColor(resources.getColor(R.color.text_gray, null))
                    setPadding(16, 16, 16, 16)
                }
                llHistoryList.addView(tvEmpty)
            } else {
                val latest = records[0]
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val date = sdf.parse(latest.startDate)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date
                        cal.add(Calendar.DAY_OF_YEAR, latest.cycleLength)
                        val nextDateStr = sdf.format(cal.time)
                        tvPredictionResult.text = "Prediksi Siklus Berikutnya:\n$nextDateStr"
                    }
                } catch (e: Exception) {
                    tvPredictionResult.text = "Prediksi Siklus Berikutnya: Error kalkulasi"
                }

                for (rec in records) {
                    val tvItem = TextView(requireContext()).apply {
                        text = "Mulai: ${rec.startDate}\nDurasi: ${rec.duration} hari, Siklus: ${rec.cycleLength} hari\nCatatan: ${rec.notes}"
                        textSize = 15f
                        setTextColor(resources.getColor(R.color.black, null))
                        setPadding(12, 12, 12, 12)
                        
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
        loadRecordsAndPredictions()

        btnSave.setOnClickListener {
            val startDateStr = etStartDate.text.toString()
            val durationStr = etDuration.text.toString()
            val cycleStr = etCycleLength.text.toString()
            val notes = etNotes.text.toString()

            if (startDateStr.isEmpty() || durationStr.isEmpty() || cycleStr.isEmpty()) {
                Toast.makeText(requireContext(), "Harap lengkapi tanggal, durasi, dan panjang siklus", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            try {
                sdf.parse(startDateStr)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Format tanggal harus yyyy-MM-dd (Contoh: 2026-03-01)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = durationStr.toIntOrNull()
            val cycleLength = cycleStr.toIntOrNull()

            if (duration == null || cycleLength == null || duration <= 0 || cycleLength <= 0) {
                Toast.makeText(requireContext(), "Durasi atau Panjang Siklus tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.insertMenstrualRecord(userId, startDateStr, duration, cycleLength, notes)
            Toast.makeText(requireContext(), "Log menstruasi berhasil disimpan!", Toast.LENGTH_SHORT).show()

            etStartDate.text.clear()
            etDuration.text.clear()
            etCycleLength.text.clear()
            etNotes.text.clear()

            loadRecordsAndPredictions()
        }

        // AI Menstrual Chat Consultation
        btnAskAi.setOnClickListener {
            val apiKey = sessionManager.getApiKey()
            if (apiKey.isEmpty()) {
                Toast.makeText(requireContext(), "Harap atur API Key Anda terlebih dahulu di menu Setting / Profil!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val query = etAiQuery.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Harap ketik pertanyaan Anda terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvAiResponse.text = "Sedang berpikir..."
            etAiQuery.text.clear()

            // Fetch user cycle records to inject as context into prompt
            val records = dbHelper.getMenstrualRecordsForUser(userId)
            val historyContext = StringBuilder()
            if (records.isNotEmpty()) {
                historyContext.append("Data Riwayat Haid User saat ini: ")
                for (r in records.take(3)) {
                    historyContext.append("[Mulai: ${r.startDate}, Durasi: ${r.duration} hari, Siklus: ${r.cycleLength} hari. Catatan Gejala: ${r.notes}]. ")
                }
            } else {
                historyContext.append("User belum mencatat riwayat haid.")
            }

            val dynamicPrompt = "Anda adalah Asisten Kesehatan Reproduksi Perempuan Cerdas pada aplikasi VITaMIN. Jawab pertanyaan user dengan ramah, berempati, edukatif, dan berbasis medis yang akurat. Berdasarkan $historyContext jawablah pertanyaan berikut: $query"

            lifecycleScope.launch {
                val responseText = AiHelper.callTextApi(sessionManager, dynamicPrompt)
                tvAiResponse.text = responseText ?: "Maaf, gagal memuat jawaban. Periksa koneksi internet, API Key, atau Model AI Anda."
            }
        }
    }
}
