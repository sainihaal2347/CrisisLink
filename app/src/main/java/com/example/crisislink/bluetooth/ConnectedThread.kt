package com.example.crisislink.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException

class ConnectedThread(private val socket: BluetoothSocket) : Thread() {

    private val inputStream = socket.inputStream
    private val outputStream = socket.outputStream
    private val buffer = ByteArray(1024)

    /** Callback invoked when a message is received */
    var onMessageReceived: ((String) -> Unit)? = null

    override fun run() {
        try {
            while (true) {
                val bytesRead = inputStream.read(buffer) // Blocking read
                if (bytesRead > 0) {
                    val message = String(buffer, 0, bytesRead)
                    onMessageReceived?.invoke(message)
                }
            }
        } catch (e: IOException) {
            Log.e("ConnectedThread", "Connection lost", e)
        }
    }

    /** Write bytes to the output stream */
    fun write(data: String) {
        try {
            outputStream.write(data.toByteArray())
        } catch (e: IOException) {
            Log.e("ConnectedThread", "Error writing to output stream", e)
        }
    }

    /** Cancel and close the socket */
    fun cancel() {
        try {
            socket.close()
        } catch (e: IOException) {
            Log.e("ConnectedThread", "Could not close socket", e)
        }
    }
}
