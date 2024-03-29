package com.robertohuertas.endless

import android.annotation.SuppressLint
import android.app.*
import android.app.Service.START_NOT_STICKY
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock.elapsedRealtimeNanos
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.Toast
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

class AccelerationService : Service(), SensorEventListener
{
    override fun onBind(p0: Intent?): IBinder? = null
    private var mSensorManager : SensorManager?= null
    private var mAccelerometer : Sensor?= null
    private var resume = false;
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    private val channelId = "i.apps.notificationss"
    private val description = "notification tart"
    private val timerforWaitNotification = Timer()


    //Services and Intents
    private var innerIntent = Intent(ACC_Updated)
    private lateinit var outerTimerIntent: Intent
    private var timerRunning = Intent(TIMER_RUNNING)
    //private lateinit var timerRunning: Intent = Intent(TIMER_RUNNING)

    //Value variables
    private var time = 0.0
    private var outerTime = 0.0
    private var wantedTime = 25.0
    private var gyroArray = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private var accelArray = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private var maxAccel : Double? = 0.0
    private var maxGyro: Double? = 0.0
    private var gyroExc: Boolean = false
    private var accelExc: Boolean = false
    private var timeMap = mutableMapOf<Int, Button>()
    private var numbersMapHash = hashMapOf<Int, Int>()
    private var useHashmape = hashMapOf<Int, Int>()
    private var usedHashmape = hashMapOf<Int, Int>()
    private var nextTime = 0
    private var nextKey = 0
    private var lastKey = false

    //Keys
    private val TAG = "AccelerationService"



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {

        Log.d(TAG, "Acceleration service started" )

        //Outer timer service setup
        outerTimerIntent = Intent(applicationContext, TimerService::class.java)
        registerReceiver(updateTime, IntentFilter(TimerService.TIMER_UPDATED))





        wantedTime = intent.getDoubleExtra(INTERVAL, wantedTime).toDouble()


        val hashMap = intent!!.getSerializableExtra(EndlessService.VALUEMAP) as HashMap<Int, Int>
        numbersMapHash = hashMap
        numbersMapHash.keys.forEach{
            Log.d("NumberMapAcc", ("Key: " + it.toString() + " " + "Value: " + numbersMapHash[it].toString()))
        }

        useHashmape = HashMap(numbersMapHash)
        var temp = nextTimeElement()
        nextTime = temp.first
        nextKey =  temp.second
        //Sensor setup
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Specify the sensor you want to listen to
        mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            mSensorManager!!.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
            mSensorManager!!.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        //Notification setup
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d("RUNRUNRUN", "RUNNNNN")
        return START_STICKY
    }


    override fun onDestroy()
    {
        Log.d("InnerSensorChange", "Stop")
        super.onDestroy()
    }

    fun append(arr: Array<Double>, element: Double): Array<Double> {
        val list: MutableList<Double> = arr.toMutableList()
        list.add(element)
        list.removeAt(0)
        return list.toTypedArray()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            Log.d("Gyros", event.values[1].toString())
            gyroArray = append(gyroArray, abs(event.values[1].toDouble()))
            Log.d("GyroArray", gyroArray.max().toString())
        }
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            accelArray = append(accelArray, abs(event.values[2].toDouble()))
            Log.d("InnerSensorChange", event.values[2].toString())
        }
        maxAccel = accelArray.max()
        maxGyro = gyroArray.max()
        gyroExc = higherThan(gyroArray, 4.0)
        accelExc = higherThan(accelArray, 19.0)
        if ((event?.sensor?.type == Sensor.TYPE_GYROSCOPE).or(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER))
            if (gyroExc && accelExc && !innerIntent.getBooleanExtra(TIMER_RUNNING, false)){
                //if (event.values[2].toDouble() > 25 && !innerIntent.getBooleanExtra(TIMER_RUNNING, false)) {
                //Log.d("InnerSensorChange", "TimerStartSet" + !event.values[2].toString())
                //notificationCall("Timer starts now.")
                timerRunning.putExtra(TIMER_RUNNING, true)
                timerRunning.putExtra(STOPTIME, nextTime)
                sendBroadcast(timerRunning)
                Toast.makeText(applicationContext, "Timer Starts.", Toast.LENGTH_SHORT).show()
                val pattern = longArrayOf(0, 100, 100, 400)
                vibrateToast(pattern)
                startTimer()
                innerIntent.putExtra(TIMER_RUNNING, true)
                //innerIntent.putExtra(ACC_EXTRA, event.values[2].toDouble())
                sendBroadcast(innerIntent)

            }

    }

    private fun higherThan(arr : Array<Double>, limit : Double): Boolean {
        var exceed = false
        for (i in arr){
            if (i > limit){
                exceed = true
                break
            }
        }
        return exceed
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }


    private fun notificationCall(Message:String){
        Log.d("Acceleration Not", "NotificationCall")
        val pattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        val intent = Intent(this, LauncherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(
                channelId,
                description,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = pattern
            notificationManager.createNotificationChannel(notificationChannel)
            builder = Notification.Builder(this, channelId).setContentTitle(
                "Fitness Timer Notification"
            ).setContentText(Message) .setSmallIcon(R.drawable.ic_brightness)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources, R.drawable
                            .ic_launcher_background
                    )
                ).setContentIntent(pendingIntent).setTimeoutAfter(3500)
        }

        notificationManager.notify(12457, builder.build())
    }


    //Outer timer functions

