package com.example.vitamin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var dbHelper: VitaminDbHelper

    private val channelId = "vitamin_health_channel"
    private val channelName = "Pengingat Kesehatan Vitamin"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Izin notifikasi diberikan", Toast.LENGTH_SHORT).show()
            sendLocalPushNotification(
                "Notifikasi Vitamin Aktif",
                "Kamu akan menerima pengingat harian & saran kesehatan.",
                "home"
            )
            scheduleDailyReminder(requireContext())
        } else {
            Toast.makeText(requireContext(), "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        dbHelper = VitaminDbHelper(requireContext())
        createNotificationChannel()

        val switchNotification = view.findViewById<SwitchMaterial>(R.id.switch_notification)
        val tvStatusDesc = view.findViewById<TextView>(R.id.tv_notification_status_desc)
        val layoutNotificationList = view.findViewById<LinearLayout>(R.id.layout_notification_list)
        val tvNotifCount = view.findViewById<TextView>(R.id.tv_notif_count)

        val userId = sessionManager.getUserId()

        // Load or seed initial history
        loadNotificationHistory(userId, layoutNotificationList, tvNotifCount)

        // Load saved toggle state
        val isEnabled = sessionManager.isNotificationEnabled()
        switchNotification.isChecked = isEnabled
        updateNotificationUiState(isEnabled, tvStatusDesc, layoutNotificationList)

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.saveNotificationEnabled(isChecked)
            updateNotificationUiState(isChecked, tvStatusDesc, layoutNotificationList)

            if (isChecked) {
                scheduleDailyReminder(requireContext())
                checkPermissionAndSendNotif(
                    "Notifikasi Vitamin Aktif",
                    "Pengingat harian & saran kesehatan berhasil diaktifkan!",
                    "home"
                )
            } else {
                cancelDailyReminder(requireContext())
                Toast.makeText(requireContext(), "Notifikasi dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNotificationHistory(
        userId: Int,
        container: LinearLayout,
        tvCount: TextView?
    ) {
        var list = dbHelper.getNotificationsForUser(userId)

        // If history is empty, seed initial sample history with real timestamps
        if (list.isEmpty()) {
            dbHelper.insertNotification(
                userId,
                "Target Kalori Harian 🥗",
                "Konsumsi protein dan pertahankan asupan gizi seimbang hari ini.",
                "Kalori",
                "kalori"
            )
            dbHelper.insertNotification(
                userId,
                "Saran Diagnose AI 💡",
                "Indikasi migrain ringan. Istirahat cukup dan konsultasikan dengan dokter jika berlanjut.",
                "Diagnose",
                "diagnose"
            )
            list = dbHelper.getNotificationsForUser(userId)
        }

        tvCount?.text = "${list.size} TERBARU"
        container.removeAllViews()

        for (item in list) {
            val card = createNotificationCardView(requireContext(), item)
            container.addView(card)
        }
    }

    private fun createNotificationCardView(ctx: Context, item: NotificationItem): MaterialCardView {
        val card = MaterialCardView(ctx).apply {
            cardElevation = dpToPx(ctx, 2f)
            radius = dpToPx(ctx, 12f)
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = 0
            val cardParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(ctx, 12f).toInt())
            }
            layoutParams = cardParams
        }

        val rowLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Color indicator bar on the left
        val indicatorColor = when (item.category.lowercase(Locale.getDefault())) {
            "kalori" -> ContextCompat.getColor(ctx, R.color.accent_yellow)
            "diagnose" -> ContextCompat.getColor(ctx, R.color.accent_blue)
            "bmi" -> ContextCompat.getColor(ctx, R.color.accent_blue)
            "menstruasi" -> ContextCompat.getColor(ctx, R.color.accent_pink)
            else -> ContextCompat.getColor(ctx, R.color.accent_yellow)
        }

        val indicatorView = View(ctx).apply {
            setBackgroundColor(indicatorColor)
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(ctx, 6f).toInt(),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val contentLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            padding(ctx, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTitle = TextView(ctx).apply {
            text = item.title
            setTextColor(ContextCompat.getColor(ctx, R.color.black))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tvCategoryBadge = TextView(ctx).apply {
            text = item.category.uppercase(Locale.getDefault())
            setTextColor(ContextCompat.getColor(ctx, R.color.black))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(indicatorColor)
            setPadding(dpToPx(ctx, 6f).toInt(), dpToPx(ctx, 2f).toInt(), dpToPx(ctx, 6f).toInt(), dpToPx(ctx, 2f).toInt())
            val badgeParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(ctx, 8f).toInt(), 0, 0, 0)
            }
            layoutParams = badgeParams
        }

        val spacer = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1.0f)
        }

        val formattedDate = formatSyncTime(item.createdAt)
        val tvTime = TextView(ctx).apply {
            text = formattedDate
            setTextColor(ContextCompat.getColor(ctx, R.color.text_gray))
            textSize = 11f
        }

        headerRow.addView(tvTitle)
        headerRow.addView(tvCategoryBadge)
        headerRow.addView(spacer)
        headerRow.addView(tvTime)

        val tvMessage = TextView(ctx).apply {
            text = item.message
            setTextColor(ContextCompat.getColor(ctx, R.color.text_gray))
            textSize = 13f
            val msgParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(ctx, 6f).toInt(), 0, 0)
            }
            layoutParams = msgParams
        }

        contentLayout.addView(headerRow)
        contentLayout.addView(tvMessage)

        rowLayout.addView(indicatorView)
        rowLayout.addView(contentLayout)

        card.addView(rowLayout)

        // Card Click Listener - Send Notification and Navigate
        card.setOnClickListener {
            if (sessionManager.isNotificationEnabled()) {
                checkPermissionAndSendNotif(item.title, item.message, item.targetFragment)
            } else {
                Toast.makeText(ctx, "Aktifkan notifikasi untuk melihat di status bar", Toast.LENGTH_SHORT).show()
            }
        }

        return card
    }

    private fun formatSyncTime(createdAt: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = sdf.parse(createdAt) ?: return createdAt
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dateDayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

            if (todayStr == dateDayStr) {
                "Hari ini $timeStr"
            } else {
                "$dateDayStr $timeStr"
            }
        } catch (e: Exception) {
            createdAt
        }
    }

    private fun dpToPx(ctx: Context, dp: Float): Float {
        return dp * ctx.resources.displayMetrics.density
    }

    private fun View.padding(ctx: Context, dp: Int) {
        val px = dpToPx(ctx, dp.toFloat()).toInt()
        setPadding(px, px, px, px)
    }

    private fun checkPermissionAndSendNotif(title: String, message: String, targetFragment: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                sendLocalPushNotification(title, message, targetFragment)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            sendLocalPushNotification(title, message, targetFragment)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel pengingat & rekomendasi kesehatan Vitamin"
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendLocalPushNotification(title: String, message: String, targetFragment: String) {
        val context = context ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_FRAGMENT", targetFragment)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            targetFragment.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notifId, builder.build())

        Toast.makeText(context, "Notifikasi terkirim ke Status Bar!", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleDailyReminder(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "vitamin_daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("vitamin_daily_reminder")
    }

    private fun updateNotificationUiState(
        isEnabled: Boolean,
        tvStatusDesc: TextView,
        layoutNotificationList: LinearLayout
    ) {
        if (isEnabled) {
            tvStatusDesc.text = getString(R.string.notification_settings_desc)
            layoutNotificationList.alpha = 1.0f
        } else {
            tvStatusDesc.text = getString(R.string.notification_disabled_desc)
            layoutNotificationList.alpha = 0.5f
        }
    }
}
