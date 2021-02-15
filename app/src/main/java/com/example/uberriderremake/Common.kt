package com.example.uberriderremake

object Common {
    fun buildWelcomeMessage(): CharSequence? {
        return "Welcome ${currentRider?.firstName} ${currentRider?.lastName} "
    }

    var currentRider : RiderInfoModel? = null
}