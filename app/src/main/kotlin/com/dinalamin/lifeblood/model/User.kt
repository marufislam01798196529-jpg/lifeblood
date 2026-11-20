package com.dinalamin.lifeblood.model

data class User(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var blood: String = "",
    var role: String = "donor", // donor | receiver
    var district: String = "",
    var available: Boolean = true,
    var donationCount: Long = 0,
    var createdAt: Any? = null
)
