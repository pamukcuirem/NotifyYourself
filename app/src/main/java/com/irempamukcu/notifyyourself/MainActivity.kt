package com.irempamukcu.notifyyourself

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.irempamukcu.notifyyourself.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        binding.buttonImage.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (isScheduleExactAlarmPermissionGranted()) {
                            scheduleNotification()
                        } else {
                            requestScheduleExactAlarmPermission()
                        }
                    } else {
                        scheduleNotification()
                    }
                    binding.buttonImage.setImageResource(R.drawable.duck2)
                    binding.buttonText.setTextColor(Color.WHITE)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.buttonImage.setImageResource(R.drawable.duck)
                    binding.buttonText.setTextColor(Color.DKGRAY)
                    true
                }
                else -> false
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isScheduleExactAlarmPermissionGranted(): Boolean {
        return (getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.canScheduleExactAlarms() == true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestScheduleExactAlarmPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        startActivity(intent)
    }

    private fun showPermissionRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("İzin Gerekli")
            .setMessage("Bu uygulamayı kullanabilmek için lütfen bildirim izinlerini açın.")
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelID = "notif_channel_id"
            val name = "Notification Channel"
            val description = "A Description of the Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelID, name, importance).apply {
                this.description = description
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNotification() {
        val title = binding.title.text.toString()
        val message = binding.message.text.toString()

        if (title.isEmpty() || message.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Hata!")
                .setMessage("Lütfen gerekli alanları doldurunuz.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }

        val intent = Intent(applicationContext, Notify::class.java).apply {
            putExtra("titleExtra", title)
            putExtra("messageExtra", message)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notificationID,
            intent,
            pendingIntentFlags
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = getTime()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )

        showAlert(time, title, message)
    }

    private fun showAlert(time: Long, title: String, message: String) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = time
        }
        val dateFormat = android.text.format.DateFormat.getLongDateFormat(applicationContext)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("Notification Scheduled")
            .setMessage(
                "Başlık: $title\n" +
                        "Mesaj: $message\n" +
                        "Vakit: ${dateFormat.format(calendar.time)} ${timeFormat.format(calendar.time)}"
            )
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun getTime(): Long {
        val calendar = Calendar.getInstance().apply {
            set(
                binding.datePick.year,
                binding.datePick.month,
                binding.datePick.dayOfMonth,
                binding.timePick.hour,
                binding.timePick.minute
            )
        }
        return calendar.timeInMillis
    }

    companion object {
        const val channelID = "notif_channel_id"
        const val notificationID = 1
    }
}
