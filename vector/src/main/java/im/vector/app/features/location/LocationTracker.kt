/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.core.location.LocationListenerCompat
import timber.log.Timber
import javax.inject.Inject

class LocationTracker @Inject constructor(
        context: Context
) : LocationListenerCompat {

    private val locationManager = context.getSystemService<LocationManager>()

    interface Callback {
        fun onLocationUpdate(locationData: LocationData)
        fun onLocationProviderIsNotAvailable()
    }

    private var callback: Callback? = null

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun start(callback: Callback?) {
        Timber.d("## LocationTracker. start()")
        this.callback = callback

        if (locationManager == null) {
            callback?.onLocationProviderIsNotAvailable()
            Timber.v("## LocationTracker. LocationManager is not available")
            return
        }

        locationManager.allProviders
                .takeIf { it.isNotEmpty() }
                ?.forEach { provider ->
                    Timber.d("## LocationTracker. track location using $provider")

                    // Send last known location without waiting location updates
                    locationManager.getLastKnownLocation(provider)?.let { lastKnownLocation ->
                        Timber.d("## LocationTracker. lastKnownLocation")
                        callback?.onLocationUpdate(lastKnownLocation.toLocationData())
                    }

                    locationManager.requestLocationUpdates(
                            provider,
                            MIN_TIME_TO_UPDATE_LOCATION_MILLIS,
                            MIN_DISTANCE_TO_UPDATE_LOCATION_METERS,
                            this
                    )
                }
                ?: run {
                    callback?.onLocationProviderIsNotAvailable()
                    Timber.v("## LocationTracker. There is no location provider available")
                }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun stop() {
        Timber.d("## LocationTracker. stop()")
        locationManager?.removeUpdates(this)
        callback = null
    }

    override fun onLocationChanged(location: Location) {
        Timber.d("## LocationTracker. onLocationChanged")
        callback?.onLocationUpdate(location.toLocationData())
    }

    override fun onProviderDisabled(provider: String) {
        Timber.d("## LocationTracker. onProviderDisabled: $provider")
        callback?.onLocationProviderIsNotAvailable()
    }

    private fun Location.toLocationData(): LocationData {
        return LocationData(latitude, longitude, accuracy.toDouble())
    }
}
