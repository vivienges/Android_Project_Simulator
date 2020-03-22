package com.example.android_project_simulator

import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.w3c.dom.Document
import java.util.*
import kotlin.concurrent.timerTask

class BikeSimulator : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var bike: Bike
    private lateinit var bikeId: String
    private lateinit var historyRef: DocumentReference
    private var regularTimer = Timer()
    private var regularTimerRunning = false
    private var pauseTimer = Timer()
    private var pauseTimerRunning = false

    private var currentUser = ""
    private var reserved = false
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bike_simulator)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        bikeId = intent.getStringExtra(EXTRA_BIKE_ID)!!

        val chargeButton = findViewById<Button>(R.id.charge_button)
        val consumeButton = findViewById<Button>(R.id.consume_button)
        val showCodeButton = findViewById<Button>(R.id.code_button)

        showCodeButton.setOnClickListener {
            Toast
                .makeText(
                    this,
                    "Enter the code $bikeId in eezy to unlock this bike!",
                    Toast.LENGTH_LONG
                ).show()
        }

        chargeButton.setOnClickListener {
            if (bike.charge + 1 <= 100) {
                bike.charge += 1
                db.collection("bikes").document(bikeId)
                    .update("charge", bike.charge)
            }
        }

        consumeButton.setOnClickListener {
            if (bike.charge - 1 >= 0) {
                bike.charge -= 1
                db.collection("bikes").document(bikeId)
                    .update("charge", bike.charge)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val bikeTitle = findViewById<TextView>(R.id.bike_title)
        val batteryLabel = findViewById<TextView>(R.id.battery_label)
        val lockLabel = findViewById<TextView>(R.id.lock_label)

        val bikeText = "Bike $bikeId"
        bikeTitle.text = bikeText

        db.collection("bikes").document(bikeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ERROR", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    bike = snapshot.toObject(Bike::class.java) as Bike
                    reserved = bike.reserved
                    if (bike.current_user != this.currentUser) {
                        this.currentUser = bike.current_user
                        if (this.currentUser == "") {
                            bikeTitle.text = bikeText
                        } else {
                            val bikeUserText = "Bike $bikeId (${this.currentUser})"
                            bikeTitle.text = bikeUserText
                        }
                    }
                    if (this.currentUser != "" && !bike.locked && !bike.reserved) {
                        db.collection("users")
                            .whereEqualTo("email", this.currentUser)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (documents != null) {
                                    for (document in documents) {
                                        val histories = document.data["history"] as List<*>
                                        if (histories.isNotEmpty()) {
                                            historyRef = histories.last() as DocumentReference
                                        }
                                    }
                                } else {
                                    Log.d("ERROR", "No user document for email ${this.currentUser}")
                                }
                                if (pauseTimerRunning) {
                                    pauseTimer.cancel()
                                    pauseTimer.purge()
                                    pauseTimerRunning = false
                                }
                                if (!regularTimerRunning) {
                                    regularTimer = Timer()
                                    regularTimer.schedule(timerTask {
                                        historyRef
                                            .get()
                                            .addOnSuccessListener { result ->
                                                val currentPrice = result["total_price"] as Double
                                                historyRef
                                                    .update("total_price", currentPrice + 0.3)
                                            }
                                    }, ONE_MINUTE.toLong(), ONE_MINUTE.toLong())
                                    regularTimerRunning = true
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d("ERROR", "get failed with ", exception)
                            }
                    }
                    if (this.currentUser != "" && bike.locked && !bike.reserved) {
                        if (regularTimerRunning) {
                            regularTimer.cancel()
                            regularTimer.purge()
                            regularTimerRunning = false
                        }
                        if (!pauseTimerRunning) {
                            pauseTimer = Timer()
                            pauseTimer.schedule(timerTask {
                                historyRef
                                    .get()
                                    .addOnSuccessListener { result ->
                                        val currentPrice = result["total_price"] as Double
                                        historyRef
                                            .update("total_price", currentPrice + 0.1)
                                    }
                            }, ONE_MINUTE.toLong(), ONE_MINUTE.toLong())
                            pauseTimerRunning = true
                        }
                    } else {
                        if (regularTimerRunning) {
                            regularTimer.cancel()
                            regularTimer.purge()
                            regularTimerRunning = false
                        }
                        if (pauseTimerRunning) {
                            pauseTimer.cancel()
                            pauseTimer.purge()
                            pauseTimerRunning = false
                        }
                    }
                    val chargeText = bike.charge.toString() + "%"
                    batteryLabel.text = chargeText
                    if (bike.locked)
                        lockLabel.text = "locked"
                    else
                        lockLabel.text = "unlocked"

                    val position = LatLng(bike.position.latitude, bike.position.longitude)
                    mMap.addMarker(MarkerOptions().position(position).title("Bike $bikeId"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 18F))
                }
            }
        mMap.setOnMapClickListener { LatLng ->
            if (this.currentUser != "" && !reserved) {
                val oldLocation = LatLng(bike.position.latitude, bike.position.longitude).toLocation()
                val newLocation = LatLng.toLocation()
                //Get distance in meters -> Divide by 1000 for km
                val distance = oldLocation.distanceTo(newLocation) / 1000
                historyRef
                    .get()
                    .addOnSuccessListener { result ->
                        val currentDistance = result["total_km"] as Double
                        historyRef
                            .update(
                                mapOf(
                                "total_km" to currentDistance + distance,
                                "route" to FieldValue.arrayUnion(GeoPoint(LatLng.latitude, LatLng.longitude))
                                )
                            )
                    }
            }

            db.collection("bikes").document(bikeId)
                .update("position", GeoPoint(LatLng.latitude, LatLng.longitude))
        }
    }

    private fun LatLng.toLocation() = Location(LocationManager.GPS_PROVIDER).also {
        it.latitude = latitude
        it.longitude = longitude
    }

    companion object {
        const val EXTRA_BIKE_ID = "BIKE_ID"
        const val ONE_MINUTE = 5 * 1000
    }
}
