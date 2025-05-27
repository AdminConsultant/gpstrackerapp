package com.thanatos.gpstrackerapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.thanatos.gpstrackerapp.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var firebaseConnected = true
    private var updateIntervalMillis = 15 * 60 * 1000L // Default: 15 minuti
    private val handler = Handler(Looper.getMainLooper())
    private val deviceId by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updateUI() }

    private val updateTask = object : Runnable {
        override fun run() {
            if (firebaseConnected && hasLocationPermission()) {
                sendLocationToFirebase()
            }
            handler.postDelayed(this, updateIntervalMillis)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsIfNecessary()

        binding.btnPermessi.setOnClickListener {
            requestPermissionsIfNecessary()
        }

        binding.btnConnetti.setOnClickListener {
            firebaseConnected = true
            updateUI()
        }

        binding.btnDisconnetti.setOnClickListener {
            firebaseConnected = false
            updateUI()
        }

        binding.btnCondividi.setOnClickListener {
            if (hasLocationPermission()) {
                sendLocationToFirebase()
            } else {
                Toast.makeText(this, "Permessi posizione non concessi", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnImpostazioni.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        }

        binding.btnImpostaIntervallo.setOnClickListener {
            val minuti = binding.etIntervallo.text.toString().toLongOrNull()
            if (minuti != null && minuti > 0) {
                updateIntervalMillis = minuti * 60 * 1000
                handler.removeCallbacks(updateTask)
                handler.post(updateTask)
                Toast.makeText(this, "Intervallo impostato a $minuti minuti", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Valore non valido", Toast.LENGTH_SHORT).show()
            }
        }

        startService(Intent(this, LocationService::class.java))
        updateUI()
        handler.post(updateTask)
    }

    private fun requestPermissionsIfNecessary() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendLocationToFirebase() {
        val location = getLastKnownLocation()
        if (location != null) {
            val data = mapOf(
                "lat" to location.latitude,
                "lng" to location.longitude,
                "timestamp" to System.currentTimeMillis(),
                "battery" to getBatteryLevel(),
                "locationPermission" to if (hasLocationPermission()) "Concesso" else "Negato"
            )

            FirebaseDatabase.getInstance().getReference("devices/$deviceId").setValue(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Posizione inviata correttamente", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore invio posizione", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (hasLocationPermission()) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } else null
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun updateUI() {
        val location = getLastKnownLocation()
        binding.txtDeviceId.text = "ID Dispositivo: $deviceId"
        binding.txtGps.text = if (location != null) {
            "Posizione: ${location.latitude}, ${location.longitude}"
        } else {
            "Posizione non disponibile"
        }
        binding.txtPermessi.text = "Permesso Posizione: " +
                if (hasLocationPermission()) "Concesso" else "Negato"
        binding.txtFirebase.text = "Firebase: " +
                if (firebaseConnected) "Connesso" else "Disconnesso"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTask)
    }
}
