package com.example.myapplication

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RandomNumberScreen()
                }
            }
        }
    }
}
class RandomNumberService : Service() {

    private val binder = LocalBinder()
    private var number = 0
    private var job: Job? = null
    private val _numberFlow = MutableSharedFlow<Int>(replay = 1)

    val numberFlow: SharedFlow<Int> get() = _numberFlow

    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberService = this@RandomNumberService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startGenerating()
    }

    private fun startGenerating() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000)
                number = Random.nextInt(0, 101)
                _numberFlow.emit(number)
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}

@Composable
fun RandomNumberScreen() {
    val context = LocalContext.current
    var isBound by remember { mutableStateOf(false) }
    var currentNumber by remember { mutableIntStateOf(0) }
    var serviceConnection: ServiceConnection? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()

    DisposableEffect(isBound) {
        if (isBound) {
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val serv = (binder as RandomNumberService.LocalBinder).getService()
                    scope.launch {
                        serv.numberFlow.collect { num ->
                            currentNumber = num
                        }
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {}
            }
            serviceConnection = conn

            val intent = Intent(context, RandomNumberService::class.java)
            context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        }

        onDispose {
            serviceConnection?.let { context.unbindService(it) }
        }
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isBound) "$currentNumber" else "—",
            fontSize = 88.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(64.dp))

        Button(onClick = { isBound = !isBound }) {
            Text(if (isBound) "Отключиться" else "Подключиться")
        }
    }
}