package com.example.fitnesstrackerapp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*

class StepBoundService : Service() {

    private val binder = StepBinder()
    private var stepCount = 0
    private var calories = 0.0
    private var points = 0
    private var job: Job? = null

    inner class StepBinder : Binder() {
        fun getService(): StepBoundService = this@StepBoundService
    }

    override fun onCreate() {
        super.onCreate()
        startStepSimulation() // Start simulation as soon as the service is created
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun startStepSimulation() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000)
//                stepCount++
                stepCount += 4
                calories = stepCount * 0.04
                points = stepCount / 100
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    fun getStepCount(): Int = stepCount
    fun getCalories(): Double = calories
    fun getPoints(): Int = points
}
