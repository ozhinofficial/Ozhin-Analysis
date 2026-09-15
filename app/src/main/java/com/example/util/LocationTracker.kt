package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val placeName: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Utility for detecting and reverse-geocoding expense locations (e.g. stores, merchants, venues)
 * using Google Play Services Location and Android Geocoder.
 */
object LocationTracker {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationResult? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        val location: Location = try {
            suspendCancellableCoroutine<Location?> { continuation ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            // Fallback to last known location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }
        } catch (e: Exception) {
            null
        } ?: return@withContext null

        val addressName = reverseGeocode(context, location.latitude, location.longitude)
        LocationResult(
            placeName = addressName,
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val feature = addr.featureName
                val thoroughfare = addr.thoroughfare
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea

                val parts = listOfNotNull(
                    feature?.takeIf { it != thoroughfare && !it.all { c -> c.isDigit() } },
                    thoroughfare,
                    locality
                ).distinct()

                if (parts.isNotEmpty()) {
                    parts.joinToString(", ")
                } else {
                    addr.getAddressLine(0) ?: String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
                }
            } else {
                String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        }
    }
}
