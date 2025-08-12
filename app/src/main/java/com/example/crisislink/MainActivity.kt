package com.example.crisislink

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.crisislink.database.CrisisLinkDatabase
import com.example.crisislink.database.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var bluetoothService: BluetoothService? = null
    private var isServiceBound = false
    private val connectedDeviceName = mutableStateOf<String?>(null)
    private lateinit var database: CrisisLinkDatabase

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            isServiceBound = true
            connectedDeviceName.value = bluetoothService?.connectedDeviceName
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
            isServiceBound = false
        }
    }

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.all { it.value }) startBluetoothService()
            else Toast.makeText(this, "Permissions denied.", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = CrisisLinkDatabase.getDatabase(this)
        requestAllPermissions()
        setContent { MaterialTheme { MainUI() } }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, BluetoothService::class.java).also {
            bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    /** Ask all Bluetooth/notification runtime permissions */
    private fun requestAllPermissions() {
        val required = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required += listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required += Manifest.permission.POST_NOTIFICATIONS
        }
        val toAsk = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toAsk.isEmpty()) startBluetoothService()
        else requestMultiplePermissionsLauncher.launch(toAsk.toTypedArray())
    }

    private fun startBluetoothService() {
        ContextCompat.startForegroundService(this, Intent(this, BluetoothService::class.java))
    }

    private fun sendMessage(text: String, isEmergency: Boolean = false) {
        bluetoothService?.sendMessageOverBluetooth(text)
        lifecycleScope.launch {
            database.messageDao().insertMessage(
                Message(
                    content = text,
                    senderName = "Me",
                    senderAddress = "Local Device",
                    timestamp = System.currentTimeMillis(),
                    isEmergency = isEmergency,
                    isReceived = false
                )
            )
        }
    }
    private fun sendEmergencyAlert() {
        val alert = "🚨 EMERGENCY SOS at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
        sendMessage(alert, isEmergency = true)
    }

    @Composable
    private fun MainUI() {
        val messages by database.messageDao().getRecentMessages().collectAsState(initial = emptyList())
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
        var msgText by remember { mutableStateOf("") }
        var isScanning by remember { mutableStateOf(false) }

        // Scroll state for message list
        val listState = rememberLazyListState()
        // Auto-scroll to bottom when messages size changes
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }

        Scaffold(
            topBar = {
                Column(Modifier.fillMaxWidth()) {
                    // Connection status bar
                    Surface(
                        color = if (connectedDeviceName.value != null) Color(0xFF4CAF50) else Color.Red
                    ) {
                        Text(
                            text = connectedDeviceName.value?.let { "Connected to $it" } ?: "Not connected",
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Emergency button
                    Button(
                        onClick = { sendEmergencyAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("🚨 EMERGENCY SOS 🚨", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            bottomBar = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = msgText,
                        onValueChange = { msgText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { sendMessage(msgText); msgText = "" },
                        enabled = msgText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Send") }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                // Scan button
                Button(
                    onClick = {
                        if (btAdapter?.isEnabled == true) {
                            isScanning = true
                            devices = btAdapter.bondedDevices.toList()
                            btAdapter.cancelDiscovery()
                            btAdapter.startDiscovery()
                            isScanning = false
                        } else {
                            Toast.makeText(this@MainActivity, "Enable Bluetooth first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) { Text(if (isScanning) "Scanning..." else "Scan Devices") }

                // Device list
                if (devices.isNotEmpty()) {
                    Text("Available Devices", fontWeight = FontWeight.Bold)
                    LazyColumn(
                        Modifier
                            .height(120.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(devices) { device ->
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        bluetoothService?.connectToDevice(device)
                                        connectedDeviceName.value = device.name
                                    }
                                    .padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(device.name ?: "Unknown", fontWeight = FontWeight.Bold)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Messages - normal order, auto-scrolled to bottom
                Text("Messages", fontWeight = FontWeight.Bold)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages) { msg ->
                        val isMine = !msg.isReceived
                        val bubbleColor = when {
                            msg.isEmergency -> Color(0xFFFFC1C1)
                            isMine -> Color(0xFFD1FFC4)
                            else -> Color.White
                        }
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(msg.content)
                                    Text(
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
