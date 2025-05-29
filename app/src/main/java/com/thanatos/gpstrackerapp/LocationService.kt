package com.thanatos.gpstrackerapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import android.os.BatteryManager
import android.provider.Settings

class LocationService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var deviceId: String

    override fun onCreate() {
        super.onCreate()
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        startForegroundServiceWithNotification()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 60 * 1000, 0f, this)
        } else {
            stopSelf()
        }
    }

    override fun onLocationChanged(location: Location) {
        val data = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "battery" to getBatteryLevel(),
            "locationPermission" to "Concesso"
        )
        FirebaseDatabase.getInstance().getReference("devices/$deviceId").setValue(data)
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "location_service_channel"
        val channelName = "Location Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GpsTrackerApp attivo")
            .setContentText("Monitoraggio posizione in corso")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
