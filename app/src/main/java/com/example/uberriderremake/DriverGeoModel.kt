package com.example.uberriderremake

import com.firebase.geofire.GeoLocation

data class DriverGeoModel(
    var key:String = "",
    var geoLocation: GeoLocation? = null,
    var driverInfoModel: DriverInfoModel? = null
){
    constructor(key: String,geoLocation: GeoLocation?) : this()
}
