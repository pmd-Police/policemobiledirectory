package com.example.policemobiledirectory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.example.policemobiledirectory.model.Employee
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(
    tableName = "pending_registrations",
    indices = [
        Index(value = ["kgid"], unique = true),
        Index(value = ["status"])
    ]
)
data class PendingRegistrationEntity(

    @PrimaryKey(autoGenerate = true)
    val roomId: Long? = null,

    @DocumentId
    var firestoreId: String? = null,

    val kgid: String = "",
    val name: String = "",
    val email: String = "",

    val mobile1: String = "",
    val mobile2: String? = null,

    val district: String = "",
    val station: String = "",
    val rank: String = "",
    val metalNumber: String? = null,

    val bloodGroup: String = "",

    var pin: String = "",
    var photoUrl: String? = null,

    var status: String = "pending",
    var rejectionReason: String? = null,

    @get:Exclude
    var isApproved: Boolean = false,

    @ServerTimestamp
    var createdAt: Date? = null,

    @ServerTimestamp
    var updatedAt: Date? = null,

    val firebaseUid: String = "",
    val photoUrlFromGoogle: String? = null
)

/* ---------------------------
   Convert pending → Employee
---------------------------- */
fun PendingRegistrationEntity.toEmployee(
    finalPhotoUrl: String? = this.photoUrl
): Employee {
    return Employee(
        kgid = kgid.trim(),
        name = name.trim(),
        email = email.trim(),
        mobile1 = mobile1.trim(),
        mobile2 = mobile2?.trim()?.takeIf { it.isNotBlank() },
        rank = rank.trim(),
        metalNumber = metalNumber?.trim()?.takeIf { it.isNotBlank() },
        district = district.trim(),
        station = station.trim(),
        bloodGroup = bloodGroup.takeIf { it.isNotBlank() },
        isAdmin = false,
        photoUrl = finalPhotoUrl
    )
}
