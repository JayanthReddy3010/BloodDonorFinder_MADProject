package com.example.blooddonorfinder.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.blooddonorfinder.model.Donor

/**
 * In-memory repository managing donor records for the application.
 * Uses Compose SnapshotStateList so UI components react automatically
 * to added or updated donor records.
 */
class DonorRepository {

    // Compose state list holding donor records in memory
    private val _donors = mutableStateListOf<Donor>()

    /**
     * Read-only view of all registered donors.
     */
    val donors: List<Donor>
        get() = _donors

    /**
     * Returns an unmodifiable snapshot list of all registered donors.
     */
    fun getAllDonors(): List<Donor> {
        return _donors.toList()
    }

    /**
     * Adds a newly registered donor to the repository.
     * Newly added donors are placed at the beginning of the list.
     */
    fun addDonor(donor: Donor) {
        _donors.add(0, donor)
    }

    /**
     * Returns the total count of registered donors.
     */
    fun getDonorCount(): Int {
        return _donors.size
    }

    /**
     * Returns true if no donors have been registered yet.
     */
    fun isEmpty(): Boolean {
        return _donors.isEmpty()
    }

    /**
     * Searches in-memory donor records by blood group and availability status.
     * Performs a case-insensitive substring match on the location when provided.
     *
     * @param bloodGroup The requested blood group, or "All Blood Groups".
     * @param locationQuery Optional city/area substring to match.
     * @param requireAvailable When true, only donors with isAvailable == true are returned.
     */
    fun searchDonors(
        bloodGroup: String,
        locationQuery: String,
        requireAvailable: Boolean = true
    ): List<Donor> {
        val trimmedLocation = locationQuery.trim()

        return _donors.filter { donor ->
            // 1. Availability filter
            val availabilityMatches = if (requireAvailable) donor.isAvailable else true

            // 2. Blood group filter
            val groupMatches = if (bloodGroup.isBlank() || bloodGroup.equals("All Blood Groups", ignoreCase = true)) {
                true
            } else {
                donor.bloodGroup.equals(bloodGroup.trim(), ignoreCase = true)
            }

            // 3. Location filter (case-insensitive substring match)
            val locationMatches = if (trimmedLocation.isEmpty()) {
                true
            } else {
                donor.location.contains(trimmedLocation, ignoreCase = true)
            }

            availabilityMatches && groupMatches && locationMatches
        }
    }

    /**
     * Clears all donors from the repository.
     */
    fun clear() {
        _donors.clear()
    }
}
