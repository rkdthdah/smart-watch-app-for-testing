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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.json.JSONException
import org.json.JSONObject
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private val sensorManager: SensorManager by lazy {
        this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val accSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }
    private val rotSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
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

    private var activityContext: Context? = null

    private val TAG_MESSAGE_RECEIVED = "receive1"
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private var mobileDeviceConnected: Boolean = false


    // Payload string items
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"


    private var messageEvent: MessageEvent? = null
    private var mobileNodeUri: String? = null

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

        activityContext = this
    }

    fun startRecording() {
        val now = System.currentTimeMillis()
        jsonObject = JSONObject()
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

    fun stopRecording(): JSONObject {
        //saveSensorData(jsonObject.toString())
        handler.removeCallbacksAndMessages(null)
        return jsonObject
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

        Wearable.getMessageClient(activityContext!!).removeListener(this)
    }

    @Override
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(accL, accSensor, 50000)
        sensorManager.registerListener(rotL, rotSensor, 50000)

        Wearable.getMessageClient(activityContext!!).addListener(this)
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

    override fun onMessageReceived(p0: MessageEvent) {

        try {
            val s1 = String(p0.data)
            val path: String = p0.path

            Log.d(TAG_MESSAGE_RECEIVED, "Message received: $path")
            Toast.makeText(activityContext, "$path, $s1 받음", Toast.LENGTH_LONG).show()


            //Send back a message back to the source node
            //This acknowledges that the receiver activity is open
            if (path.isNotEmpty() && path == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                try {
                    // Get the node id of the node that created the data item from the host portion of
                    // the uri.
                    val nodeId: String = p0.sourceNodeId
                    // Set the data of the message to be the bytes of the Uri.
                    val returnPayloadAck = wearableAppCheckPayloadReturnACK
                    val payload: ByteArray = returnPayloadAck.toByteArray()

                    // Send the rpc
                    // Instantiates clients without member variables, as clients are inexpensive to
                    // create. (They are cached and shared between GoogleApi instances.)
                    val sendMessageTask = Wearable.getMessageClient(activityContext!!)
                            .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)

                    Log.d(TAG_MESSAGE_RECEIVED, "ACK with payload: $returnPayloadAck")

                    messageEvent = p0
                    mobileNodeUri = p0.sourceNodeId

                    sendMessageTask.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d(TAG_MESSAGE_RECEIVED, "Message sent successfully")

                            mobileDeviceConnected = true
                        } else {
                            Log.d(TAG_MESSAGE_RECEIVED, "Message failed.")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }//emd of if
            else if (path.isNotEmpty() && path == MESSAGE_ITEM_RECEIVED_PATH) {
                if (s1.isNotEmpty() && s1 == "start") {
                    Log.d("request", "start")

                    startRecording()
                } else if (s1.isNotEmpty() && s1 == "stop") {
                    Log.d("request", "stop")

                    val nodeId: String = p0.sourceNodeId

                    val sensorData = stopRecording()
                    val sendMessageTask = Wearable.getMessageClient(activityContext!!)
                        .sendMessage(nodeId, MESSAGE_ITEM_RECEIVED_PATH, sensorData.toString().toByteArray())

                    sendMessageTask.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d("send1", "Message sent successfully")
                            Toast.makeText(activityContext, "json 전송 성공", Toast.LENGTH_LONG).show()
                        } else {
                            Log.d("send1", "Message failed.")
                            Toast.makeText(activityContext, "json 전송 실패", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}