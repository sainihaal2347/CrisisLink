package com.example.crisislink.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException

class AcceptThread(
    private val adapter: BluetoothAdapter,
    private val onSocketConnected: (BluetoothSocket) -> Unit
) : Thread() {
    private val serverSocket: BluetoothServerSocket? =
        adapter.listenUsingRfcommWithServiceRecord("CrisisLink", APP_UUID)
    private var running = true

    override fun run() {
        while (running) {
            try {
                val socket: BluetoothSocket = serverSocket!!.accept()
                onSocketConnected(socket)      // Use the callback
                serverSocket.close()
                running = false
            } catch (e: IOException) {
                Log.e("AcceptThread", "Accept failed", e)
                running = false
            }
        }
    }

    fun cancel() {
        running = false
        try { serverSocket?.close() } catch (_: IOException) {}
    }
}
