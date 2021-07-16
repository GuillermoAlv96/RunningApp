package com.example.distancetrackerapp.ui.maps

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.location.Location
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentMapsBinding
import com.example.distancetrackerapp.model.Result
import com.example.distancetrackerapp.service.TrackerService
import com.example.distancetrackerapp.ui.maps.MapUtils.calculateElapsedTime
import com.example.distancetrackerapp.ui.maps.MapUtils.calculateTheDistance
import com.example.distancetrackerapp.ui.maps.MapUtils.setCameraPosition
import com.example.distancetrackerapp.util.Constants
import com.example.distancetrackerapp.util.Constants.DEFAULT_ZOOM
import com.example.distancetrackerapp.util.ExtensionFunctions.disable
import com.example.distancetrackerapp.util.ExtensionFunctions.enable
import com.example.distancetrackerapp.util.ExtensionFunctions.hide
import com.example.distancetrackerapp.util.ExtensionFunctions.show
import com.example.distancetrackerapp.util.Permissions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * On this class we initialize the map and buttons, texfields, and everything on it also their functions
 * Also ask for background permissions
 */

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMarkerClickListener, EasyPermissions.PermissionCallbacks {


    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Construct a PlacesClient




    }
    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {

        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this


        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }
        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.restarButton.setOnClickListener {
            onResetButtonClicked()
        }
        binding.placeButton.setOnClickListener {
            val directions = MapsFragmentDirections.actionMapsFragmentToPlaceFragment()
            findNavController().navigate(directions)
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }


    /**
     * After the view has been completely created it calls...
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment?.getMapAsync(this@MapsFragment)


    }


    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {


        map = googleMap!!

        // Retrieve location and camera position from saved instance state.
        getDeviceLocation()

        //initialize google maps button with my location
        if (Permissions.hasLocationPermission(requireContext())) {
            Toast.makeText(context, "Welcome", Toast.LENGTH_SHORT).show()
            map.isMyLocationEnabled = true
        } else {
            Permissions.requestLocationPermission(this)
        }

        map.setOnMyLocationButtonClickListener(this)
        //map.setOnMarkerClickListener(this)


        observeTrackerService()
        val layer = GeoJsonLayer(map, R.raw.map, context)
        layer.addLayerToMap()


    }


    /**
     *......BUTTONS.........
     */

    /**
     * What happens when click on start button
     */
    private fun onStartButtonClicked() {

        Log.d("MapsActivity", "Already Enabled")
        startCountDown()
        binding.startButton.disable()
        binding.startButton.hide()
        binding.stopButton.show()

    }

    /**
     * What happens when click on stop button
     */
    private fun onStopButtonClicked() {

        stopForegroundService()
        binding.stopButton.hide()
        binding.startButton.show()
    }

    /**
     * What happens when click on reset button
     */
    @SuppressLint("MissingPermission")
    private fun onResetButtonClicked() {
        for (polyLine in polylineList) {
            polyLine.remove()
        }
        for (marker in markerList) {
            marker.remove()
        }
        locationList.clear()
        markerList.clear()
        binding.restarButton.hide()
        binding.startButton.show()
    }

    /**
     * Google MyLocation Button
     */
    override fun onMyLocationButtonClick(): Boolean {
        lifecycleScope.launch {
            delay(2500)
            binding.startButton.show()
        }
        return false
    }


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (Permissions.hasLocationPermission(requireContext())) {

                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }


    /**
     * Once the map is ready we initialize tracking services (time,location,...)
     */
    private fun observeTrackerService() {
        TrackerService.locationList.observe(viewLifecycleOwner, {
            if (it != null) {
                locationList = it
                drawPolyline()
                followPolyline()
            }
        })
        TrackerService.started.observe(viewLifecycleOwner, {
            started.value = it
        })
        TrackerService.startTime.observe(viewLifecycleOwner, {
            startTime = it
        })
        TrackerService.stopTime.observe(viewLifecycleOwner, {
            stopTime = it
            if (stopTime != 0L) {
                showBiggerPicture()
                displayResults()
            }
        })
    }

    /**
     *Creates a countdown on the interface
     */
    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopButton.disable()
        //It will start from second 4 and it will decrease by 1
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000

                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                }
            }

            override fun onFinish() {
                sendActionCommnadToService(Constants.ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }


    /**
     * Here you can modify polyline style
     */
    private fun drawPolyline() {
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polylineList.add(polyline)
    }


    /**
     * Pollyline follows options
     */
    private fun followPolyline() {
        if (locationList.isNotEmpty()) {
            map.animateCamera(
                (CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(
                        locationList.last()
                    )
                )), 1000, null
            )
        }
    }


    private fun stopForegroundService() {
        binding.startButton.disable()
        sendActionCommandToService(Constants.ACTION_SERVICE_STOP)
    }

    /**
     * Channel to send info to Trackerserviceclass
     */
    private fun sendActionCommandToService(action: String) {
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    /**
     * will send a specific action to our tracking service and start our service so it should be called in the onFinish
     */
    private fun sendActionCommnadToService(action: String) {
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(), 100
            ), 2000, null
        )
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    /**
     * Adds markers at the end of the activity
     */
    private fun addMarker(position: LatLng) {
        val marker = map.addMarker(MarkerOptions().position(position))
        markerList.add(marker)
    }

    /**
     * Calls result fragment
     * Displays results
     */
    private fun displayResults() {
        val result = Result(
            calculateTheDistance(locationList),
            calculateElapsedTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.startButton.apply {
                hide()
                enable()
            }
            binding.stopButton.hide()
            binding.restarButton.show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    /**
     * When user accepts permissions
     */
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        TODO("Not yet implemented")
    }


    /**
     * What happens when user doesnt accept permissions
     */
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            Permissions.requestLocationPermission(this)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null

    }

    override fun onMarkerClick(p0: Marker): Boolean {
        TODO("Not yet implemented")
    }


}


