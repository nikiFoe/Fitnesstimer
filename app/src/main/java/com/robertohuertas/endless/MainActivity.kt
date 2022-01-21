package com.robertohuertas.endless

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.robertohuertas.endless.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import android.app.AlertDialog
import android.content.DialogInterface
import java.io.Serializable


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var serviceIntentTimer: Intent
    private var numbersMap = mutableMapOf<Int, Button>()
    private var buttonMapHash = hashMapOf<Int, Button>()
    private var numbersMapHash = hashMapOf<Int, Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        serviceIntentTimer = Intent(applicationContext, AccelerationService::class.java)
        registerReceiver(receivingTime, IntentFilter(AccelerationService.BROADCASTTIME))

        title = "Endless Service"

        findViewById<Button>(R.id.btnStartService).let {
            it.setOnClickListener {
                log("START THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.START)
            }
        }

        findViewById<Button>(R.id.btnStopService).let {
            it.setOnClickListener {
                log("STOP THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.STOP)
            }
        }

        findViewById<Button>(R.id.showDynamicButtons).let {
            it.setOnClickListener{
                numbersMap.keys.forEach{
                    Log.d("NumberMap", ("Key: " + it.toString() + " " + "Value: " + numbersMap[it]?.getText().toString()))
                }

                buttonMapHash.keys.forEach{
                    Log.d("NumberMap", ("Key: " + it.toString() + " " + "Value: " + buttonMapHash[it]?.getText().toString()))
                }
            }
        }
        val numberInput = findViewById<EditText>(R.id.editTextNumber)
        var counter = 0
        var button:Button
        numberInput.setOnClickListener{
            val valueString = numberInput.text.toString()
            val valueLengt = valueString.length

            Log.d("InputTest", numberInput.text.toString())
            //listButtons[counter].setText(valueString)
            //listButtons[counter].setBackgroundColor(getResources().getColor(R.color.grey))
            if (!valueString.isEmpty()){
                Log.d("NotEmpty", numberInput.text.toString())
                //findFirstElement(numbersMap.keys)
                //numbersMap[1]?.setText(valueString)
                button = createButtonDynamically(counter.toString(), valueString)
                numbersMap[counter] = button
                buttonMapHash.put(counter, button)
                //numbersMap[1]?.setBackgroundColor(getResources().getColor(R.color.grey))
                numberInput.text.delete(0, valueLengt)
                counter = counter + 1
                Log.d("Map_kl", numbersMap.keys.toString())
            }

        }

    }

    private fun createButtonDynamically(ID: String, value: String):Button {
        // creating the button
        val dynamicButton = Button(this)
        // setting layout_width and layout_height using layout parameters
        dynamicButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT

        )
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.setMargins(
            5,
            5, 5, 5
        )
        dynamicButton.layoutParams = layoutParams
        dynamicButton.text = value
        dynamicButton.setTextColor(Color.WHITE)
        dynamicButton.setBackgroundColor(Color.GRAY)
        var name = "dynamicButton_"
        dynamicButton.id = ID.toInt()
        val shape = GradientDrawable()
        shape.cornerRadius = 8f
        // add Button to LinearLayout
        val layout = findViewById<android.widget.GridLayout>(R.id.grid)
        dynamicButton.background = roundedCornersDrawable(
            2.dpToPixels(applicationContext), // border width in pixels
            Color.GRAY, // border color
            10.dpToPixels(applicationContext).toFloat() // corners radius
        )
        layout.addView(dynamicButton)
        findViewById<Button>(ID.toInt()).let {
            it.setOnClickListener {
                val animationBounce = AnimationUtils.loadAnimation(this, R.anim.bounce_press)
                it.startAnimation(animationBounce)
                layout.removeView(it)
                numbersMap.remove(ID.toInt())
                buttonMapHash.remove(ID.toInt())
                Log.d("LongClick", "Pressed")
            }
            it.setOnLongClickListener{
                val animationBounce = AnimationUtils.loadAnimation(this, R.anim.bounce_hold)
                it.startAnimation(animationBounce)
                var newValue:String
                newValue = callAlert(dynamicButton)
                Log.d("LongClick", newValue)
                return@setOnLongClickListener true

            }
        }


        return dynamicButton
    }


    fun roundedCornersDrawable(
        borderWidth: Int = 10, // border width in pixels
        borderColor: Int = Color.GRAY, // border color
        cornerRadius: Float = 25F, // corner radius in pixels
        bgColor: Int = Color.GRAY // view background color
    ): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(borderWidth, borderColor)
            setColor(bgColor)
            // make it rounded corners
            this.cornerRadius = cornerRadius
        }
    }


    // extension function to convert dp to equivalent pixels
    fun Int.dpToPixels(context: Context):Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics

    ).toInt()

    private fun callAlert(button: Button):String{
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change Interval")

        // Set up the input
        val input = EditText(this)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setHint("Set Seconds")
        //input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            // Here you get get input text from the Edittext
            button.setText(input.text.toString())
            var m_Text = input.text.toString()
        })
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

        builder.show()
        return input.toString()
    }

    private fun findFirstElement(keys: MutableSet<Int>){
        Log.d("Keys", keys.toString())
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            convertButtonToNumber()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                it.putExtra(EndlessService.VALUEMAP, numbersMapHash)
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }
    }

    private fun convertButtonToNumber(){
        buttonMapHash.keys.forEach{
            numbersMapHash.put(it, buttonMapHash[it]?.getText().toString().toInt())
        }
    }

    private val receivingTime: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            binding.timeTV.text = getTimeStringFromDouble(
                intent.getDoubleExtra(
                    AccelerationService.BROADCASTTIME,
                    0.0
                )
            )
            Log.d(
                "BraodcastTime",
                intent.getDoubleExtra(AccelerationService.BROADCASTTIME, 0.0).toString()
            )
        }
    }


    private fun getTimeStringFromDouble(time: Double): String
    {
        val resultInt = time.roundToInt()
        val hours = resultInt % 86400 / 3600
        val minutes = resultInt % 86400 % 3600 / 60
        val seconds = resultInt % 86400 % 3600 % 60

        return makeTimeString(hours, minutes, seconds)
    }
    private val buttonClick = AlphaAnimation(1f, 0.8f)

    private fun makeTimeString(hour: Int, min: Int, sec: Int): String = String.format(
        "%02d:%02d:%02d",
        hour,
        min,
        sec
    )

}

