package com.example.blooddonorfinder

import com.example.blooddonorfinder.model.Donor
import com.example.blooddonorfinder.repository.DonorRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DonorRepositoryTest {

    private lateinit var repository: DonorRepository

    @Before
    fun setUp() {
        repository = DonorRepository()
    }

    @Test
    fun testRepositoryInitiallyEmpty() {
        assertTrue(repository.isEmpty())
        assertEquals(0, repository.getDonorCount())
    }

    @Test
    fun testAddDonor() {
        val donor = Donor(
            name = "Ramesh Kumar",
            bloodGroup = "O+",
            location = "Anna Nagar, Chennai",
            contactNumber = "9840123456",
            isAvailable = true
        )
        repository.addDonor(donor)

        assertFalse(repository.isEmpty())
        assertEquals(1, repository.getDonorCount())
        assertEquals(donor, repository.getAllDonors().first())
    }

    @Test
    fun testSearchByBloodGroup() {
        val donor1 = Donor(
            name = "Ramesh Kumar",
            bloodGroup = "O+",
            location = "Chennai",
            contactNumber = "9840123456",
            isAvailable = true
        )
        val donor2 = Donor(
            name = "Priya Sharma",
            bloodGroup = "A+",
            location = "Chennai",
            contactNumber = "9876543210",
            isAvailable = true
        )
        repository.addDonor(donor1)
        repository.addDonor(donor2)

        val oPlusResults = repository.searchDonors(bloodGroup = "O+", locationQuery = "")
        assertEquals(1, oPlusResults.size)
        assertEquals("Ramesh Kumar", oPlusResults.first().name)

        val aPlusResults = repository.searchDonors(bloodGroup = "A+", locationQuery = "")
        assertEquals(1, aPlusResults.size)
        assertEquals("Priya Sharma", aPlusResults.first().name)
    }

    @Test
    fun testSearchByCaseInsensitiveLocation() {
        val donor = Donor(
            name = "Karthik Raja",
            bloodGroup = "B+",
            location = "Velachery, Chennai",
            contactNumber = "9123456780",
            isAvailable = true
        )
        repository.addDonor(donor)

        val matchesLower = repository.searchDonors(bloodGroup = "All Blood Groups", locationQuery = "chennai")
        assertEquals(1, matchesLower.size)

        val matchesUpper = repository.searchDonors(bloodGroup = "All Blood Groups", locationQuery = "VELACHERY")
        assertEquals(1, matchesUpper.size)

        val noMatch = repository.searchDonors(bloodGroup = "All Blood Groups", locationQuery = "Bangalore")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun testSearchExcludesUnavailableDonors() {
        val availableDonor = Donor(
            name = "Ananya Verma",
            bloodGroup = "O-",
            location = "Bangalore",
            contactNumber = "9988776655",
            isAvailable = true
        )
        val unavailableDonor = Donor(
            name = "Mohamed Irfan",
            bloodGroup = "O-",
            location = "Bangalore",
            contactNumber = "9765432109",
            isAvailable = false
        )
        repository.addDonor(availableDonor)
        repository.addDonor(unavailableDonor)

        val results = repository.searchDonors(bloodGroup = "O-", locationQuery = "Bangalore", requireAvailable = true)
        assertEquals(1, results.size)
        assertEquals("Ananya Verma", results.first().name)
    }
}
