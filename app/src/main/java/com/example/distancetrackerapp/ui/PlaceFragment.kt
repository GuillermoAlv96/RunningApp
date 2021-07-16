package com.example.distancetrackerapp.ui

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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaceFragment : BottomSheetDialogFragment() {


    private var _binding: FragmentPlaceBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaceBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null


    }
}
