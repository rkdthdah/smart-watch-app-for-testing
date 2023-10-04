package com.example.myapplication.presentation

import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class WearableService(mainActivity: MainActivity): WearableListenerService(), MessageClient.OnMessageReceivedListener {

    override fun onCreate() {
        super.onCreate()

        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(p0: MessageEvent) {
        if (p0.path == "/start") {
            Log.i("request", "start")
            MainActivity().startRecording()
        }
        if (p0.path == "/stop") {
            Log.i("request", "stop")
            val sensorData = MainActivity().stopRecording()

            sendMessageToPhone("/data", sensorData.toString())
        }
    }

    private fun sendMessageToPhone(path: String, message: String) {
        Wearable.getNodeClient(this)
            .connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    val sendMessageTask = Wearable.getMessageClient(this)
                        .sendMessage(node.id, path, message.toByteArray())

                    sendMessageTask.addOnSuccessListener {
                        Log.i("send", "success")
                    }.addOnFailureListener {
                        Log.i("send", "fail")
                    }
                }
            }
            .addOnFailureListener {
                Log.i("node", "fail")
            }
    }
}