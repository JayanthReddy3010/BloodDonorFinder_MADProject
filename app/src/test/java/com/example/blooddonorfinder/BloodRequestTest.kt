package com.example.blooddonorfinder

import com.example.blooddonorfinder.model.BloodGroups
import com.example.blooddonorfinder.model.BloodRequest
import com.example.blooddonorfinder.model.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodRequestTest {

    @Test
    fun testBloodRequestCreationDefaultValues() {
        val request = BloodRequest(
            requesterUid = "auth_user_123",
            patientIdentifier = "Rahul Verma",
            bloodGroup = "O+",
            unitsRequired = 2,
            hospitalName = "Apollo Hospital",
            hospitalLocation = "Greams Road, Chennai",
            contactNumber = "9840123456",
            requiredDate = "2026-10-15"
        )

        assertEquals("auth_user_123", request.requesterUid)
        assertEquals("Rahul Verma", request.patientIdentifier)
        assertEquals("O+", request.bloodGroup)
        assertEquals(2, request.unitsRequired)
        assertEquals("Apollo Hospital", request.hospitalName)
        assertEquals("Greams Road, Chennai", request.hospitalLocation)
        assertEquals("9840123456", request.contactNumber)
        assertEquals("2026-10-15", request.requiredDate)
        assertEquals(RequestStatus.OPEN, request.status)
        assertNull(request.assignedDonorUid)
        assertNull(request.assignedDonorName)
        assertTrue(request.declinedDonorUids.isEmpty())
    }

    @Test
    fun testToFirestoreMapSerialization() {
        val request = BloodRequest(
            requestId = "req_abc_999",
            requesterUid = "user_456",
            patientIdentifier = "Ananya Sen",
            bloodGroup = "AB-",
            unitsRequired = 3,
            hospitalName = "Fortis Malar Hospital",
            hospitalLocation = "Adyar, Chennai",
            contactNumber = "9876543210",
            requiredDate = "2026-10-20",
            additionalInformation = "Emergency surgery scheduled",
            status = RequestStatus.ACCEPTED,
            assignedDonorUid = "donor_789",
            assignedDonorName = "Karthik Raja",
            declinedDonorUids = listOf("donor_111", "donor_222"),
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L
        )

        val map = request.toFirestoreMap()

        assertEquals("req_abc_999", map["requestId"])
        assertEquals("user_456", map["requesterUid"])
        assertEquals("Ananya Sen", map["patientIdentifier"])
        assertEquals("AB-", map["bloodGroup"])
        assertEquals(3, map["unitsRequired"])
        assertEquals("Fortis Malar Hospital", map["hospitalName"])
        assertEquals("Adyar, Chennai", map["hospitalLocation"])
        assertEquals("9876543210", map["contactNumber"])
        assertEquals("2026-10-20", map["requiredDate"])
        assertEquals("Emergency surgery scheduled", map["additionalInformation"])
        assertEquals("ACCEPTED", map["status"])
        assertEquals("donor_789", map["assignedDonorUid"])
        assertEquals("Karthik Raja", map["assignedDonorName"])
        assertEquals(listOf("donor_111", "donor_222"), map["declinedDonorUids"])
        assertEquals(1700000000000L, map["createdAt"])
        assertEquals(1700000050000L, map["updatedAt"])
    }

    @Test
    fun testFromFirestoreMapDeserialization() {
        val map = mapOf<String, Any>(
            "requesterUid" to "uid_test_1",
            "patientIdentifier" to "Suresh Raina",
            "bloodGroup" to "B+",
            "unitsRequired" to 2L, // Firestore numbers often parse as Long
            "hospitalName" to "MIOT Hospital",
            "hospitalLocation" to "Manapakkam, Chennai",
            "contactNumber" to "9123456789",
            "requiredDate" to "2026-10-18",
            "additionalInformation" to "Platelet requirement",
            "status" to "OPEN",
            "assignedDonorUid" to "",
            "assignedDonorName" to "",
            "declinedDonorUids" to listOf("donor_x", "donor_y"),
            "createdAt" to 1690000000000L,
            "updatedAt" to 1690000010000L
        )

        val request = BloodRequest.fromFirestoreMap("doc_id_101", map)

        assertEquals("doc_id_101", request.requestId)
        assertEquals("uid_test_1", request.requesterUid)
        assertEquals("Suresh Raina", request.patientIdentifier)
        assertEquals("B+", request.bloodGroup)
        assertEquals(2, request.unitsRequired)
        assertEquals("MIOT Hospital", request.hospitalName)
        assertEquals("Manapakkam, Chennai", request.hospitalLocation)
        assertEquals("9123456789", request.contactNumber)
        assertEquals("2026-10-18", request.requiredDate)
        assertEquals("Platelet requirement", request.additionalInformation)
        assertEquals(RequestStatus.OPEN, request.status)
        assertNull(request.assignedDonorUid)
        assertNull(request.assignedDonorName)
        assertEquals(listOf("donor_x", "donor_y"), request.declinedDonorUids)
        assertEquals(1690000000000L, request.createdAt)
        assertEquals(1690000010000L, request.updatedAt)
    }

    @Test
    fun testFromFirestoreMapHandlesMissingOrCorruptFieldsGracefully() {
        val emptyMap = emptyMap<String, Any>()
        val request = BloodRequest.fromFirestoreMap("fallback_id", emptyMap)

        assertEquals("fallback_id", request.requestId)
        assertEquals("", request.requesterUid)
        assertEquals("", request.patientIdentifier)
        assertEquals("", request.bloodGroup)
        assertEquals(1, request.unitsRequired)
        assertEquals("", request.hospitalName)
        assertEquals("", request.hospitalLocation)
        assertEquals("", request.contactNumber)
        assertEquals(RequestStatus.OPEN, request.status)
        assertTrue(request.declinedDonorUids.isEmpty())
    }

    @Test
    fun testStatusTransitions() {
        val initial = BloodRequest(
            requestId = "req_1",
            requesterUid = "user_1",
            patientIdentifier = "Patient A",
            bloodGroup = "O-",
            unitsRequired = 1,
            hospitalName = "GH",
            hospitalLocation = "Chennai",
            contactNumber = "9840123456",
            requiredDate = "2026-10-12",
            status = RequestStatus.OPEN
        )

        // Donor accepts
        val accepted = initial.copy(
            status = RequestStatus.ACCEPTED,
            assignedDonorUid = "donor_1",
            assignedDonorName = "Dr. Donor"
        )
        assertEquals(RequestStatus.ACCEPTED, accepted.status)
        assertEquals("donor_1", accepted.assignedDonorUid)

        // Requester completes
        val completed = accepted.copy(status = RequestStatus.COMPLETED)
        assertEquals(RequestStatus.COMPLETED, completed.status)

        // Requester cancels from OPEN
        val cancelled = initial.copy(status = RequestStatus.CANCELLED)
        assertEquals(RequestStatus.CANCELLED, cancelled.status)
    }

    @Test
    fun testBloodGroupsList() {
        val expected = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        assertEquals(expected, BloodGroups)
    }

    @Test
    fun testIndianMobileNumberValidationRegex() {
        val regex = Regex("^[6-9]\\d{9}$")

        // Valid 10-digit Indian numbers starting with 6, 7, 8, or 9
        assertTrue(regex.matches("9840123456"))
        assertTrue(regex.matches("8765432109"))
        assertTrue(regex.matches("7012345678"))
        assertTrue(regex.matches("6381234567"))

        // Invalid numbers
        assertFalse(regex.matches("1234567890")) // Starts with 1
        assertFalse(regex.matches("5987654321")) // Starts with 5
        assertFalse(regex.matches("984012345"))  // 9 digits
        assertFalse(regex.matches("98401234567")) // 11 digits
        assertFalse(regex.matches("98401abcde")) // Non-digits
        assertFalse(regex.matches(""))           // Blank
    }
}
