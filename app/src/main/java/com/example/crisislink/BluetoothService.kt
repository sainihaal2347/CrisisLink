package com.example.crisislink

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.crisislink.bluetooth.AcceptThread
import com.example.crisislink.bluetooth.ConnectThread
import com.example.crisislink.bluetooth.ConnectedThread
import com.example.crisislink.database.CrisisLinkDatabase
import com.example.crisislink.database.Message
import kotlinx.coroutines.*
import java.util.*

class BluetoothService : Service() {

    // Binder for Activity binding
    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    private val binder = LocalBinder()
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null
    private var connectedSocket: BluetoothSocket? = null

    private lateinit var database: CrisisLinkDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var connectedDeviceName: String? = null
        private set

    // Track last message we sent to prevent duplicate echo display
    private var lastSentMessage: String? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        database = CrisisLinkDatabase.getDatabase(applicationContext)
        createNotificationChannel()
        startForeground(1, createServiceNotification("Bluetooth Service Running"))

        val adapter = BluetoothAdapter.getDefaultAdapter()
        acceptThread = AcceptThread(adapter, this::onSocketConnected).apply { start() }
    }

    fun connectToDevice(device: BluetoothDevice) {
        connectedThread?.cancel()
        ConnectThread(device, this::onSocketConnected).start()
    }

    private fun onSocketConnected(socket: BluetoothSocket) {
        connectedSocket = socket
        connectedDeviceName = socket.remoteDevice.name ?: socket.remoteDevice.address

        connectedThread?.cancel()
        connectedThread = ConnectedThread(socket).apply {
            onMessageReceived = { message ->
                // Prevent inserting your own echoed message
                if (message != lastSentMessage) {
                    postMessageNotification(message)
                    serviceScope.launch {
                        database.messageDao().insertMessage(
                            Message(
                                content = message,
                                senderName = "Remote",
                                senderAddress = socket.remoteDevice.address,
                                timestamp = System.currentTimeMillis(),
                                isEmergency = false,
                                isReceived = true
                            )
                        )
                    }
                } else {
                    // Clear saved last message so same text can be sent again later
                    lastSentMessage = null
                }
            }
            start()
        }
    }

    // Public method: send and mark as last sent to prevent echo duplication
    fun sendMessageOverBluetooth(text: String) {
        lastSentMessage = text
        connectedThread?.write(text)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "crisislink_channel",
                "CrisisLink Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(content: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, "crisislink_channel")
            .setContentTitle("CrisisLink")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun postMessageNotification(message: String) {
        val notification = NotificationCompat.Builder(this, "crisislink_channel")
            .setContentTitle("New CrisisLink Message")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(Random().nextInt(), notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        acceptThread?.cancel()
        connectedThread?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
