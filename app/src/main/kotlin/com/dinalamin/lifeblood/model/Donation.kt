package com.dinalamin.lifeblood.model

data class Donation(
    var id: String = "",
    var donorId: String = "",
    var patient: String = "",
    var date: com.google.firebase.Timestamp? = null
)

data class TeamMember(
    var id: String = "",
    var name: String = "",
    var role: String = "",
    var image: String = ""
)
