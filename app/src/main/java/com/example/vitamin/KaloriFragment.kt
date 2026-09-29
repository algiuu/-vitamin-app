package com.example.vitamin

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KaloriFragment : Fragment() {

    private lateinit var dbHelper: VitaminDbHelper
    private lateinit var sessionManager: SessionManager

    private lateinit var layoutManualSection: LinearLayout
    private lateinit var layoutCameraSection: LinearLayout
    private lateinit var viewCameraPreview: PreviewView
    private lateinit var btnModeManual: Button
    private lateinit var btnModeCamera: Button
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Izin kamera ditolak. Tidak bisa cek via kamera.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kalori, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = VitaminDbHelper(requireContext())
        sessionManager = SessionManager(requireContext())

        val userId = sessionManager.getUserId()

        val tvTotalCaloriesToday = view.findViewById<TextView>(R.id.tv_total_calories_today)
        val tvCaloriePercent = view.findViewById<TextView>(R.id.tv_calorie_percent)
        val pbCalorieLimit = view.findViewById<ProgressBar>(R.id.pb_calorie_limit)
        val btnEditTarget = view.findViewById<Button>(R.id.btn_edit_calorie_target)

        val etFoodName = view.findViewById<EditText>(R.id.et_food_name)
        val etCaloriesAmount = view.findViewById<EditText>(R.id.et_calories_amount)
        val btnSaveCalorie = view.findViewById<Button>(R.id.btn_save_calorie)
        val llCaloriesHistoryList = view.findViewById<LinearLayout>(R.id.ll_calories_history_list)

        // Sections & Toggle views
        layoutManualSection = view.findViewById(R.id.layout_manual_section)
        layoutCameraSection = view.findViewById(R.id.layout_camera_section)
        viewCameraPreview = view.findViewById(R.id.view_camera_preview)
        btnModeManual = view.findViewById(R.id.btn_mode_manual)
        btnModeCamera = view.findViewById(R.id.btn_mode_camera)
        val btnCaptureFood = view.findViewById<Button>(R.id.btn_capture_food)

        fun loadLogsAndSummary() {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dailyTotal = dbHelper.getDailyCalorieTotal(userId, currentDate)
            val targetCalorie = sessionManager.getCalorieTarget()

            tvTotalCaloriesToday.text = "$dailyTotal / $targetCalorie kcal"
            val percent = if (targetCalorie > 0) ((dailyTotal * 100) / targetCalorie).coerceAtMost(100) else 0
            tvCaloriePercent.text = "Progres: $percent% dari limit harian ($targetCalorie kcal)"
            pbCalorieLimit.progress = percent

            llCaloriesHistoryList.removeAllViews()
            val logs = dbHelper.getCalorieLogsForUser(userId)
            if (logs.isEmpty()) {
                val tvEmpty = TextView(requireContext()).apply {
                    text = "Belum ada log makanan."
                    setTextColor(resources.getColor(R.color.text_gray, null))
                    setPadding(16, 16, 16, 16)
                }
                llCaloriesHistoryList.addView(tvEmpty)
            } else {
                for (log in logs) {
                    val tvItem = TextView(requireContext()).apply {
                        text = "Tanggal: ${log.date}\n${log.foodName} - ${log.calories} kcal"
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
                    llCaloriesHistoryList.addView(tvItem)
                }
            }
        }

        btnEditTarget?.setOnClickListener {
            val currentTarget = sessionManager.getCalorieTarget()
            val inputEt = EditText(requireContext()).apply {
                hint = "Masukkan limit kalori (contoh: 2000)"
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(currentTarget.toString())
            }

            AlertDialog.Builder(requireContext()).apply {
                setTitle("Ubah Limit Kalori Harian")
                setMessage("Atur batas maksimal asupan kalori harianmu (kcal):")
                setView(inputEt)
                setPositiveButton("Simpan") { _, _ ->
                    val newTarget = inputEt.text.toString().toIntOrNull()
                    if (newTarget != null && newTarget > 0) {
                        sessionManager.saveCalorieTarget(newTarget)
                        Toast.makeText(requireContext(), "Limit kalori diubah menjadi $newTarget kcal!", Toast.LENGTH_SHORT).show()
                        loadLogsAndSummary()
                    } else {
                        Toast.makeText(requireContext(), "Nilai limit tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
                setNegativeButton("Batal", null)
                show()
            }
        }

        // Toggle Buttons Listeners
        btnModeManual.setOnClickListener {
            layoutManualSection.visibility = View.VISIBLE
            layoutCameraSection.visibility = View.GONE
        }

        btnModeCamera.setOnClickListener {
            layoutManualSection.visibility = View.GONE
            layoutCameraSection.visibility = View.VISIBLE
            checkCameraPermission()
        }

        // Camera capture food real-time intelligent detection via AI REST client
        btnCaptureFood.setOnClickListener {
            val apiKey = sessionManager.getApiKey()
            if (apiKey.isEmpty()) {
                Toast.makeText(requireContext(), "Harap atur API Key Anda terlebih dahulu di menu Setting / Profil!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val bitmap = viewCameraPreview.bitmap
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Gagal menangkap frame kamera. Coba lagi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Sedang mendeteksi makanan lewat AI...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                val resultJson = AiHelper.callVisionApi(sessionManager, bitmap)
                if (resultJson != null) {
                    val foodName = resultJson.optString("food", "Makanan Tidak Dikenal")
                    val calories = resultJson.optInt("calories", 0)

                    AlertDialog.Builder(requireContext()).apply {
                        setTitle("Hasil Deteksi AI")
                        setMessage("Terdeteksi: $foodName\nEstimasi Kalori: $calories kcal\n\nApakah Anda ingin menyimpan log ini?")
                        setPositiveButton("Simpan") { _, _ ->
                            dbHelper.insertCalorieLog(userId, foodName, calories)
                            Toast.makeText(requireContext(), "Log makanan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            sendCalorieAddedNotification(foodName, calories)
                            loadLogsAndSummary()
                        }
                        setNegativeButton("Batal", null)
                        show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal terhubung atau mengenali makanan. Cek koneksi, API Key, atau Model AI Anda.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Initial load
        loadLogsAndSummary()

        btnSaveCalorie.setOnClickListener {
            val foodName = etFoodName.text.toString()
            val calStr = etCaloriesAmount.text.toString()

            if (foodName.isEmpty() || calStr.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi nama makanan dan jumlah kalori", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calories = calStr.toIntOrNull()
            if (calories == null || calories <= 0) {
                Toast.makeText(requireContext(), "Jumlah kalori tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.insertCalorieLog(userId, foodName, calories)
            Toast.makeText(requireContext(), "Log kalori berhasil disimpan!", Toast.LENGTH_SHORT).show()
            sendCalorieAddedNotification(foodName, calories)

            etFoodName.text.clear()
            etCaloriesAmount.text.clear()

            loadLogsAndSummary()
        }
    }

    private fun sendCalorieAddedNotification(foodName: String, calories: Int) {
        val ctx = context ?: return
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val userId = sessionManager.getUserId()
        val dailyTotal = dbHelper.getDailyCalorieTotal(userId, currentDate)
        val targetCalorie = sessionManager.getCalorieTarget()

        val notifTitle: String
        val notifMessage: String

        if (dailyTotal > targetCalorie) {
            notifTitle = "Limit Kalori Terlampaui! 🚨"
            notifMessage = "Perhatian! Asupan kalori hari ini ($dailyTotal kcal) telah melebihi limit harian ($targetCalorie kcal)."
        } else if (dailyTotal == targetCalorie) {
            notifTitle = "Target Kalori Harian Tercapai! 🎉"
            notifMessage = "Selamat! Asupan kalori hari ini ($dailyTotal kcal) tepat mencapai limit harian ($targetCalorie kcal)."
        } else if (dailyTotal >= (targetCalorie * 0.8)) {
            notifTitle = "Hampir Capai Limit Kalori ⚠️"
            notifMessage = "Asupan kalori hari ini ($dailyTotal / $targetCalorie kcal) sudah mencapai 80%+ dari limit harian."
        } else {
            notifTitle = "Log Kalori Ditambahkan 🥗"
            notifMessage = "Berhasil mencatat $foodName ($calories kcal). Total: $dailyTotal / $targetCalorie kcal."
        }

        // Save notification record to SQLite database table for Notification Fragment history
        dbHelper.insertNotification(userId, notifTitle, notifMessage, "Kalori", "kalori")

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
            putExtra("TARGET_FRAGMENT", "kalori")
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            "kalori".hashCode(),
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

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewCameraPreview.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
}
