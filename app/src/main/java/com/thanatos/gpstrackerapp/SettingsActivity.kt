package com.thanatos.gpstrackerapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private var isFirebaseConnected = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Inizializza servizi
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = FirebaseDatabase.getInstance()

        // Setup UI iniziale
        updatePermissionStatus()
        checkFirebaseConnection()
        setupButtons()
        requestLastLocation()
    }

    private fun setupButtons() {
        // Pulsante permessi
        findViewById<Button>(R.id.btn_request_permissions).setOnClickListener {
            requestLocationPermissions()
        }

        // Pulsante Firebase
        findViewById<Button>(R.id.btn_toggle_firebase).setOnClickListener {
            toggleFirebaseConnection()
        }

        // Pulsante condivisione
        findViewById<Button>(R.id.btn_share_location).setOnClickListener {
            shareCurrentLocation()
        }
    }

    private fun requestLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    updateLocationUI(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun updateLocationUI(lat: Double, lon: Double) {
        findViewById<TextView>(R.id.txt_current_location).text =
            String.format("Lat: %.6f, Lon: %.6f", lat, lon)
    }

    private fun updatePermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val icon = findViewById<ImageView>(R.id.icon_permission_status)
        val text = findViewById<TextView>(R.id.txt_permission_status)

        if (hasPermission) {
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            text.text = "Concesso"
        } else {
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            text.text = "Non concesso"
        }
    }

    private fun checkFirebaseConnection() {
        // Implementa la logica di verifica connessione Firebase
        updateFirebaseStatus(true) // Simulazione connessione
    }

    private fun updateFirebaseStatus(connected: Boolean) {
        isFirebaseConnected = connected
        val icon = findViewById<ImageView>(R.id.icon_firebase_status)
        val text = findViewById<TextView>(R.id.txt_firebase_status)
        val button = findViewById<Button>(R.id.btn_toggle_firebase)

        if (connected) {
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            text.text = "Connesso"
            button.text = "Disconnetti da Firebase"
        } else {
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            text.text = "Disconnesso"
            button.text = "Connetti a Firebase"
        }
    }

    private fun toggleFirebaseConnection() {
        isFirebaseConnected = !isFirebaseConnected
        updateFirebaseStatus(isFirebaseConnected)
        Toast.makeText(this,
            if (isFirebaseConnected) "Connesso a Firebase" else "Disconnesso da Firebase",
            Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentLocation() {
        if (isFirebaseConnected) {
            // Implementa la logica di condivisione
            Toast.makeText(this, "Posizione condivisa", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Connetti prima a Firebase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            updatePermissionStatus()
            requestLastLocation()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}