package com.example.uberriderremake

interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel : DriverGeoModel)
}

interface FirebaseFailedListener{
    fun onFirebaseFailed(message : String)
}