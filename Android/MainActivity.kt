package com.simplebuttonandtoggle

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    // UI
    private lateinit var lightTextView: TextView
    private lateinit var bluetoothTextView: TextView
    private var isLightOn = false
    private var isBluetoothConnected = false
    private val REQUEST_ENABLE_BT = 1

    // Bluetooth
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Default UUID
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
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
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("BluetoothTesterApp", MY_UUID)
                bluetoothTextView.text = "SEARCHING..."
                val socket: BluetoothSocket? = serverSocket?.accept()
                socket?.let {
                    manageConnectedSocket(it)
                    serverSocket?.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Manages the connection once a client is connected
    private fun manageConnectedSocket(socket: BluetoothSocket) {
        clientSocket = socket
        runOnUiThread {
            isBluetoothConnected = true
            updateBluetoothStatus()
        }
        try {
            val inputStream = socket.inputStream
            val outputStream = socket.outputStream
            while (isBluetoothConnected) {
                val signal = inputStream.read()
                if (signal == 1) {
                    isLightOn = !isLightOn
                    runOnUiThread { updateLightStatus() }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

    // Use reflection to create an RFCOMM socket connection to a paired device
    @SuppressLint("MissingPermission")
    private fun createRfcommSocket(theBTDevice: BluetoothDevice, channel: Int): BluetoothSocket? {
        return try {
            val cls = Class.forName("android.bluetooth.BluetoothDevice")
            val meth = cls.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            meth.invoke(theBTDevice, channel) as? BluetoothSocket
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Use reflection to retrieve UUIDs from the Bluetooth device
    @SuppressLint("MissingPermission")
    private fun getDeviceUUIDs(device: BluetoothDevice): Array<ParcelUuid>? {
        return try {
            val cls = Class.forName("android.bluetooth.BluetoothDevice")
            val meth = cls.getMethod("getUuids")
            meth.invoke(device) as? Array<ParcelUuid>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateLightStatus() {
        if (isLightOn) {
            lightTextView.text = "Light On"
            lightTextView.setBackgroundColor(Color.GREEN)
        } else {
            lightTextView.text = "Light Off"
            lightTextView.setBackgroundColor(Color.RED)
        }
    }

    private fun updateBluetoothStatus() {
        bluetoothTextView.text = if (isBluetoothConnected) "CONNECTED" else "DISCONNECTED"
        bluetoothTextView.setBackgroundColor(if (isBluetoothConnected) Color.BLUE else Color.GRAY)
    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket?.close()
        clientSocket?.close()
    }
}