package com.example.android_project_simulator

import com.google.firebase.firestore.GeoPoint

data class Bike (
    var charge: Int,
    var available: Boolean,
    var locked: Boolean,
    var reserved: Boolean,
    var current_user: String,
    var position: GeoPoint
)

{
    constructor() : this(0, true, true, false,"", GeoPoint(0.0, 0.0))
}