package com.example.blooddonorfinder.repository

import android.util.Log
import com.example.blooddonorfinder.model.BloodRequest
import com.example.blooddonorfinder.model.RequestStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query

private const val TAG = "BloodRequestRepo"

/**
 * Cloud Firestore repository for Blood Requests and Status Tracking.
 *
 * Implements atomic concurrency-safe status transitions, donor acceptance,
 * requester cancellation, and completion.
 */
class FirebaseBloodRequestRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val requestsCollection = firestore.collection(COLLECTION_REQUESTS)

    companion object {
        const val COLLECTION_REQUESTS = "bloodRequests"
    }

    /**
     * CREATE: Saves a validated blood request document to Cloud Firestore.
     * Requires an authenticated requesterUid.
     *
     * @param request BloodRequest instance to persist.
     * @return Result containing the document requestId on success.
     */
    suspend fun createBloodRequest(request: BloodRequest): Result<String> {
        return try {
            if (request.requesterUid.isBlank()) {
                return Result.failure(IllegalStateException("Cannot create request: User must be authenticated."))
            }

            val docId = if (request.requestId.isNotBlank()) request.requestId else requestsCollection.document().id
            val requestToSave = request.copy(
                requestId = docId,
                status = RequestStatus.OPEN,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            requestsCollection
                .document(docId)
                .set(requestToSave.toFirestoreMap())
                .awaitTask()

            Log.d(TAG, "Blood request successfully created in Firestore with ID: $docId")
            Result.success(docId)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error creating blood request: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * READ: Fetches a single blood request by document ID.
     */
    suspend fun getBloodRequestById(requestId: String): Result<BloodRequest?> {
        return try {
            val snapshot = requestsCollection.document(requestId).get().awaitTask()
            if (snapshot.exists() && snapshot.data != null) {
                val request = BloodRequest.fromFirestoreMap(snapshot.id, snapshot.data!!)
                Result.success(request)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error fetching blood request $requestId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * READ: Fetches all blood requests created by a specific requester UID.
     * Ordered by most recently created.
     */
    suspend fun getRequestsByRequester(requesterUid: String): Result<List<BloodRequest>> {
        return try {
            if (requesterUid.isBlank()) {
                return Result.success(emptyList())
            }

            val querySnapshot = requestsCollection
                .whereEqualTo("requesterUid", requesterUid)
                .get()
                .awaitTask()

            val requests = querySnapshot.documents.mapNotNull { doc ->
                doc.data?.let { BloodRequest.fromFirestoreMap(doc.id, it) }
            }.sortedByDescending { it.createdAt }

            Log.d(TAG, "Fetched ${requests.size} requests for requester $requesterUid")
            Result.success(requests)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error fetching requests for requester $requesterUid: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * READ: Fetches open blood requests available for eligible donors.
     * Excludes requests created by the donor themselves and requests declined by this donor.
     *
     * @param donorBloodGroup Optional blood group filter.
     * @param donorLocation Optional city/area filter.
     * @param donorUid The viewing donor's UID (to filter out declined or self-created requests).
     */
    suspend fun getOpenRequestsForDonor(
        donorBloodGroup: String? = null,
        donorLocation: String? = null,
        donorUid: String? = null
    ): Result<List<BloodRequest>> {
        return try {
            var query: Query = requestsCollection.whereEqualTo("status", RequestStatus.OPEN.name)

            if (!donorBloodGroup.isNullOrBlank() && !donorBloodGroup.equals("All Blood Groups", ignoreCase = true)) {
                query = query.whereEqualTo("bloodGroup", donorBloodGroup.trim())
            }

            val querySnapshot = query.get().awaitTask()
            val trimmedLocation = donorLocation?.trim() ?: ""

            val filtered = querySnapshot.documents.mapNotNull { doc ->
                doc.data?.let { BloodRequest.fromFirestoreMap(doc.id, it) }
            }.filter { request ->
                // 1. Exclude self-created requests
                val notSelf = if (!donorUid.isNullOrBlank()) request.requesterUid != donorUid else true

                // 2. Exclude requests previously declined by this donor
                val notDeclined = if (!donorUid.isNullOrBlank()) !request.declinedDonorUids.contains(donorUid) else true

                // 3. Location filter (case-insensitive substring match)
                val locationMatch = if (trimmedLocation.isBlank()) {
                    true
                } else {
                    request.hospitalLocation.contains(trimmedLocation, ignoreCase = true) ||
                            request.hospitalName.contains(trimmedLocation, ignoreCase = true)
                }

                notSelf && notDeclined && locationMatch
            }.sortedByDescending { it.createdAt }

            Log.d(TAG, "Fetched ${filtered.size} open requests for donor")
            Result.success(filtered)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error querying open requests: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * ACCEPT (Atomic Concurrency-Safe Transaction):
     * A donor commits to fulfilling an OPEN blood request.
     * Uses a Firestore transaction to prevent multiple donors from accepting the same request concurrently.
     *
     * @param requestId Document ID of the blood request.
     * @param donorUid UID of the accepting donor.
     * @param donorName Name of the accepting donor.
     */
    suspend fun acceptBloodRequest(
        requestId: String,
        donorUid: String,
        donorName: String
    ): Result<Unit> {
        return try {
            if (donorUid.isBlank()) {
                return Result.failure(IllegalStateException("Donor must be authenticated to accept a request."))
            }

            val docRef = requestsCollection.document(requestId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw NoSuchElementException("Blood request $requestId not found.")
                }

                val currentStatus = snapshot.getString("status")
                val assignedUid = snapshot.getString("assignedDonorUid")

                if (currentStatus != RequestStatus.OPEN.name || !assignedUid.isNullOrBlank()) {
                    throw IllegalStateException(
                        "This request is no longer OPEN. Another donor may have already accepted it (current status: $currentStatus)."
                    )
                }

                val requesterUid = snapshot.getString("requesterUid")
                if (requesterUid == donorUid) {
                    throw IllegalStateException("A requester cannot accept their own blood request.")
                }

                val updates = mapOf(
                    "status" to RequestStatus.ACCEPTED.name,
                    "assignedDonorUid" to donorUid,
                    "assignedDonorName" to donorName.ifBlank { "Registered Donor" },
                    "updatedAt" to System.currentTimeMillis()
                )

                transaction.update(docRef, updates)
            }.awaitTask()

            Log.d(TAG, "Request $requestId successfully accepted by donor $donorUid")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Failed to accept request $requestId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * DECLINE:
     * A donor declines an open request directed to them.
     * Atomically adds the donor's UID to declinedDonorUids so the request remains OPEN
     * for all other donors while being hidden from this declining donor.
     */
    suspend fun declineBloodRequest(requestId: String, donorUid: String): Result<Unit> {
        return try {
            if (donorUid.isBlank()) {
                return Result.failure(IllegalStateException("Donor must be authenticated to decline."))
            }

            val docRef = requestsCollection.document(requestId)
            docRef.update(
                mapOf(
                    "declinedDonorUids" to FieldValue.arrayUnion(donorUid),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).awaitTask()

            Log.d(TAG, "Donor $donorUid declined request $requestId (request remains open for others)")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error declining request $requestId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * CANCEL:
     * The requester cancels their own request.
     * Permitted only if the request is not already COMPLETED or CANCELLED.
     */
    suspend fun cancelBloodRequest(requestId: String, requesterUid: String): Result<Unit> {
        return try {
            if (requesterUid.isBlank()) {
                return Result.failure(IllegalStateException("User must be authenticated."))
            }

            val docRef = requestsCollection.document(requestId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw NoSuchElementException("Request $requestId does not exist.")
                }

                val actualRequester = snapshot.getString("requesterUid")
                if (actualRequester != requesterUid) {
                    throw SecurityException("Only the requester who created this request can cancel it.")
                }

                val currentStatus = snapshot.getString("status")
                if (currentStatus == RequestStatus.COMPLETED.name) {
                    throw IllegalStateException("Cannot cancel an already completed blood request.")
                }
                if (currentStatus == RequestStatus.CANCELLED.name) {
                    throw IllegalStateException("Request is already cancelled.")
                }

                val updates = mapOf(
                    "status" to RequestStatus.CANCELLED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
                transaction.update(docRef, updates)
            }.awaitTask()

            Log.d(TAG, "Request $requestId cancelled by requester $requesterUid")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error cancelling request $requestId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * COMPLETE:
     * The requester marks an ACCEPTED blood request as successfully COMPLETED upon fulfillment.
     */
    suspend fun completeBloodRequest(requestId: String, requesterUid: String): Result<Unit> {
        return try {
            if (requesterUid.isBlank()) {
                return Result.failure(IllegalStateException("User must be authenticated."))
            }

            val docRef = requestsCollection.document(requestId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw NoSuchElementException("Request $requestId does not exist.")
                }

                val actualRequester = snapshot.getString("requesterUid")
                if (actualRequester != requesterUid) {
                    throw SecurityException("Only the requester who created this request can mark it completed.")
                }

                val currentStatus = snapshot.getString("status")
                if (currentStatus != RequestStatus.ACCEPTED.name) {
                    throw IllegalStateException("Only ACCEPTED requests can be marked COMPLETED (current status: $currentStatus).")
                }

                val updates = mapOf(
                    "status" to RequestStatus.COMPLETED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
                transaction.update(docRef, updates)
            }.awaitTask()

            Log.d(TAG, "Request $requestId marked COMPLETED by requester $requesterUid")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error completing request $requestId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }
}
