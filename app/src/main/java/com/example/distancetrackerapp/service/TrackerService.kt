package com.example.distancetrackerapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetrackerapp.ui.maps.MapUtils
import com.example.distancetrackerapp.util.Constants
import com.example.distancetrackerapp.util.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetrackerapp.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.distancetrackerapp.util.Constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


//@AndroidEntryPoint genera un componente individual de Hilt para cada clase de Android de tu proyecto. Estos componentes pueden recibir dependencias de sus respectivas clases superiores
@AndroidEntryPoint

/**
 * Class where we group all services like notifications,locations updates,etc...
 */
class TrackerService : LifecycleService() {

    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val started = MutableLiveData<Boolean>()
        val startTime = MutableLiveData<Long>()
        val stopTime = MutableLiveData<Long>()

        val locationList = MutableLiveData<MutableList<LatLng>>()
    }


    private fun setInitialValues() {
        started.postValue(false)
        startTime.postValue(0L)
        stopTime.postValue(0L)

        locationList.postValue(mutableListOf())
    }


    private val locationCallback = object : LocationCallback() {
        /**
         * After acquiring the location it will update location and notification
         */
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            result?.locations?.let { locations ->
                for (location in locations) {
                    updateLocationList(location)
                    updateNotificationPeriodically()
                }
            }
        }
    }

    /**
     * Update location
     */
    private fun updateLocationList(location: Location) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        locationList.value?.apply {
            add(newLatLng)
            locationList.postValue(this)
        }
    }


    override fun onCreate() {
        setInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }


    /**
     * Function that will send info to start foregroundservice or stop it
     * and call location updates if its on going
     */

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_SERVICE_START -> {
                    started.postValue(true)
                    startForegroundService()
                    startLocationUpdates()
                }
                Constants.ACTION_SERVICE_STOP -> {
                    started.postValue(false)
                    stopForegroundService()
                }
                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }


    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification.build())
    }

    /**
     * Specify the time ratio in which we give location info
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        startTime.postValue(System.currentTimeMillis())
    }


    private fun stopForegroundService() {
        removeLocationUpdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun updateNotificationPeriodically() {
        notification.apply {
            setContentTitle("Distance Travelled")
            setContentText(locationList.value?.let { MapUtils.calculateTheDistance(it) } + "km")
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


}








