package com.example.blooddonorfinder.model

import java.util.UUID

/**
 * Data model representing a blood donor profile.
 * Designed for Firestore document mapping as well as in-memory use.
 *
 * NOTE: strictly stores donor contact and availability details.
 * Never store passwords, tokens, or credentials here.
 *
 * @property id Unique document identifier for the donor record.
 * @property name Full name of the donor.
 * @property bloodGroup Blood group of the donor (e.g., "O+", "A-").
 * @property location City or area of the donor.
 * @property contactNumber 10-digit contact phone number.
 * @property isAvailable Availability status to donate blood.
 * @property userId Optional Firebase Authentication UID of the donor account.
 * @property updatedAt Timestamp of the last profile update.
 */
data class Donor(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val bloodGroup: String = "",
    val location: String = "",
    val contactNumber: String = "",
    val isAvailable: Boolean = true,
    val userId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Converts the donor instance into a Firestore-compatible Map.
     * Contains only safe public donor fields.
     */
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "bloodGroup" to bloodGroup,
        "location" to location,
        "contactNumber" to contactNumber,
        "isAvailable" to isAvailable,
        "userId" to userId,
        "updatedAt" to updatedAt
    )

    companion object {
        /**
         * Reconstructs a Donor instance from Firestore document key-value pairs.
         */
        fun fromFirestoreMap(documentId: String, data: Map<String, Any?>): Donor {
            return Donor(
                id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: documentId,
                name = data["name"] as? String ?: "",
                bloodGroup = data["bloodGroup"] as? String ?: "",
                location = data["location"] as? String ?: "",
                contactNumber = data["contactNumber"] as? String ?: "",
                isAvailable = (data["isAvailable"] as? Boolean) ?: true,
                userId = data["userId"] as? String,
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Standard list of valid blood groups for the application.
 */
val BloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
