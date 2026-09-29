package com.example.vitamin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val sessionManager = SessionManager(context)
        if (!sessionManager.isNotificationEnabled()) {
            return Result.success()
        }

        val dbHelper = VitaminDbHelper(context)
        val userId = sessionManager.getUserId()
        val bioList = dbHelper.getBiometricsForUser(userId)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayCal = dbHelper.getDailyCalorieTotal(userId, todayStr)

        val notifTitle: String
        val notifMessage: String
        val targetFragment: String

        if (bioList.isNotEmpty()) {
            val latestBio = bioList[0]
            val cat = latestBio.category.lowercase(Locale.getDefault())
            val formattedBmi = String.format(Locale.getDefault(), "%.1f", latestBio.bmiValue)

            if (cat.contains("kurus") || cat.contains("underweight")) {
                notifTitle = "Saran Asupan (BMI: Kurus)"
                notifMessage = "BMI Anda $formattedBmi (${latestBio.category}). Total kalori hari ini: $todayCal kcal. Yuk tingkatkan asupan nutrisi di menu Kalori!"
                targetFragment = "kalori"
            } else if (cat.contains("gemuk") || cat.contains("obese") || cat.contains("overweight")) {
                notifTitle = "Pengingat Kalori Sehat"
                notifMessage = "BMI Anda $formattedBmi (${latestBio.category}). Total kalori hari ini: $todayCal kcal. Jaga asupan makanan seimbang!"
                targetFragment = "kalori"
            } else {
                notifTitle = "Evaluasi Kesehatan Harian"
                notifMessage = "BMI Anda Normal ($formattedBmi). Asupan kalori hari ini: $todayCal kcal. Pertahankan pola hidup sehat!"
                targetFragment = "bmi"
            }
        } else if (todayCal > 0) {
            notifTitle = "Progres Kalori Harian"
            notifMessage = "Total asupan kalori Kamu hari ini adalah $todayCal kcal. Tetap jaga pola makan sehat!"
            targetFragment = "kalori"
        } else {
            notifTitle = "Pengingat Kesehatan Harian"
            notifMessage = "Belum ada log makanan hari ini. Catat asupan kalori dan pantau BMI Kamu bersama ViTaMiN!"
            targetFragment = "kalori"
        }

        val channelId = "vitamin_health_channel"
        val channelName = "Pengingat Kesehatan Vitamin"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Channel pengingat & rekomendasi kesehatan Vitamin"
            }
            notificationManager.createNotificationChannel(channel)
        }

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
            .setContentTitle(notifTitle)
            .setContentText(notifMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notifId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notifId, builder.build())

        return Result.success()
    }
}
