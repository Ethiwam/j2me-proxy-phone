package com.simplebuttonandtoggle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {
    // UI
    private lateinit var lightTextView: TextView
    private lateinit var bluetoothTextView: TextView
    private var isLightOn = false
    private var isBluetoothConnected = false
    private val REQUEST_ENABLE_BT = 1

    // Bluetooth
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID for the Bluetooth connection
    private var serverSocket: BluetoothServerSocket? = null

    // Bluetooth Adapter and Manager
    private val bluetoothManager: BluetoothManager by lazy { getSystemService(BluetoothManager::class.java) }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        lightTextView = findViewById(R.id.lightTextView)
        bluetoothTextView = findViewById(R.id.bluetoothTextView)
        val toggleButton: Button = findViewById(R.id.toggleButton)

        // Set button click listener to toggle the light
        toggleButton.setOnClickListener {
            isLightOn = !isLightOn
            updateLightStatus()
        }

        // Enable Bluetooth if it's not already enabled
        if (bluetoothAdapter == null) {
            bluetoothTextView.text = "Bluetooth not supported"
        } else {
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            // Start Bluetooth server connection
            startBluetoothServer()
        }
    }

    // Starts the Bluetooth server socket to listen for connections
    @SuppressLint("MissingPermission")
    private fun startBluetoothServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create the Bluetooth server socket
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("BluetoothTesterApp", MY_UUID)

                // Wait for a client to connect
                val socket: BluetoothSocket? = serverSocket?.accept()

                // When a connection is accepted, handle it
                socket?.let {
                    manageConnectedSocket(it)
                    serverSocket?.close() // Close the server socket once a client is connected
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Manages the connection once a client is connected
    private fun manageConnectedSocket(socket: BluetoothSocket) {
        // Update connection status on the main thread
        runOnUiThread {
            isBluetoothConnected = true
            updateBluetoothStatus()
        }

        try {
            val inputStream = socket.inputStream
            val outputStream = socket.outputStream

            // Continuously listen for incoming data from the client
            while (isBluetoothConnected) {
                val signal = inputStream.read() // Assuming the client sends a single byte signal
                if (signal == 1) { // If signal is received to toggle light
                    isLightOn = !isLightOn
                    runOnUiThread {
                        updateLightStatus()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

    // Updates the light status on screen
    private fun updateLightStatus() {
        if (isLightOn) {
            lightTextView.text = "Light On"
            lightTextView.setBackgroundColor(Color.GREEN) // Green for 'on' state
        } else {
            lightTextView.text = "Light Off"
            lightTextView.setBackgroundColor(Color.RED) // Red for 'off' state
        }
    }

    // Updates the Bluetooth connection status on screen
    private fun updateBluetoothStatus() {
            bluetoothTextView.text = if (isBluetoothConnected) "CONNECTED" else "DISCONNECTED"
        bluetoothTextView.setBackgroundColor(if (isBluetoothConnected) Color.BLUE else Color.GRAY)
    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket?.close() // Close server socket when app is destroyed
    }
}