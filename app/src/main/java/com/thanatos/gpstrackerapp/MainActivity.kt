package com.thanatos.gpstrackerapp

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationService.LocationUpdateListener {

    // UI Components
    private lateinit var statusText: TextView
    private lateinit var startServiceButton: Button
    private lateinit var btnPermessi: Button
    private lateinit var txtGps: TextView
    private lateinit var txtPermessi: TextView
    private lateinit var txtFirebase: TextView
    private lateinit var etIntervallo: EditText
    private lateinit var btnImpostaIntervallo: Button
    private lateinit var btnConnetti: Button
    private lateinit var btnDisconnetti: Button
    private lateinit var mapView: MapView

    // State variables
    private var googleMap: GoogleMap? = null
    private var updateInterval: Long = 5000 // Default 5 seconds
    private lateinit var firebaseManager: FirebaseManager
    private var locationService: LocationService? = null

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService().apply {
                addLocationListener(this@MainActivity)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService?.removeLocationListener(this@MainActivity)
            locationService = null
        }
    }

    // Permissions handling
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            plus(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            plus(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            updateStatusText()
            enableMyLocation()
            Toast.makeText(this, "Permessi concessi", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Alcuni permessi necessari non sono stati concessi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            MapsInitializer.initialize(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Errore inizializzazione mappe: ${e.message}", Toast.LENGTH_LONG).show()
        }

        initViews()
        setupMap(savedInstanceState)
        setupFirebase()
        setupButtons()
        checkPermissions()
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        startServiceButton = findViewById(R.id.startServiceButton)
        btnPermessi = findViewById(R.id.btnPermessi)
        txtGps = findViewById(R.id.txtGps)
        txtPermessi = findViewById(R.id.txtPermessi)
        txtFirebase = findViewById(R.id.txtFirebase)
        etIntervallo = findViewById(R.id.etIntervallo)
        btnImpostaIntervallo = findViewById(R.id.btnImpostaIntervallo)
        btnConnetti = findViewById(R.id.btnConnetti)
        btnDisconnetti = findViewById(R.id.btnDisconnetti)
        mapView = findViewById(R.id.mapView)
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun setupFirebase() {
        firebaseManager = FirebaseManager(this)
        updateFirebaseStatus("Disconnesso")
    }

    private fun setupButtons() {
        btnPermessi.setOnClickListener { requestPermissions() }

        startServiceButton.setOnClickListener {
            if (hasPermissions()) {
                startLocationService()
            } else {
                requestPermissions()
            }
        }

        btnImpostaIntervallo.setOnClickListener {
            updateInterval = (etIntervallo.text.toString().toLongOrNull() ?: 1L) * 60 * 1000
            locationService?.updateInterval(updateInterval)
            Toast.makeText(this, "Intervallo aggiornato", Toast.LENGTH_SHORT).show()
        }

        btnConnetti.setOnClickListener {
            updateFirebaseStatus("Connesso")
            Toast.makeText(this, "Connesso a Firebase", Toast.LENGTH_SHORT).show()
        }

        btnDisconnetti.setOnClickListener {
            updateFirebaseStatus("Disconnesso")
            Toast.makeText(this, "Disconnesso da Firebase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFirebaseStatus(status: String) {
        txtFirebase.text = "Firebase: $status"
        firebaseManager.updateDeviceStatus(status.lowercase())
    }

    private fun checkPermissions() {
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            updateStatusText()
        }
    }

    private fun requestPermissions() {
        if (shouldShowRationale()) {
            Toast.makeText(this,
                "I permessi di localizzazione sono necessari per tracciare la posizione",
                Toast.LENGTH_LONG).show()
        }
        requestPermissionLauncher.launch(permissions)
    }

    private fun shouldShowRationale() = permissions.any {
        ActivityCompat.shouldShowRequestPermissionRationale(this, it)
    }

    private fun hasPermissions() = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateStatusText() {
        val status = if (hasPermissions()) "Concessi" else "Non concessi"
        statusText.text = "Stato permessi: $status"
        txtPermessi.text = "Permessi: $status"
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    private fun startLocationService() {
        Intent(this, LocationService::class.java).apply {
            putExtra("interval", updateInterval)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(this)
            } else {
                startService(this)
            }
            bindService(this, serviceConnection, BIND_AUTO_CREATE)
        }
        txtGps.text = "Stato: Attivo"
        Toast.makeText(this, "Servizio avviato", Toast.LENGTH_SHORT).show()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            uiSettings.isZoomControlsEnabled = true
            if (hasPermissions()) enableMyLocation()
        }
    }

    override fun onLocationUpdated(location: Location) {
        runOnUiThread {
            googleMap?.apply {
                val latLng = LatLng(location.latitude, location.longitude)
                animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                clear()
                addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Posizione corrente")
                )

                if (txtFirebase.text == "Firebase: Connesso") {
                    firebaseManager.sendLocation(
                        location.latitude,
                        location.longitude,
                        System.currentTimeMillis()
                    )
                }
            }
        }
    }

    // MapView lifecycle
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}