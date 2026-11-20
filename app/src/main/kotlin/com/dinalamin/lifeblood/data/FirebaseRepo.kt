package com.dinalamin.lifeblood.data

import com.dinalamin.lifeblood.model.BloodRequest
import com.dinalamin.lifeblood.model.Donation
import com.dinalamin.lifeblood.model.TeamMember
import com.dinalamin.lifeblood.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Single access point for Firebase Auth + Firestore, mirroring the
 * collections used by the original web app: users, requests, donations, team.
 */
object FirebaseRepo {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUid: String? get() = auth.currentUser?.uid

    // ---------- AUTH ----------

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(user: User, password: String) {
        val result = auth.createUserWithEmailAndPassword(user.email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("No UID returned")
        user.uid = uid
        val data = hashMapOf(
            "uid" to uid,
            "name" to user.name,
            "email" to user.email,
            "phone" to user.phone,
            "blood" to user.blood,
            "role" to user.role,
            "district" to user.district,
            "available" to true,
            "donationCount" to 0L,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users").document(uid).set(data).await()
    }

    fun logout() = auth.signOut()

    suspend fun fetchCurrentUser(): User? {
        val uid = currentUid ?: return null
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(User::class.java)
    }

    suspend fun setAvailability(uid: String, available: Boolean) {
        db.collection("users").document(uid).update("available", available).await()
    }

    suspend fun updateProfile(uid: String, name: String, phone: String, district: String) {
        db.collection("users").document(uid)
            .update(mapOf("name" to name, "phone" to phone, "district" to district))
            .await()
    }

    /** Public counters shown on the home screen: (active requests, donors, fulfilled requests). */
    suspend fun fetchHomeStats(): Triple<Int, Int, Int> {
        val active = db.collection("requests").whereEqualTo("status", "active").get().await().size()
        val donors = db.collection("users").whereEqualTo("role", "donor").get().await().size()
        val fulfilled = db.collection("requests").whereEqualTo("status", "fulfilled").get().await().size()
        return Triple(active, donors, fulfilled)
    }

    // ---------- DONORS / USERS ----------

    suspend fun searchDonors(blood: String?, district: String?): List<User> {
        var q: Query = db.collection("users").whereEqualTo("role", "donor")
        if (!blood.isNullOrBlank()) q = q.whereEqualTo("blood", blood)
        if (!district.isNullOrBlank()) q = q.whereEqualTo("district", district)
        val snap = q.get().await()
        return snap.documents.mapNotNull { it.toObject(User::class.java) }
    }

    // ---------- REQUESTS ----------

    suspend fun postRequest(request: BloodRequest) {
        val data = hashMapOf(
            "patient" to request.patient,
            "problem" to request.problem,
            "hospital" to request.hospital,
            "blood" to request.blood,
            "date" to request.date,
            "location" to request.location,
            "phone" to request.phone,
            "note" to request.note,
            "isEmergency" to request.isEmergency,
            "status" to "active",
            "createdBy" to (currentUid ?: ""),
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("requests").add(data).await()
    }

    suspend fun updateRequest(request: BloodRequest) {
        val data = hashMapOf<String, Any>(
            "patient" to request.patient,
            "problem" to request.problem,
            "hospital" to request.hospital,
            "blood" to request.blood,
            "date" to request.date,
            "location" to request.location,
            "phone" to request.phone,
            "note" to request.note,
            "isEmergency" to request.isEmergency
        )
        db.collection("requests").document(request.id).update(data).await()
    }

    suspend fun deleteRequest(id: String) {
        db.collection("requests").document(id).delete().await()
    }

    suspend fun fetchActiveRequests(): List<BloodRequest> {
        val snap = db.collection("requests").whereEqualTo("status", "active").get().await()
        return snap.documents.mapNotNull { d ->
            d.toObject(BloodRequest::class.java)?.apply { id = d.id }
        }.sortedWith(
            compareByDescending<BloodRequest> { it.isEmergency }
                .thenByDescending { (it.createdAt as? com.google.firebase.Timestamp)?.seconds ?: 0L }
        )
    }

    suspend fun fetchMyRequests(): List<BloodRequest> {
        val uid = currentUid ?: return emptyList()
        val snap = db.collection("requests").whereEqualTo("createdBy", uid).get().await()
        return snap.documents.mapNotNull { d ->
            d.toObject(BloodRequest::class.java)?.apply { id = d.id }
        }.sortedByDescending { (it.createdAt as? com.google.firebase.Timestamp)?.seconds ?: 0L }
    }

    // ---------- DONATIONS ----------

    suspend fun recordDonation(donorPhone: String, patient: String) {
        val donorSnap = db.collection("users").whereEqualTo("phone", donorPhone).get().await()
        if (donorSnap.isEmpty) throw IllegalStateException("No donor found with that phone number")
        val donorDoc = donorSnap.documents.first()
        val donorId = donorDoc.id
        db.collection("donations").add(
            hashMapOf(
                "donorId" to donorId,
                "patient" to patient,
                "date" to com.google.firebase.Timestamp.now()
            )
        ).await()
        db.collection("users").document(donorId)
            .update("donationCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
    }

    suspend fun fetchMyDonationHistory(): List<Donation> {
        val uid = currentUid ?: return emptyList()
        val snap = db.collection("donations").whereEqualTo("donorId", uid).get().await()
        return snap.documents.mapNotNull { d -> d.toObject(Donation::class.java)?.apply { id = d.id } }
            .sortedByDescending { it.date?.seconds ?: 0 }
    }

    // ---------- TEAM ----------

    suspend fun fetchTeam(): List<TeamMember> {
        val snap = db.collection("team").get().await()
        return snap.documents.mapNotNull { d -> d.toObject(TeamMember::class.java)?.apply { id = d.id } }
    }

}
