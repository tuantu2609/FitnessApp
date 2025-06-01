package com.example.fitnesstrackerapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import com.example.fitnesstrackerapp.data.StepDatabase
import com.example.fitnesstrackerapp.model.StepEntry
import kotlinx.coroutines.*
import android.content.ComponentName
import android.content.ServiceConnection

class StepBackgroundService : Service() {

    private var job: Job? = null
    private var boundService: StepBoundService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val stepBinder = binder as? StepBoundService.StepBinder
            boundService = stepBinder?.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, StepBoundService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)

        job = CoroutineScope(Dispatchers.IO).launch {
            val db = StepDatabase.getDatabase(applicationContext)
            while (isActive) {
                delay(5000)
                val steps = boundService?.getStepCount() ?: 0
                val entry = StepEntry(stepCount = steps)
                db.stepDao().insert(entry)
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
