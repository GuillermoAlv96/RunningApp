package com.example.distancetrackerapp.bindingAdapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.databinding.BindingAdapter

/**
 * This class will make changes on the map interface after reopening the app that is still running
 */
class MapsBindingAdapter {


    /**
     * this function will make our text invisible and our stop button visible
     * after reopening the app while its still running
     */
    companion object {
        @BindingAdapter("observeTracking")
        @JvmStatic
        fun observeTracking(view: View, started: Boolean) {
            if (started && view is Button) {
                view.visibility = View.VISIBLE
            } else if (started && view is TextView) {
                view.visibility = View.INVISIBLE
            }
        }
    }
}