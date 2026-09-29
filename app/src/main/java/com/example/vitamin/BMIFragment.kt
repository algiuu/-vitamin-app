package com.example.vitamin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import java.util.Locale

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
                        text = "Tanggal: ${rec.date}\nBerat: ${rec.weight} kg, Tinggi: ${rec.height} cm\nBMI: ${String.format(Locale.getDefault(), "%.2f", rec.bmiValue)} (${rec.category})"
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

            tvScore.text = "BMI Score: ${String.format(Locale.getDefault(), "%.2f", bmi)}"
            tvStatus.text = "Kategori: $category"

            // Save to DB
            dbHelper.insertBiometric(userId, weight, heightCm, bmi, category)
            Toast.makeText(requireContext(), "BMI berhasil disimpan!", Toast.LENGTH_SHORT).show()

            sendBmiNotification(bmi, category)

            // Refresh list
            loadHistory()
        }
    }

    private fun sendBmiNotification(bmi: Double, category: String) {
        val ctx = context ?: return
        val userId = sessionManager.getUserId()
        val formattedBmi = String.format(Locale.getDefault(), "%.2f", bmi)
        val notifTitle = "Hasil BMI Baru ⚖️"
        val notifMessage = "Skor BMI Anda: $formattedBmi ($category). Cek rekomendasi kesehatan lengkapmu!"

        // Save notification record to SQLite database table for Notification Fragment history
        dbHelper.insertNotification(userId, notifTitle, notifMessage, "BMI", "bmi")

        if (!sessionManager.isNotificationEnabled()) return

        val channelId = "vitamin_health_channel"
        val channelName = "Pengingat Kesehatan Vitamin"
        val notificationManager =
            ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel pengingat & rekomendasi kesehatan Vitamin"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_FRAGMENT", "bmi")
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            "bmi".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(notifMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notifId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notifId, builder.build())
    }
}
