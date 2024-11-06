package com.bluetoothtesterandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var sendSignalButton: Button
    private lateinit var signalLight: View

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendSignalButton = findViewById(R.id.sendSignalButton)
        signalLight = findViewById(R.id.signalLight)

        // Register for permission result for Bluetooth connection
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)

        sendSignalButton.setOnClickListener {
            sendSignal()
        }

        connectToBluetoothDevice() // Initiates connection to your dumbphone
        startListeningForSignals()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Log.e("Bluetooth", "Bluetooth permission denied")
            }
        }

    // Define a request code for the permission
    private val btPermissionRequestCode = 1

    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToBluetoothDevice() {
        val deviceAddress = "f8:a9:d0:ed:11:56" // Replace with your dumbphone's Bluetooth MAC address
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            try {
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                bluetoothSocket?.connect()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Could not connect to device", e)
            }
        } else {
            // Request the BLUETOOTH_CONNECT permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                btPermissionRequestCode
            )
        }
    }

    // Handle the result of the permission request
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == btPermissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was granted; try to connect again
                connectToBluetoothDevice()
            } else {
                Log.e("Bluetooth", "Bluetooth connection permission denied")
            }
        }
    }

    private fun sendSignal() {
        bluetoothSocket?.let { socket ->
            try {
                val outputStream: OutputStream = socket.outputStream
                outputStream.write("signal".toByteArray()) // Send signal as a string
                outputStream.flush()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Could not send signal", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startListeningForSignals() {
        bluetoothSocket?.let { socket ->
            Thread {
                try {
                    val inputStream: InputStream = socket.inputStream
                    val buffer = ByteArray(1024)
                    var bytesRead: Int

                    while (true) {
                        bytesRead = inputStream.read(buffer)
                        val message = String(buffer, 0, bytesRead)
                        if (message == "signal_received") {
                            runOnUiThread {
                                signalLight.setBackgroundColor(getColor(R.color.teal_200)) // Set signal light color to indicate signal
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error reading from input stream", e)
                }
            }.start()
        }
    }
}