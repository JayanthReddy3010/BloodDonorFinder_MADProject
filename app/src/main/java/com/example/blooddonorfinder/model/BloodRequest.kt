package com.example.blooddonorfinder.model

import java.util.UUID

/**
 * Lifecycle status of a blood donation request.
 *
 * Allowed transitions:
 * - OPEN -> ACCEPTED (atomic donor acceptance)
 * - OPEN -> CANCELLED (by requester)
 * - ACCEPTED -> COMPLETED (by requester upon fulfillment)
 * - ACCEPTED -> CANCELLED (by requester if no longer required)
 * - CANCELLED -> [Terminal state]
 * - COMPLETED -> [Terminal state]
 */
enum class RequestStatus(val label: String) {
    OPEN("Open"),
    ACCEPTED("Accepted"),
    REJECTED("Declined"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    companion object {
        fun fromString(value: String?): RequestStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OPEN
        }
    }
}

/**
 * Data model representing an emergency blood request.
 *
 * NOTE: strictly stores medical request coordination details.
 * Never store passwords or credentials in this model or in Firestore documents.
 */
data class BloodRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val requesterUid: String = "",
    val patientIdentifier: String = "",
    val bloodGroup: String = "",
    val unitsRequired: Int = 1,
    val hospitalName: String = "",
    val hospitalLocation: String = "",
    val contactNumber: String = "",
    val requiredDate: String = "",
    val additionalInformation: String = "",
    val status: RequestStatus = RequestStatus.OPEN,
    val assignedDonorUid: String? = null,
    val assignedDonorName: String? = null,
    val declinedDonorUids: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Serializes this request into a Firestore-compatible document map.
     */
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "requestId" to requestId,
        "requesterUid" to requesterUid,
        "patientIdentifier" to patientIdentifier,
        "bloodGroup" to bloodGroup,
        "unitsRequired" to unitsRequired,
        "hospitalName" to hospitalName,
        "hospitalLocation" to hospitalLocation,
        "contactNumber" to contactNumber,
        "requiredDate" to requiredDate,
        "additionalInformation" to additionalInformation,
        "status" to status.name,
        "assignedDonorUid" to assignedDonorUid,
        "assignedDonorName" to assignedDonorName,
        "declinedDonorUids" to declinedDonorUids,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        /**
         * Reconstructs a BloodRequest from Firestore document data.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(documentId: String, data: Map<String, Any?>): BloodRequest {
            return BloodRequest(
                requestId = (data["requestId"] as? String)?.takeIf { it.isNotBlank() } ?: documentId,
                requesterUid = data["requesterUid"] as? String ?: "",
                patientIdentifier = data["patientIdentifier"] as? String ?: "",
                bloodGroup = data["bloodGroup"] as? String ?: "",
                unitsRequired = (data["unitsRequired"] as? Number)?.toInt() ?: 1,
                hospitalName = data["hospitalName"] as? String ?: "",
                hospitalLocation = data["hospitalLocation"] as? String ?: "",
                contactNumber = data["contactNumber"] as? String ?: "",
                requiredDate = data["requiredDate"] as? String ?: "",
                additionalInformation = data["additionalInformation"] as? String ?: "",
                status = RequestStatus.fromString(data["status"] as? String),
                assignedDonorUid = (data["assignedDonorUid"] as? String)?.takeIf { it.isNotBlank() },
                assignedDonorName = (data["assignedDonorName"] as? String)?.takeIf { it.isNotBlank() },
                declinedDonorUids = (data["declinedDonorUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
