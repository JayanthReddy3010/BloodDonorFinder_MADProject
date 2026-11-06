package com.example.blooddonorfinder.repository

import android.util.Log
import com.example.blooddonorfinder.model.Donor
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "FirebaseDonorRepo"

/**
 * Extension helper to await Google Play Services / Firebase Tasks using Kotlin Coroutines
 * without requiring additional third-party dependencies.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}

/**
 * Maps raw Firestore exceptions to clear, actionable student-friendly error messages.
 */
fun mapFirestoreException(e: Exception): Exception {
    return when (e) {
        is FirebaseFirestoreException -> {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    Exception("Firestore permission denied. Go to Firebase Console > Firestore Database > Rules and enable test mode or configure read/write permissions.", e)
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    Exception("Network unavailable. Ensure your emulator or test device has an active internet connection.", e)
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    Exception("Unauthenticated request. Ensure Firebase Authentication is enabled in your Firebase project.", e)
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    Exception("Requested Firestore document or collection was not found.", e)
                else ->
                    Exception("Cloud Firestore error (${e.code}): ${e.localizedMessage ?: e.message}", e)
            }
        }
        else -> e
    }
}

/**
 * Cloud Firestore repository for Blood Donor Finder.
 * Provides clean Create, Read, and Update operations on the "donors" collection.
 *
 * NOTE: strictly stores donor profiles.
 * Never stores passwords or authentication secrets in Firestore.
 */
class FirebaseDonorRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val donorsCollection = firestore.collection(COLLECTION_DONORS)

    companion object {
        const val COLLECTION_DONORS = "donors"
    }

    /**
     * VERIFICATION: Tests the live connection to Cloud Firestore.
     * Attempts a lightweight collection read (limit 1) to verify connectivity and rules.
     */
    suspend fun checkConnection(): Result<String> {
        return try {
            donorsCollection.limit(1).get().awaitTask()
            Log.d(TAG, "Cloud Firestore connection check succeeded.")
            Result.success("Cloud Firestore is connected and reachable.")
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Cloud Firestore connection check failed: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * CREATE: Adds or overwrites a donor profile in Cloud Firestore.
     *
     * @param donor The donor profile to create.
     * @return Result containing the donor document ID on success.
     */
    suspend fun createDonor(donor: Donor): Result<String> {
        return try {
            val docId = if (donor.id.isNotBlank()) donor.id else donorsCollection.document().id
            val donorToSave = donor.copy(
                id = docId,
                updatedAt = System.currentTimeMillis()
            )

            donorsCollection
                .document(docId)
                .set(donorToSave.toFirestoreMap())
                .awaitTask()

            Log.d(TAG, "Donor document created in Firestore with ID: $docId")
            Result.success(docId)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error creating donor in Firestore: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for createDonor.
     */
    suspend fun createDonorProfile(donor: Donor): Result<String> = createDonor(donor)

    /**
     * READ: Fetches a single donor profile by document ID.
     *
     * @param donorId The unique document identifier.
     * @return Result containing the donor profile if found, or null if not found.
     */
    suspend fun getDonorById(donorId: String): Result<Donor?> {
        return try {
            val snapshot = donorsCollection.document(donorId).get().awaitTask()
            if (snapshot.exists() && snapshot.data != null) {
                val donor = Donor.fromFirestoreMap(snapshot.id, snapshot.data!!)
                Result.success(donor)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error fetching donor with ID $donorId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for getDonorById.
     */
    suspend fun getDonorProfile(donorId: String): Result<Donor?> = getDonorById(donorId)

    /**
     * READ: Fetches all registered donors from Firestore, sorted by most recently updated.
     *
     * @return Result containing the list of all donor profiles.
     */
    suspend fun getAllDonors(): Result<List<Donor>> {
        return try {
            val querySnapshot = donorsCollection
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .awaitTask()

            val donors = querySnapshot.documents.mapNotNull { doc ->
                doc.data?.let { Donor.fromFirestoreMap(doc.id, it) }
            }
            Log.d(TAG, "Fetched ${donors.size} donors from Firestore.")
            Result.success(donors)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error fetching all donors: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for getAllDonors.
     */
    suspend fun getAllDonorProfiles(): Result<List<Donor>> = getAllDonors()

    /**
     * READ / QUERY: Searches donor records by blood group, location, and availability.
     *
     * @param bloodGroup Blood group to match (e.g., "O+", or "All Blood Groups" for any).
     * @param locationQuery Optional city or area to match (case-insensitive substring).
     * @param requireAvailable Whether only donors with isAvailable == true should be returned.
     */
    suspend fun searchDonors(
        bloodGroup: String,
        locationQuery: String,
        requireAvailable: Boolean = true
    ): Result<List<Donor>> {
        return try {
            var query: Query = donorsCollection

            // Filter by availability if requested
            if (requireAvailable) {
                query = query.whereEqualTo("isAvailable", true)
            }

            // Filter by blood group if specified
            if (bloodGroup.isNotBlank() && !bloodGroup.equals("All Blood Groups", ignoreCase = true)) {
                query = query.whereEqualTo("bloodGroup", bloodGroup.trim())
            }

            val querySnapshot = query.get().awaitTask()
            val trimmedLocation = locationQuery.trim()

            val results = querySnapshot.documents.mapNotNull { doc ->
                doc.data?.let { Donor.fromFirestoreMap(doc.id, it) }
            }.filter { donor ->
                // Apply case-insensitive substring match on location
                if (trimmedLocation.isEmpty()) {
                    true
                } else {
                    donor.location.contains(trimmedLocation, ignoreCase = true)
                }
            }

            Log.d(TAG, "Search in Firestore returned ${results.size} matches.")
            Result.success(results)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error querying donors: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for searchDonors.
     */
    suspend fun searchDonorProfiles(
        bloodGroup: String,
        locationQuery: String,
        requireAvailable: Boolean = true
    ): Result<List<Donor>> = searchDonors(bloodGroup, locationQuery, requireAvailable)

    /**
     * UPDATE: Updates an existing donor profile in Cloud Firestore.
     * Refreshes the updatedAt timestamp automatically.
     *
     * @param donor The updated donor object.
     */
    suspend fun updateDonor(donor: Donor): Result<Unit> {
        return try {
            if (donor.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Cannot update donor without an id"))
            }

            val updatedMap = donor.copy(updatedAt = System.currentTimeMillis()).toFirestoreMap()
            donorsCollection.document(donor.id).set(updatedMap).awaitTask()
            Log.d(TAG, "Updated donor profile in Firestore: ${donor.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error updating donor ${donor.id}: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for updateDonor.
     */
    suspend fun updateDonorProfile(donor: Donor): Result<Unit> = updateDonor(donor)

    /**
     * UPDATE: Updates only the availability status of a donor profile.
     *
     * @param donorId The donor document identifier.
     * @param isAvailable New availability status.
     */
    suspend fun updateAvailability(donorId: String, isAvailable: Boolean): Result<Unit> {
        return try {
            val updates = mapOf(
                "isAvailable" to isAvailable,
                "updatedAt" to System.currentTimeMillis()
            )
            donorsCollection.document(donorId).update(updates).awaitTask()
            Log.d(TAG, "Updated availability for donor $donorId to $isAvailable")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error updating availability for $donorId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for updateAvailability.
     */
    suspend fun updateDonorAvailability(donorId: String, isAvailable: Boolean): Result<Unit> =
        updateAvailability(donorId, isAvailable)

    /**
     * DELETE: Deletes a donor profile from Cloud Firestore.
     *
     * @param donorId The unique document identifier to delete.
     */
    suspend fun deleteDonor(donorId: String): Result<Unit> {
        return try {
            donorsCollection.document(donorId).delete().awaitTask()
            Log.d(TAG, "Deleted donor profile from Firestore: $donorId")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapFirestoreException(e)
            Log.e(TAG, "Error deleting donor $donorId: ${mapped.message}", e)
            Result.failure(mapped)
        }
    }

    /**
     * Alias for deleteDonor.
     */
    suspend fun deleteDonorProfile(donorId: String): Result<Unit> = deleteDonor(donorId)
}
