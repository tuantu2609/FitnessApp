package com.example.fitnesstrackerapp.ui

import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesstrackerapp.R
import com.example.fitnesstrackerapp.service.StepBoundService
import com.example.fitnesstrackerapp.service.StepForegroundService
import com.example.fitnesstrackerapp.sync.WearSyncHelper    // Thêm import này
import com.example.fitnesstrackerapp.util.CalorieTask
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private lateinit var stepText: TextView
    private lateinit var calorieText: TextView
    private lateinit var pointText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var badgeImage: ImageView
    private lateinit var quoteText: TextView

    private var boundService: StepBoundService? = null
    private var isBound = false
    private var updateJob: Job? = null

    private val milestoneSteps = listOf(50, 100, 150)
    private var lastMilestone = 0

    private val REQUEST_CODE_POST_NOTIFICATIONS = 101

    private val wearSyncHelper by lazy { WearSyncHelper(this) }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as StepBoundService.StepBinder
            boundService = binder.getService()
            isBound = true
            startUpdatingUI()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            boundService = null
            stopUpdatingUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stepText = findViewById(R.id.stepsText)
        calorieText = findViewById(R.id.caloriesText)
        pointText = findViewById(R.id.pointsText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.pauseButton)
        badgeImage = findViewById(R.id.badgeImage)
        quoteText = findViewById(R.id.quoteText)

        checkNotificationPermission()

        startButton.setOnClickListener {
            if (!isBound) {
                val boundIntent = Intent(this, StepBoundService::class.java)
                startService(boundIntent)
                bindService(boundIntent, connection, Context.BIND_AUTO_CREATE)

                val fgIntent = Intent(this, StepForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(fgIntent)
                } else {
                    startService(fgIntent)
                }
            }
        }

        stopButton.setOnClickListener {
            if (isBound) {
                unbindService(connection)
                isBound = false
                boundService = null
                stopUpdatingUI()
            }
            stopService(Intent(this, StepBoundService::class.java))
            stopService(Intent(this, StepForegroundService::class.java))
            badgeImage.visibility = View.GONE
            lastMilestone = 0
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            val message = if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                "Notification permission granted"
            } else {
                "Notification permission denied"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startUpdatingUI() {
        if (updateJob?.isActive == true) return
        updateJob = lifecycleScope.launch {
            while (isBound) {
                val steps = boundService?.getStepCount() ?: 0
                CalorieTask(steps) { calories, points ->
                    runOnUiThread {
                        stepText.text = "Steps: $steps"
                        quoteText.text = getRandomMotivationalQuote()
                        calorieText.text = "Calories: $calories"
                        pointText.text = "Points: $points"

                        val nextMilestone = milestoneSteps.firstOrNull { it > lastMilestone && steps >= it }
                        if (nextMilestone != null) {
                            showMilestoneAnimation()
                            lastMilestone = nextMilestone
                        }
                    }

                    // Gửi dữ liệu lên Wear OS trong coroutine khác
                    lifecycleScope.launch {
                        try {
                            wearSyncHelper.syncStepData(steps, calories, points)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }.execute()
                delay(1000)
            }
        }
    }

    private fun stopUpdatingUI() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun showMilestoneAnimation() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.badge_pop)
        badgeImage.visibility = View.VISIBLE
        badgeImage.startAnimation(animation)

        lifecycleScope.launch {
            delay(3000)
            badgeImage.visibility = View.GONE
        }
    }

    private fun getRandomMotivationalQuote(): String {
        val quotes = listOf(
            "Keep going, you're doing great!",
            "Every step counts!",
            "Stay strong and keep moving!",
            "You got this!",
            "Almost there!"
        )
        return quotes.random()
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
            boundService = null
        }
        stopUpdatingUI()
        super.onDestroy()
    }
}
