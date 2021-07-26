package com.example.distancetrackerapp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.databinding.FragmentPlaceBinding
import com.example.distancetrackerapp.databinding.FragmentResultBinding
import com.example.distancetrackerapp.ui.result.ResultFragmentArgs
import com.example.distancetrackerapp.util.Constants
import com.example.distancetrackerapp.util.Permissions
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import java.util.*

class PlaceFragment : BottomSheetDialogFragment() {


    private lateinit var autoCompleteSupportMapFragment: AutocompleteSupportFragment

    private var _binding: FragmentPlaceBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaceBinding.inflate(inflater, container, false)

        googlePlacesOrigin()

        return binding.root
    }


    /**
     * Place service
     */
    private fun googlePlacesOrigin() {

        // Initialize the SDK
        Places.initialize(requireContext(), Constants.API_KEY)
        //Initialize the autocomplete fragment
        autoCompleteSupportMapFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment_origin) as AutocompleteSupportFragment
        //Specify the types of place data to return
        autoCompleteSupportMapFragment.setPlaceFields(
            Arrays.asList(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.NAME
            )
        )
        autoCompleteSupportMapFragment.setTypeFilter(TypeFilter.ADDRESS)
        autoCompleteSupportMapFragment.setCountries("ES")


        //Set up placeselectionlistener to handle the response.
        autoCompleteSupportMapFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            @SuppressLint("MissingPermission")
            override fun onPlaceSelected(p0: Place) {
                if (Permissions.hasLocationPermission(requireContext())) {

                    /* fusedLocationProviderClient.lastLocation.addOnSuccessListener {

                         val origin = LatLng(it.latitude, it.longitude)
                         addMarker(origin)

                     }*/

                    Snackbar.make(requireView(), "" + p0.latLng, Snackbar.LENGTH_SHORT).show()

                } else {
                    Permissions.requestLocationPermission(this@PlaceFragment)
                }

            }

            override fun onError(p0: Status) {
                Snackbar.make(requireView(), p0.statusMessage!!, Snackbar.LENGTH_LONG).show()
            }

        })
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null


    }
}