/*    private inner class OuterTimeTask(private var outerTime: Double, private val timer: Timer) : TimerTask() {
        override fun run() {
            val intent = Intent(AccelerationService.TIMER_UPDATED)
            outerTime++
            Log.d(TAG, "time: " + outerTime)
            intent.putExtra(AccelerationService.TIME_EXTRA, outerTime)
            sendBroadcast(intent)
        }
    }*/


    private val updateTime: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            Log.d(TAG, "outerTime: " + intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0).toString() + " s")
            var broadcastTimeIntent = Intent(BROADCASTTIME)
            var t = intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0)
            broadcastTimeIntent.putExtra(BROADCASTTIME, t)
            broadcastTimeIntent.putExtra(STOPTIME, nextTime)
            broadcastTimeIntent.putExtra(BUTTONKEY, nextKey)
            broadcastTimeIntent.putExtra(LASTKEY, lastKey)
            sendBroadcast(broadcastTimeIntent)
            lastKey = false
            if(t>=nextTime)
            {
                //notificationCall("Timer runs for " + (t).toString() + "s.")
                resetTimer()
                outerTimerIntent.putExtra(TIMER_RUNNING, false)
                timerRunning.putExtra(TIMER_RUNNING, false)
                sendBroadcast(timerRunning)
                var temp = nextTimeElement()
                nextTime = temp.first
                nextKey = temp.second
                lastKey = temp.third
                Log.d("LastKey", lastKey.toString())
                Toast.makeText(applicationContext, "Stop after " + t.toString() + "s", Toast.LENGTH_SHORT).show()
                val pattern = longArrayOf(0, 400, 100, 100)
                vibrateToast(pattern)
            }
        }
    }

    @SuppressLint("ServiceCast")
    fun vibrateToast (pattern:LongArray){
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) { // Vibrator availability checking
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, VibrationEffect.DEFAULT_AMPLITUDE)) // New vibrate method for API Level 26 or higher

            } else {
                vibrator.vibrate(500) // Vibrate method for below API Level 26
            }
        }
    }
    private fun nextTimeElement(): Triple<Int, Int, Boolean> {
        var counter = -1
        var found = true
        var last = false

        if (useHashmape.isEmpty()){
            useHashmape = HashMap(numbersMapHash)
            last = true
            Log.d("NextElement_1", numbersMapHash.keys.toString())
            Log.d("NextElement_2", useHashmape.keys.toString())
        }

        while(found){
            counter = counter + 1
            found = !useHashmape.containsKey(counter)
        }
        Log.d("CounterLog", useHashmape.keys.toString())
        var value:Int
        value = useHashmape.get(counter)!!
        useHashmape.remove(counter)
        return Triple(value, counter, last)
    }
    private fun startTimer()
    {
        outerTimerIntent.putExtra(TimerService.TIMER_UPDATED, 0.0)
        outerTimerIntent.putExtra(TimerService.INTERVAL, 25.0)
        startService(outerTimerIntent)

        Log.d(TAG, "TimerStart with: " + outerTimerIntent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0).toString())
    }

    private fun stopTimer()
    {
        stopService(outerTimerIntent)
        innerIntent.putExtra(TIMER_RUNNING, false)
        timerRunning.putExtra(TIMER_RUNNING, false)
        sendBroadcast(timerRunning)
        //timerRunning = false
        //serviceIntentAcc.putExtra(AccelerationService.TIMER_RUNNING, timerRunning)
    }

    private fun resetTimer()
    {
        stopTimer()
        //startMeasurment()
        //outerTime = 0.0
        //var broadcastTimeIntent = Intent(BROADCASTTIME)
        //broadcastTimeIntent.putExtra(BROADCASTTIME, outerTime)
        //sendBroadcast(broadcastTimeIntent)
    }


    //Input varibles for service
    companion object
    {
        const val ACC_Updated = "accUpdated"
        const val VALUEMAP = "valueMap"
        const val ACC_EXTRA = "accExtra"
        const val TIMER_START = "timerStart"
        const val TIMER_RUNNING = "timerisRunning"
        const val MEASUREWAIT = "measureWait"
        const val INNER_TIMER_RUNNING = "innerTimerRunning"
        const val BROADCASTTIME = "braodcastTime"
        const val STOPTIME = "stopTime"
        const val BUTTONKEY = "buttonKey"
        const val LASTKEY = "lastKey"
        const val INTERVAL = "timeInterval"
        const val TIMER_UPDATED_Acc = "timerUpdatedAcc"
        const val TIME_EXTRA_Acc = "timeExtraAcc"
        const val INNER_TIMER_RUNNING_NEW = "innerTimerRunningNew"

    }
}