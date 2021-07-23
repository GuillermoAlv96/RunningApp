package com.example.distancetrackerapp.ui.maps

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentMapsBinding
import com.example.distancetrackerapp.model.PlaceLocation
import com.example.distancetrackerapp.model.Result
import com.example.distancetrackerapp.service.TrackerService
import com.example.distancetrackerapp.ui.maps.MapUtils.calculateElapsedTime
import com.example.distancetrackerapp.ui.maps.MapUtils.calculateTheDistance
import com.example.distancetrackerapp.ui.maps.MapUtils.setCameraPosition
import com.example.distancetrackerapp.util.Constants
import com.example.distancetrackerapp.util.Constants.API_KEY
import com.example.distancetrackerapp.util.Constants.DEFAULT_ZOOM
import com.example.distancetrackerapp.util.ExtensionFunctions.disable
import com.example.distancetrackerapp.util.ExtensionFunctions.hide
import com.example.distancetrackerapp.util.ExtensionFunctions.show
import com.example.distancetrackerapp.util.Permissions
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.util.HttpUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import java.util.*


/**
 * On this class we initialize the map and buttons, texfields, and everything on it also their functions
 * Also ask for background permissions
 */

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMarkerClickListener, EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    private lateinit var autoCompleteSupportMapFragment_destination: AutocompleteSupportFragment
    private lateinit var autoCompleteSupportMapFragment_origin: AutocompleteSupportFragment

    //Origin and Destination LatLng
    private lateinit var destinationPlace: PlaceLocation
    private lateinit var originPlace: PlaceLocation

    val started = MutableLiveData(false)

    private var startTime = 0L
    private var stopTime = 0L

    //Location
    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()


    // Construct a FusedLocationProviderClient.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    //probando autocomplete maps
    private var something: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the SDK
        Places.initialize(requireContext(), API_KEY)

    }

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {

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


    /* private fun getJsonFromURL(){

         //Directions API request
         val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?" +
                 "origin=${originPlace.id}&destination=${destinationPlace.id}" +
                 "&key=$API_KEY&mode=driving"
         try {
             //Get the API response.
             response = urllib.request.urlopen(url)

         }

     }*/

    private fun prueba() {

        val path: MutableList<List<LatLng>> = ArrayList()

        //Directions API request
        val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=place_id:${originPlace.id}&destination=place_id:${destinationPlace.id}" +
                "&key=$API_KEY&mode=driving"

        val directionsRequest = object :
            StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> { response ->
                val jsonResponse = JSONObject(response)
                // Get routes
                val routes = jsonResponse.getJSONArray("routes")
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")

                for (i in 0 until steps.length()) {
                    val points =
                        steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                    path.add(PolyUtil.decode(points))
                }
                for (i in 0 until path.size) {
                    map.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
                }
            }, Response.ErrorListener { _ ->
                Log.d("JSON", "JSON RESPONSE ERROR")
            }) {}
        val requestQueue = Volley.newRequestQueue(requireContext())
        requestQueue.add(directionsRequest)


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

        if (Permissions.hasLocationPermission(requireContext())) {

            map = googleMap!!

            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.uber_style
                )
            )

            map.isMyLocationEnabled = true
            map.setOnMyLocationButtonClickListener(this)

            // Retrieve location and camera position from saved instance state.
            getDeviceCurrentLocation()

            observeTrackerService()

            // val layer = GeoJsonLayer(map, R.raw.map, context)
            // layer.addLayerToMap()

            googlePlacesDestination()

        } else {
            Permissions.requestLocationPermission(this)
        }

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

        return false
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceCurrentLocation() {

        try {
            if (Permissions.hasLocationPermission(requireContext())) {

                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map.moveCamera(
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
            } else {
                Permissions.requestBackgroundLocationPermission(this)
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
     * Place service
     */
    private fun googlePlacesDestination() {

        //Initialize the autocomplete fragment
        autoCompleteSupportMapFragment_destination =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment_destination) as AutocompleteSupportFragment

        autoCompleteSupportMapFragment_origin =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment_origin) as AutocompleteSupportFragment

        //Set autocomplete filters
        autoCompletePlaceFilters(autoCompleteSupportMapFragment_destination)
        autoCompletePlaceFilters(autoCompleteSupportMapFragment_origin)

        //Set up placeselectionlistener to handle the response.
        autoCompleteSupportMapFragment_destination.setOnPlaceSelectedListener(object :
            PlaceSelectionListener {
            @SuppressLint("MissingPermission")
            override fun onPlaceSelected(p0: Place) {
                if (Permissions.hasLocationPermission(requireContext())) {

                    val id = p0.id ?: return
                    val name = p0.name ?: return
                    val latLng = p0.latLng ?: return

                    destinationPlace = PlaceLocation(id, name, latLng.latitude, latLng.longitude)

                    //After select location show placesoriginfragment
                    binding.autocompleteFragmentOrigin.show()

                    Snackbar.make(requireView(), "" + p0.latLng, Snackbar.LENGTH_SHORT).show()

                    if (something) {
                        generateRoute()
                    }

                } else {
                    Permissions.requestLocationPermission(requireParentFragment())
                }
            }

            override fun onError(p0: Status) {
                Snackbar.make(requireView(), p0.statusMessage!!, Snackbar.LENGTH_LONG).show()
            }


        })

        //Set up placeselectionlistener to handle the response.
        autoCompleteSupportMapFragment_origin.setOnPlaceSelectedListener(object :
            PlaceSelectionListener {

            override fun onPlaceSelected(p0: Place) {

                val id = p0.id ?: return
                val name = p0.name ?: return
                val latLng = p0.latLng ?: return
                originPlace = PlaceLocation(id, name, latLng.latitude, latLng.longitude)
                something = true
                generateRoute()

            }

            override fun onError(p0: Status) {
                Snackbar.make(requireView(), p0.statusMessage!!, Snackbar.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun autoCompletePlaceFilters(p0: AutocompleteSupportFragment) {

        p0.setTypeFilter(TypeFilter.ADDRESS)
        p0.setCountries("ES")
        //Specify the types of place data to return
        p0.setPlaceFields(
            Arrays.asList(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.NAME
            )
        )
    }


    private fun generateRoute() {


        val origin: LatLng = LatLng(originPlace.latitude, originPlace.longitude)
        val destination: LatLng = LatLng(destinationPlace.latitude, destinationPlace.longitude)
        locationList.add(origin)
        locationList.add(destination)
        showBiggerPicture()
        drawPolyline()
        prueba()

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
                color(Color.BLACK)
                jointType(JointType.ROUND)
                startCap(SquareCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        val greyPolyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.GRAY)
                jointType(JointType.ROUND)
                startCap(SquareCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )

        //Animator
        val valueAnimator = ValueAnimator.ofInt(0, 100)
        valueAnimator.repeatCount = ValueAnimator.INFINITE
        valueAnimator.duration = 1100
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener {
            val points = greyPolyline.points
            val size = points.size
            val percentValue = it.animatedValue.toString().toInt()
            val newpoints = (size * (percentValue / 100.0f)).toInt()
            val p = points.subList(0, newpoints)
            polyline.points
        }
        valueAnimator.start()

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
                bounds.build(), 30
            ), 2000, null
        )
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
        onMapReady(map)
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




