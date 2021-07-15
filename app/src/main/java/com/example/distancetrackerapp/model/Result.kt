package com.example.distancetrackerapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model to share info between mapfragment and resultfragment
 */
@Parcelize
data class Result(
        var distance: String,
        var time: String
): Parcelable