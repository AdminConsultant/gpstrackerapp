package com.thanatos.gpstrackerapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LocationService : Service(), LocationListener {

    interface LocationUpdateListener {
        fun onLocationUpdated(location: Location)
    }

    private lateinit var locationManager: LocationManager
    private lateinit var firebaseManager: FirebaseManager
    private val channelId = "LocationServiceChannel"
    private val listeners = mutableListOf<LocationUpdateListener>()
    private var updateInterval: Long = 5000

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    override fun onCreate() {
        super.onCreate()
        firebaseManager = FirebaseManager(this)
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateInterval = intent?.getLongExtra("interval", 5000) ?: 5000
        startLocationUpdates()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracciamento Posizione")
            .setContentText("Servizio in esecuzione")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                updateInterval,
                10f,
                this
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun updateInterval(newInterval: Long) {
        updateInterval = newInterval
        locationManager.removeUpdates(this)
        startLocationUpdates()
    }

    override fun onLocationChanged(location: Location) {
        listeners.forEach { it.onLocationUpdated(location) }
        firebaseManager.updateDeviceStatus("active")
    }

    fun addLocationListener(listener: LocationUpdateListener) {
        listeners.add(listener)
    }

    fun removeLocationListener(listener: LocationUpdateListener) {
        listeners.remove(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
        listeners.clear()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}