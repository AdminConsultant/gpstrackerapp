package com.thanatos.gpstrackerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.thanatos.gpstrackerapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatInterval = 30000L // 30 seconds

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            updateDeviceStatus()
            handler.postDelayed(this, heartbeatInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setContentView(R.layout.activity_main) // Verifica che il layout usi componenti compatibili


        database = FirebaseDatabase.getInstance()

        if (checkPermissions()) {
            initializeApp()
        } else {
            requestPermissions()
        }
    }

    private fun initializeApp() {
        startHeartbeat()
        startLocationService()
    }

    private fun updateDeviceStatus() {
        val deviceId = generateDeviceId() // Rinominato il metodo
        val updates = mapOf(
            "lastUpdate" to ServerValue.TIMESTAMP,
            "status" to "online",
            "metadata/lastHeartbeat" to System.currentTimeMillis()
        )

        database.getReference("devices/$deviceId").updateChildren(updates)
            .addOnFailureListener { e ->
                showToast("Update failed: ${e.message}")
            }
    }

    // Rinominato da getDeviceId() a generateDeviceId()
    private fun generateDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeApp()
        } else {
            showToast("Location permission required")
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun startHeartbeat() {
        handler.post(heartbeatRunnable)
    }

    private fun stopHeartbeat() {
        handler.removeCallbacks(heartbeatRunnable)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        startHeartbeat()
    }

    override fun onPause() {
        super.onPause()
        stopHeartbeat()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}