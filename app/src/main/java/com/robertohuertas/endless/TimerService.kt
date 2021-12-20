package com.robertohuertas.endless

import android.app.*
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import java.util.*


class TimerService : Service()
{
    override fun onBind(p0: Intent?): IBinder? = null

    private val timer = Timer()
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    private val channelId = "i.apps.notifications"
    private val description = "Test notification"


    //Keys
    private var TAG = "TimerService"


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {
        val time = intent.getDoubleExtra(TIME_EXTRA, 0.0)
        val wantedValue = intent.getDoubleExtra(INTERVAL, 17.0).toDouble()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d("MainActivity", "Started Time")
        timer.scheduleAtFixedRate(TimeTask(time, wantedValue), 0, 1000)


        return START_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("TimerService", "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy()
    {
        Log.d("TimerService_", "StopTimer")
        super.onDestroy()
        timer.cancel()
        notificationManager.cancel(12345)
    }


    private inner class TimeTask(private var time: Double, private var wantedInterval : Double) : TimerTask() {
        override fun run() {
            val intent = Intent(TIMER_UPDATED)
            time++
            intent.putExtra(TIME_EXTRA, time)
            sendBroadcast(intent)
            Log.d(TAG, "time: "  + time)
        }
    }


    companion object
    {
        const val TIMER_UPDATED = "timerUpdated"
        const val TIME_EXTRA = "timeExtra"
        const val INTERVAL = "timeInterval"
    }

}