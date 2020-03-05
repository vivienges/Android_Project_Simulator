package com.example.android_project_simulator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class BikeSimulator : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bike_simulator)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        val bikeId = intent.getStringExtra(EXTRA_BIKE_ID)
        val bikeTitle = findViewById<TextView>(R.id.bike_title)
        val bikeText = "Bike $bikeId"
        bikeTitle.text = bikeText

    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
    }

    companion object {
        const val EXTRA_BIKE_ID = "BIKE_ID"
    }
}
