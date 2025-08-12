package com.example.crisislink.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothAdapter
import android.util.Log
import java.io.IOException

class ConnectThread(
    private val device: BluetoothDevice,
    private val onSocketConnected: (BluetoothSocket) -> Unit
) : Thread() {

    private val socket: BluetoothSocket? =
        device.createRfcommSocketToServiceRecord(APP_UUID)

    override fun run() {
        // Cancel discovery on the default adapter for faster connection
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        try {
            socket?.connect()
            socket?.let { onSocketConnected(it) }
        } catch (e: IOException) {
            Log.e("ConnectThread", "Connection failed", e)
            try { socket?.close() } catch (_: IOException) {}
        }
    }

    fun cancel() {
        try { socket?.close() } catch (_: IOException) {}
    }
}
