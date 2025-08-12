package com.example.crisislink.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothController(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusMessage = MutableStateFlow("Bluetooth Disabled")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Broadcast receiver for device discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return
                    }

                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        val currentList = _discoveredDevices.value.toMutableList()
                        if (!currentList.contains(it)) {
                            currentList.add(it)
                            _discoveredDevices.value = currentList
                            Log.d("BluetoothController", "Found device: ${it.name ?: "Unknown"}")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                    _statusMessage.value = "Scanning for devices..."
                    Log.d("BluetoothController", "Discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    val deviceCount = _discoveredDevices.value.size
                    _statusMessage.value = if (deviceCount > 0) {
                        "Found $deviceCount devices"
                    } else {
                        "No devices found"
                    }
                    Log.d("BluetoothController", "Discovery finished")
                }
            }
        }
    }

    init {
        updateBluetoothStatus()
    }

    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    fun updateBluetoothStatus() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _statusMessage.value = if (_isBluetoothEnabled.value) {
            "Bluetooth Enabled - Ready to Connect"
        } else {
            "Bluetooth Disabled"
        }
    }

    fun startDiscovery(): Boolean {
        if (!_isBluetoothEnabled.value) {
            _statusMessage.value = "Please enable Bluetooth first"
            return false
        }

        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                _statusMessage.value = "Bluetooth scan permission required"
                return false
            }
        } else {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                _statusMessage.value = "Location permission required"
                return false
            }
        }

        // Register receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)

        // Clear previous discoveries and add paired devices
        _discoveredDevices.value = getPairedDevices()

        // Start discovery
        return bluetoothAdapter?.startDiscovery() == true
    }

    fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            context.unregisterReceiver(discoveryReceiver)
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e("BluetoothController", "Error stopping discovery: ${e.message}")
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun connectToDevice(device: BluetoothDevice) {
        // For now, just simulate connection
        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            device.address
        } else {
            device.name ?: device.address
        }
        _statusMessage.value = "Connected to $deviceName"
        Log.d("BluetoothController", "Connected to $deviceName")
    }

    fun release() {
        try {
            stopDiscovery()
        } catch (e: Exception) {
            Log.e("BluetoothController", "Error releasing resources: ${e.message}")
        }
    }
}
