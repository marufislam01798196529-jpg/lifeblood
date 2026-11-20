package com.dinalamin.lifeblood.model

import com.google.firebase.firestore.PropertyName

data class BloodRequest(
    var id: String = "",
    var patient: String = "",
    var problem: String = "",
    var hospital: String = "",
    var blood: String = "",
    var date: String = "",
    var location: String = "",
    var phone: String = "",
    var note: String = "",
    // Without the annotation Firestore looks for a field named "emergency",
    // so the stored "isEmergency" flag was never read back.
    @get:PropertyName("isEmergency") @set:PropertyName("isEmergency")
    var isEmergency: Boolean = false,
    var status: String = "active",
    var createdBy: String = "",
    var createdAt: Any? = null
)
