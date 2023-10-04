package com.example.backgroundworks

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.backgroundworks.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var alarmManager: AlarmManager? = null
    private lateinit var workManager: WorkManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        if (map.values.isNotEmpty() && map.values.all { it }) {
            startLocation()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {}
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.timePicker.setIs24HourView(true)
        val timeZone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)
        workManager = WorkManager.getInstance(applicationContext)

        binding.buttonStart.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val outputData = Data.Builder()
                    .putDouble(Constants.LATITUDE, location.latitude)
                    .putDouble(Constants.LONGITUDE, location.longitude)
                    .putString(Constants.TIME_ZONE, timeZone)
                    .putInt(Constants.USER_HOUR, binding.timePicker.hour)
                    .putInt(Constants.USER_MINUTE, binding.timePicker.minute)
                    .build()

                val workRequest = OneTimeWorkRequest.Builder(MyWork::class.java)
                    .setInputData(outputData)
                    .addTag(WORKER_TAG)
                    .build()
                workManager.enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.REPLACE, workRequest)
            }

            workManager.getWorkInfosByTagLiveData(WORKER_TAG)
                .observe(this, Observer { info ->
                    for (workInfo in info) {
                        if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                            val outputData = workInfo.outputData
                            val workerHour = outputData.getInt(Constants.HOUR_ALARM, 0)
                            val workerMinute = outputData.getInt(Constants.MINUTE_ALARM, 30)
                            createBackgroundAlarm(workerHour, workerMinute)
                        }
                    }
                })
        }

        binding.buttonStop.setOnClickListener {
            val alarmIntent = MyAlarmReceiver.createIntent(this, "Background exact idle alarm")
            val alarmPendingIntent =
                PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager?.cancel(alarmPendingIntent)
            val soundIntent = Intent(this, SoundService::class.java)
            stopService(soundIntent)
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        workManager.cancelUniqueWork(WORKER_TAG)
    }

    private fun createBackgroundAlarm(workerHour: Int, workerMinute: Int) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, workerHour + 24)
            set(Calendar.MINUTE, workerMinute)
            set(Calendar.SECOND, 0)
        }
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmTimeAtUTC = calendar.timeInMillis
        val alarmType = AlarmManager.RTC_WAKEUP
        val alarmIntent = MyAlarmReceiver.createIntent(this, "Background exact idle alarm")
        val alarmPendingIntent =
            PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms()
        }
        alarmManager?.setExactAndAllowWhileIdle(
            alarmType,
            alarmTimeAtUTC,
            alarmPendingIntent
        )
    }

    private fun checkPermissions() {
        if (REQUIRED_PERMISSION.all { permission ->
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            startLocation()
        } else {
            launcher.launch(REQUIRED_PERMISSION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100000)
            .build()
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    companion object {
        private val REQUIRED_PERMISSION: Array<String> = arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        private const val WORKER_TAG = "WorkerTag"
    }
}