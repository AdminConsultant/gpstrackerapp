package com.thanatos.gpstrackerapp

import android.content.Context
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseManager(context: Context) {
    private val database = Firebase.database
    private val deviceId = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )

    fun sendLocation(latitude: Double, longitude: Double, timestamp: Long) {
        val locationRef = database.getReference("devices/$deviceId/locations")
        val locationData = hashMapOf(
            "lat" to latitude,
            "lng" to longitude,
            "timestamp" to timestamp
        )
        locationRef.push().setValue(locationData)
    }

    fun updateDeviceStatus(status: String) {
        val deviceRef = database.getReference("devices/$deviceId")
        deviceRef.child("status").setValue(status)
        deviceRef.child("lastUpdate").setValue(System.currentTimeMillis())

        deviceRef.child("info").setValue(
            hashMapOf(
                "model" to android.os.Build.MODEL,
                "manufacturer" to android.os.Build.MANUFACTURER,
                "androidVersion" to android.os.Build.VERSION.RELEASE
            )
        )
    }
}