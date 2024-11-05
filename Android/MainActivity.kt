package com.bluetoothtesterandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID

class AndroidApp : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var socket: BluetoothSocket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null
    private lateinit var signalLight: TextView
    private lateinit var sendButton: Button
    private var lightOn = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signalLight = findViewById(R.id.signalLight)
        sendButton = findViewById(R.id.sendButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter.getRemoteDevice("f8:a9:d0:ed:11:56")  // replace with the J2ME device's address

        // Run Bluetooth connection and listener on a separate thread
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()  // You might want to prompt the user instead
        }
        Thread {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    socket?.connect()
                    inputStream = DataInputStream(socket?.inputStream)
                    outputStream = DataOutputStream(socket?.outputStream)

                    // Listen for incoming signals from the J2ME app
                    while (true) {
                        val signal = inputStream?.readInt()
                        if (signal == 1) {
                            lightOn = !lightOn
                            runOnUiThread { updateLight() }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    signalLight.text = "Connection Failed"
                }
            }
        }.start()

        sendButton.setOnClickListener {
            sendSignal()
        }
    }

    private fun updateLight() {
        signalLight.text = "Signal Light: ${if (lightOn) "ON" else "OFF"}"
    }

    private fun sendSignal() {
        try {
            outputStream?.writeInt(1)
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}