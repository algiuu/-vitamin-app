package com.example.vitamin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DiagnoseFragment : Fragment() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diagnose, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = VitaminDbHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        val userId = sessionManager.getUserId()

        val etSymptomInput = view.findViewById<EditText>(R.id.et_symptom_description)
        val btnDiagnose = view.findViewById<Button>(R.id.btn_diagnose_ai)
        val tvDiagnosisResult = view.findViewById<TextView>(R.id.tv_diagnosis_text)

        // Quick symptom chip buttons
        val chipPusing = view.findViewById<Button>(R.id.chip_pusing)
        val chipDemam = view.findViewById<Button>(R.id.chip_demam)
        val chipMual = view.findViewById<Button>(R.id.chip_mual)
        val chipBatuk = view.findViewById<Button>(R.id.chip_batuk)
        val chipNyeriPerut = view.findViewById<Button>(R.id.chip_nyeri_perut)
        val chipLemas = view.findViewById<Button>(R.id.chip_lemas)

        fun appendSymptomTag(symptomTag: String) {
            val currentText = etSymptomInput.text.toString().trim()
            if (!currentText.contains(symptomTag, ignoreCase = true)) {
                if (currentText.isEmpty()) {
                    etSymptomInput.setText(symptomTag)
                } else {
                    etSymptomInput.setText("$currentText, $symptomTag")
                }
                etSymptomInput.setSelection(etSymptomInput.text.length)
            }
        }

        chipPusing.setOnClickListener { appendSymptomTag("Pusing") }
        chipDemam.setOnClickListener { appendSymptomTag("Demam") }
        chipMual.setOnClickListener { appendSymptomTag("Mual") }
        chipBatuk.setOnClickListener { appendSymptomTag("Batuk/Flu") }
        chipNyeriPerut.setOnClickListener { appendSymptomTag("Nyeri Perut") }
        chipLemas.setOnClickListener { appendSymptomTag("Lemas") }

        btnDiagnose.setOnClickListener {
            val apiKey = sessionManager.getApiKey()
            if (apiKey.isEmpty()) {
                Toast.makeText(requireContext(), "Harap atur API Key Anda terlebih dahulu di menu Setting / Profil!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val symptomsInput = etSymptomInput.text.toString().trim()
            if (symptomsInput.isEmpty()) {
                Toast.makeText(requireContext(), "Harap masukkan atau pilih gejala yang Anda rasakan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvDiagnosisResult.text = "Sedang menganalisis gejala Anda via AI..."

            // Build health context from local database
            val user = dbHelper.getUser(userId)
            val latestBio = dbHelper.getBiometricsForUser(userId).firstOrNull()
            val latestMen = dbHelper.getMenstrualRecordsForUser(userId).firstOrNull()

            val bioContext = if (latestBio != null) {
                "BMI ${latestBio.bmiValue} (${latestBio.category}), BB ${latestBio.weight}kg, TB ${latestBio.height}cm"
            } else {
                "Belum ada data BMI"
            }

            val menContext = if (latestMen != null) {
                "Haid terakhir: ${latestMen.startDate}, Siklus: ${latestMen.cycleLength} hari, Catatan: ${latestMen.notes}"
            } else {
                "Belum ada data haid"
            }

            val contextPrompt = "Data Profil User: [Nama: ${user?.nickname ?: "User"}, Status Biometrik: $bioContext, Data Haid: $menContext]."

            val prompt = """
                Anda adalah Asisten Medis Cerdas pada aplikasi kesehatan VITaMIN. User melaporkan gejala kesehatan berikut: "$symptomsInput".
                
                Berdasarkan $contextPrompt, berikan respon analisis medis dan skrining awal yang ramah, empatik, terstruktur, dan akurat dengan format:
                1. 🔍 **Kemungkinan Kondisi/Penyebab**: (Sebutkan 1-3 kemungkinan kondisi kesehatan)
                2. 🚦 **Tingkat Kegentingan (Triage)**: [Pilih: 🟢 Ringan (Perawatan Mandiri) / 🟡 Sedang (Konsultasi Dokter) / 🔴 Perlu Perhatian Segera (Ke IGD)] + sertakan alasannya.
                3. 🩹 **Saran Pertolongan Pertama & Care Plan**: (Saran praktis perawatannya di rumah, misal hidrasi, istirahat, kompres, dll.)
                4. 🚨 **Tanda Bahaya (Red Flags)**: Kapan user harus segera pergi ke fasilitas medis.
            """.trimIndent()

            lifecycleScope.launch {
                val responseText = AiHelper.callTextApi(sessionManager, prompt)
                tvDiagnosisResult.text = responseText ?: "Maaf, gagal memuat hasil diagnosis. Periksa koneksi internet, API Key, atau Model AI Anda."
            }
        }
    }
}
