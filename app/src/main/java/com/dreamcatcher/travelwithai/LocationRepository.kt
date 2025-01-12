package com.dreamcatcher.travelwithai

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

class LocationRepository(context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(location) {}
                } else {
                    continuation.resumeWithException(Exception("Location not found"))
                }
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    // For testing only
    suspend fun getFakeLocation(): Location? {

        val edinburghLocation = Location("provider").apply {
            latitude = 55.9701 // Edinburgh, Leith
            longitude = -3.1894
        }

        val wroclawLocation = Location("provider").apply {
            latitude = 51.1080 // Wroclaw, Hiszpanska street
            longitude = 17.0310
        }

        val bangkokLocation = Location("provider").apply {
            latitude = 13.7367 // Bangkok, SuunCity Condo
            longitude = 100.5339
        }

        val location = edinburghLocation
        return suspendCancellableCoroutine { continuation ->
            continuation.resume(location) {}
        }
    }
}
