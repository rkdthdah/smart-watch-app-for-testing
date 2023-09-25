/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.myapplication.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.myapplication.R
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    private val sensorManager: SensorManager by lazy {
        this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val accSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }
    private val rotSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    private val accL: SensorEventListener by lazy {
        AccListener(aView)
    }

    private val rotL: SensorEventListener by lazy {
        RotListener(rView)
    }

    lateinit var aView: TextView
    lateinit var rView: TextView

    var ax = ""
    var ay = ""
    var az = ""
    var rx = ""
    var ry = ""
    var rz = ""

    private lateinit var jsonObject: JSONObject
    private val handler = Handler(Looper.getMainLooper())
    private var fileWriter: FileWriter? = null

    private lateinit var startButton: Button
    private lateinit var pauseButton: Button


    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        aView = findViewById<TextView>(R.id.acc_value)
        rView = findViewById<TextView>(R.id.rot_value)

        sensorManager
        accSensor
        rotSensor
        accL
        rotL
        sensorManager.registerListener(accL, accSensor, 50000)
        sensorManager.registerListener(rotL, rotSensor, 50000)

        startButton = findViewById(R.id.start)
        pauseButton = findViewById(R.id.pause)
        pauseButton.isEnabled = false

        startButton.setOnClickListener {
            jsonObject = JSONObject()

            startButton.isEnabled = false
            pauseButton.isEnabled = true

            startRecording()
        }

        pauseButton.setOnClickListener {
            startButton.isEnabled = true
            pauseButton.isEnabled = false

            stopRecording()
        }
    }

    private fun startRecording() {
        var now = System.currentTimeMillis()
        handler.postDelayed(object: Runnable {
            override fun run() {
                try {
                    val nowObject = JSONObject()

                    val accObject = JSONObject()
                    accObject.put("x", ax)
                    accObject.put("y", ay)
                    accObject.put("z", az)

                    nowObject.put("acceleration", accObject)

                    val rotObject = JSONObject()
                    rotObject.put("x", rx)
                    rotObject.put("y", ry)
                    rotObject.put("z", rz)

                    nowObject.put("rotation", rotObject)

                    jsonObject.put((System.currentTimeMillis() - now).toString(), nowObject)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                handler.postDelayed(this, 50)
            }
        }, 50)
    }

    private fun stopRecording() {
        saveSensorData(jsonObject.toString())
        handler.removeCallbacksAndMessages(null)
    }

    private fun saveSensorData(data: String) {
        try {
            val fileName = "Sensor_data_" + LocalDateTime.now().toString() + ".json"
            val fos: FileOutputStream = openFileOutput(fileName, Context.MODE_APPEND)
            fos.write(data.toByteArray())
            fos.close()
            Log.i("data", data)
            Log.i("filename", fileName)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Override
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accL)
        sensorManager.unregisterListener(rotL)
    }

    @Override
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(accL, accSensor, 50000)
        sensorManager.registerListener(rotL, rotSensor, 50000)
    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(accL)
        sensorManager.unregisterListener(rotL)
    }

    private class AccListener(private val view: TextView): SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            view.text = "x = " + String.format("%.2f", p0!!.values[0]) + "   y = " + String.format("%.2f", p0.values[1]) + "   z = " + String.format("%.2f", p0.values[2])
            (view.context as MainActivity).ax = String.format("%.4f", p0.values[0])
            (view.context as MainActivity).ay = String.format("%.4f", p0.values[1])
            (view.context as MainActivity).az = String.format("%.4f", p0.values[2])
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }
    }

    private class RotListener(private val view: TextView): SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            view.text = "x = " + String.format("%.2f", p0!!.values[0]) + "   y = " + String.format("%.2f", p0.values[1]) + "   z = " + String.format("%.2f", p0.values[2])
            (view.context as MainActivity).rx = String.format("%.4f", p0.values[0])
            (view.context as MainActivity).ry = String.format("%.4f", p0.values[1])
            (view.context as MainActivity).rz = String.format("%.4f", p0.values[2])
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        }

    }
}