package com.chefmate.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.chefmate.R
import kotlinx.coroutines.*

class TimerService : Service() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private var remainingSeconds: Long = 0
    private var timerLabel: String? = null
    private var isPaused: Boolean = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var alarmMediaPlayer: MediaPlayer? = null
    private var alarmJob: Job? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "timer_channel"
        private const val ACTION_START = "com.chefmate.TimerService.START"
        private const val ACTION_STOP = "com.chefmate.TimerService.STOP"
        private const val ACTION_PAUSE = "com.chefmate.TimerService.PAUSE"
        private const val ACTION_RESUME = "com.chefmate.TimerService.RESUME"
        private const val ACTION_UPDATE = "com.chefmate.TimerService.UPDATE"
        private const val EXTRA_SECONDS = "extra_seconds"
        private const val EXTRA_LABEL = "extra_label"
        private const val EXTRA_REMAINING = "extra_remaining"
        
        private var instance: TimerService? = null
        
        fun startTimer(context: Context, seconds: Long, label: String? = null) {
            try {
                val intent = Intent(context, TimerService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_SECONDS, seconds)
                    putExtra(EXTRA_LABEL, label)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Error starting timer service", e)
                throw e
            }
        }
        
        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
        
        fun pauseTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }
        
        fun resumeTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }
        
        fun updateTimer(context: Context, remainingSeconds: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_REMAINING, remainingSeconds)
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        instance = this
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getLongExtra(EXTRA_SECONDS, 0)
                val label = intent.getStringExtra(EXTRA_LABEL)
                startTimer(seconds, label)
            }
            ACTION_STOP -> {
                stopTimer()
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_UPDATE -> {
                val remaining = intent.getLongExtra(EXTRA_REMAINING, 0)
                remainingSeconds = remaining
                updateNotification()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startTimer(seconds: Long, label: String?) {
        remainingSeconds = seconds
        timerLabel = label
        
        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ChefMate::TimerWakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 hours max
        }
        
        // Start foreground service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires special use type
                startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-13: can use without type or with media playback
                try {
                    startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } catch (e: SecurityException) {
                    android.util.Log.w("TimerService", "Cannot use media playback type, using without type", e)
                    @Suppress("DEPRECATION")
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error starting foreground service", e)
            e.printStackTrace()
            stopSelf()
            return
        }
        
        // Start countdown
        timerJob?.cancel()
        isPaused = false
        timerJob = serviceScope.launch {
            android.util.Log.d("TimerService", "Starting countdown from $remainingSeconds seconds")
            while (remainingSeconds > 0) {
                delay(1000)
                
                // Check if paused
                while (isPaused) {
                    delay(100)
                }
                
                remainingSeconds--
                android.util.Log.d("TimerService", "Timer: $remainingSeconds seconds remaining")
                updateNotification()
                
                // Broadcast update
                try {
                    val broadcastIntent = Intent("com.chefmate.TIMER_UPDATE").apply {
                        putExtra("remaining_seconds", remainingSeconds)
                    }
                    sendBroadcast(broadcastIntent)
                    android.util.Log.d("TimerService", "Broadcast sent: $remainingSeconds seconds")
                } catch (e: Exception) {
                    android.util.Log.e("TimerService", "Error sending broadcast", e)
                }
            }
            
            // Timer finished
            android.util.Log.d("TimerService", "Timer finished!")
            onTimerFinished()
        }
    }
    
    private fun pauseTimer() {
        isPaused = true
        android.util.Log.d("TimerService", "Timer paused")
        updateNotification()
    }
    
    private fun resumeTimer() {
        isPaused = false
        android.util.Log.d("TimerService", "Timer resumed")
        updateNotification()
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        isPaused = false
        stopContinuousBeep()
        wakeLock?.release()
        wakeLock = null
        stopForeground(true)
        stopSelf()
        instance = null
    }
    
    private fun onTimerFinished() {
        // Start continuous beeping
        startContinuousBeep()
        vibrate()
        
        // Show finished notification with stop action
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer Finished")
            .setContentText(timerLabel ?: "Your timer has finished!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_timer, "Stop", stopPendingIntent)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
        
        // Don't stop timer automatically - let user stop it
    }
    
    private fun startContinuousBeep() {
        alarmJob?.cancel()
        alarmJob = serviceScope.launch {
            while (true) {
                try {
                    // Use ToneGenerator for simple beep
                    val toneGenerator = android.media.ToneGenerator(
                        android.media.AudioManager.STREAM_ALARM,
                        100
                    )
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                    
                    delay(1300) // Wait 1.3 seconds (300ms beep + 1s pause)
                    
                    toneGenerator.release()
                } catch (e: Exception) {
                    android.util.Log.e("TimerService", "Error playing beep", e)
                    // Fallback: use notification sound
                    try {
                        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
                        ringtone?.play()
                        delay(1000)
                        ringtone?.stop()
                        delay(1000) // Pause between beeps
                    } catch (e2: Exception) {
                        android.util.Log.e("TimerService", "Error with ringtone fallback", e2)
                        delay(1000)
                    }
                }
            }
        }
    }
    
    private fun stopContinuousBeep() {
        alarmJob?.cancel()
        alarmJob = null
        alarmMediaPlayer?.stop()
        alarmMediaPlayer?.release()
        alarmMediaPlayer = null
    }
    
    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 200, 500, 200, 500),
                        -1
                    ))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error vibrating", e)
        }
    }
    
    private fun createNotification(): Notification {
        val timeString = formatTime(remainingSeconds)
        val contentText = if (timerLabel != null) {
            "$timerLabel - $timeString"
        } else {
            timeString
        }
        
        val intent = Intent(this, com.chefmate.ui.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cooking Timer")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_timer, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cooking Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows cooking timer countdown"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        alarmJob?.cancel()
        stopContinuousBeep()
        wakeLock?.release()
        instance = null
    }
}
