package com.example.fitnesstrackerapp.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import kotlinx.coroutines.tasks.await
import android.util.Log

class WearSyncHelper(private val context: Context) {

    private val dataClient = Wearable.getDataClient(context)

    suspend fun syncStepData(steps: Int, calories: Double, points: Int) {
        Log.d("WearSyncHelper", "Preparing to send data to WearOS")
        Log.d("WearSyncHelper", "Data to send: steps=$steps, calories=$calories, points=$points")


        val putDataReq = PutDataMapRequest.create("/fitness_data").apply {
            dataMap.putInt("steps", steps)
            dataMap.putDouble("calories", calories)
            dataMap.putInt("points", points)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            val result = dataClient.putDataItem(putDataReq).await()
            Log.d("WearSyncHelper", "Data sent successfully. Uri=${result.uri}")
        } catch (e: Exception) {
            Log.e("WearSyncHelper", "Failed to send data to WearOS", e)
        }
    }
}
