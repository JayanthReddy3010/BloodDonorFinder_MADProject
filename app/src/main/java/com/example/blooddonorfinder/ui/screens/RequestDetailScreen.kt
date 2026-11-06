package com.example.blooddonorfinder.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blooddonorfinder.model.BloodRequest
import com.example.blooddonorfinder.model.RequestStatus
import com.example.blooddonorfinder.repository.FirebaseBloodRequestRepository
import com.example.blooddonorfinder.ui.theme.AvailableGreen
import com.example.blooddonorfinder.ui.theme.AvailableGreenBg
import com.example.blooddonorfinder.ui.theme.BloodRed
import com.example.blooddonorfinder.ui.theme.BloodRedContainer
import com.example.blooddonorfinder.ui.theme.BloodRedDark
import com.example.blooddonorfinder.ui.theme.BorderLight
import com.example.blooddonorfinder.ui.theme.PureWhite
import com.example.blooddonorfinder.ui.theme.TextPrimary
import com.example.blooddonorfinder.ui.theme.TextSecondary
import com.example.blooddonorfinder.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    initialRequest: BloodRequest,
    currentUid: String,
    bloodRequestRepository: FirebaseBloodRequestRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Live request state (can be updated by fetch or actions)
    var currentRequest by remember { mutableStateOf(initialRequest) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Async action state
    var isPerformingAction by remember { mutableStateOf(false) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }
    var actionSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Dialog visibility states
    var showAcceptDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    // Accept input state
    var donorInputName by remember { mutableStateOf("") }

    // Fetch latest request document on initial display
    fun refreshRequest() {
        if (currentRequest.requestId.isNotBlank()) {
            isRefreshing = true
            coroutineScope.launch {
                val result = bloodRequestRepository.getBloodRequestById(currentRequest.requestId)
                isRefreshing = false
                if (result.isSuccess) {
                    result.getOrNull()?.let { updated ->
                        currentRequest = updated
                    }
                }
            }
        }
    }

    LaunchedEffect(currentRequest.requestId) {
        refreshRequest()
    }

    val isRequester = currentRequest.requesterUid == currentUid
    val isAssignedDonor = currentRequest.assignedDonorUid == currentUid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Details",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshRequest() },
                        enabled = !isRefreshing && !isPerformingAction
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Request",
                                tint = PureWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BloodRed)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status and Role Banner
            RequestStatusHeader(request = currentRequest, isRequester = isRequester)

            // Blood Group & Urgent Requirement Summary
            RequirementSummaryCard(request = currentRequest)

            // Patient and Hospital Details
            HospitalDetailsCard(request = currentRequest)

            // Contact Information
            ContactDetailsCard(
                request = currentRequest,
                onCallClick = { phone ->
                    if (phone.isNotBlank()) {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        context.startActivity(dialIntent)
                    }
                }
            )

            // Additional Medical Notes
            if (currentRequest.additionalInformation.isNotBlank()) {
                AdditionalNotesCard(notes = currentRequest.additionalInformation)
            }

            // Assigned Donor Details (visible if accepted/completed)
            if (currentRequest.status == RequestStatus.ACCEPTED || currentRequest.status == RequestStatus.COMPLETED) {
                AssignedDonorCard(
                    donorName = currentRequest.assignedDonorName,
                    isCurrentDonor = isAssignedDonor,
                    status = currentRequest.status
                )
            }

            // Action Buttons Section
            ActionButtonsSection(
                request = currentRequest,
                isRequester = isRequester,
                isAssignedDonor = isAssignedDonor,
                isPerformingAction = isPerformingAction,
                onAcceptClick = { showAcceptDialog = true },
                onDeclineClick = { showDeclineDialog = true },
                onCancelClick = { showCancelDialog = true },
                onCompleteClick = { showCompleteDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Accept Confirmation Dialog
    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPerformingAction) showAcceptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Confirm Blood Donation", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you willing to donate ${currentRequest.unitsRequired} unit(s) of ${currentRequest.bloodGroup} at ${currentRequest.hospitalName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = donorInputName,
                        onValueChange = { donorInputName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "By accepting, the requester will see your commitment and can contact you to coordinate donation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val donorName = donorInputName.trim().ifBlank { "Registered Donor" }
                        showAcceptDialog = false
                        isPerformingAction = true
                        coroutineScope.launch {
                            val result = bloodRequestRepository.acceptBloodRequest(
                                requestId = currentRequest.requestId,
                                donorUid = currentUid,
                                donorName = donorName
                            )
                            isPerformingAction = false
                            if (result.isSuccess) {
                                actionSuccessMessage = "You have successfully committed to fulfill this request."
                                refreshRequest()
                            } else {
                                actionErrorMessage = result.exceptionOrNull()?.localizedMessage
                                    ?: "Failed to accept request."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("Accept & Commit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAcceptDialog = false },
                    enabled = !isPerformingAction
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Decline Confirmation Dialog
    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPerformingAction) showDeclineDialog = false },
            title = {
                Text(text = "Decline Blood Request", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to decline this request? It will be removed from your open requests list, but will remain open for other compatible voluntary donors.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeclineDialog = false
                        isPerformingAction = true
                        coroutineScope.launch {
                            val result = bloodRequestRepository.declineBloodRequest(
                                requestId = currentRequest.requestId,
                                donorUid = currentUid
                            )
                            isPerformingAction = false
                            if (result.isSuccess) {
                                onNavigateBack()
                            } else {
                                actionErrorMessage = result.exceptionOrNull()?.localizedMessage
                                    ?: "Failed to decline request."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                ) {
                    Text("Decline")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeclineDialog = false },
                    enabled = !isPerformingAction
                ) {
                    Text("Keep Request")
                }
            }
        )
    }

    // Cancel Confirmation Dialog (Requester)
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPerformingAction) showCancelDialog = false },
            title = {
                Text(text = "Cancel Blood Request", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel this blood request? Once cancelled, donors will no longer be able to accept it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        isPerformingAction = true
                        coroutineScope.launch {
                            val result = bloodRequestRepository.cancelBloodRequest(
                                requestId = currentRequest.requestId,
                                requesterUid = currentUid
                            )
                            isPerformingAction = false
                            if (result.isSuccess) {
                                actionSuccessMessage = "The blood request has been cancelled."
                                refreshRequest()
                            } else {
                                actionErrorMessage = result.exceptionOrNull()?.localizedMessage
                                    ?: "Failed to cancel request."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    enabled = !isPerformingAction
                ) {
                    Text("Back")
                }
            }
        )
    }

    // Complete Confirmation Dialog (Requester)
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPerformingAction) showCompleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AvailableGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Mark as Fulfilled", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                val donorDisplay = currentRequest.assignedDonorName?.ifBlank { "the volunteer donor" } ?: "the volunteer donor"
                Text(
                    text = "Has the blood donation been completed by $donorDisplay? This will mark the request as successfully fulfilled.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        isPerformingAction = true
                        coroutineScope.launch {
                            val result = bloodRequestRepository.completeBloodRequest(
                                requestId = currentRequest.requestId,
                                requesterUid = currentUid
                            )
                            isPerformingAction = false
                            if (result.isSuccess) {
                                actionSuccessMessage = "Request marked as COMPLETED. Thank you!"
                                refreshRequest()
                            } else {
                                actionErrorMessage = result.exceptionOrNull()?.localizedMessage
                                    ?: "Failed to mark request as completed."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen)
                ) {
                    Text("Mark Completed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCompleteDialog = false },
                    enabled = !isPerformingAction
                ) {
                    Text("Not Yet")
                }
            }
        )
    }

    // Error Alert Dialog
    if (actionErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { actionErrorMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Notice", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = actionErrorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { actionErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Success Alert Dialog
    if (actionSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { actionSuccessMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AvailableGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Success", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = actionSuccessMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { actionSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun RequestStatusHeader(request: BloodRequest, isRequester: Boolean) {
    val (statusBg, statusFg, statusLabel) = when (request.status) {
        RequestStatus.OPEN -> Triple(BloodRedContainer, BloodRed, "OPEN - AWAITING DONOR")
        RequestStatus.ACCEPTED -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "ACCEPTED BY DONOR")
        RequestStatus.COMPLETED -> Triple(AvailableGreenBg, AvailableGreen, "FULFILLED & COMPLETED")
        RequestStatus.CANCELLED -> Triple(Color(0xFFEEEEEE), Color(0xFF757575), "CANCELLED")
        RequestStatus.REJECTED -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "DECLINED")
    }

    val formattedDate = remember(request.createdAt) {
        if (request.createdAt > 0) {
            SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(request.createdAt))
        } else {
            "Recently"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        color = statusFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (isRequester) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BloodRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Created by You",
                            color = BloodRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "Created: $formattedDate",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun RequirementSummaryCard(request: BloodRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BloodRedContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "REQUIRED BLOOD TYPE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BloodRed
                )
                Text(
                    text = request.bloodGroup,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BloodRed
                )
                Text(
                    text = "${request.unitsRequired} Unit(s) Needed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Required Date",
                        tint = BloodRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Needed By",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PureWhite
                ) {
                    Text(
                        text = request.requiredDate.ifBlank { "Immediate" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BloodRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HospitalDetailsCard(request: BloodRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Patient & Hospital Information",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )

            // Patient
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Patient Name / Identifier",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = request.patientIdentifier,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
            }

            // Hospital Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Hospital & City",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = request.hospitalName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = request.hospitalLocation,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactDetailsCard(
    request: BloodRequest,
    onCallClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Contact Phone",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "+91 ${request.contactNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
            }

            Button(
                onClick = { onCallClick(request.contactNumber) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AvailableGreen,
                    contentColor = PureWhite
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AdditionalNotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Additional Clinical Notes",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = notes,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun AssignedDonorCard(
    donorName: String?,
    isCurrentDonor: Boolean,
    status: RequestStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status == RequestStatus.COMPLETED) AvailableGreenBg else Color(0xFFE3F2FD)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (status == RequestStatus.COMPLETED) AvailableGreen.copy(alpha = 0.3f) else Color(0xFF90CAF9)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (status == RequestStatus.COMPLETED) AvailableGreen else Color(0xFF1976D2),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (status == RequestStatus.COMPLETED) "Donation Fulfilled By" else "Committed Donor",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (status == RequestStatus.COMPLETED) AvailableGreen else Color(0xFF1976D2)
                )
                val displayDonor = donorName?.ifBlank { "Registered Donor" } ?: "Registered Donor"
                Text(
                    text = if (isCurrentDonor) "$displayDonor (You)" else displayDonor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    request: BloodRequest,
    isRequester: Boolean,
    isAssignedDonor: Boolean,
    isPerformingAction: Boolean,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isRequester) {
            // Requester Controls
            when (request.status) {
                RequestStatus.OPEN -> {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPerformingAction,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BloodRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed)
                    ) {
                        if (isPerformingAction) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Request", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                RequestStatus.ACCEPTED -> {
                    Button(
                        onClick = onCompleteClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPerformingAction,
                        colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen)
                    ) {
                        if (isPerformingAction) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Fulfilled & Completed", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPerformingAction,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BloodRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BloodRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel Request")
                    }
                }

                RequestStatus.COMPLETED -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = AvailableGreenBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AvailableGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This request has been successfully fulfilled.",
                                fontSize = 13.sp,
                                color = AvailableGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                RequestStatus.CANCELLED -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEEEEEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This request was cancelled by you.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                RequestStatus.REJECTED -> {
                    // Not a terminal state for requesters
                }
            }
        } else {
            // Volunteer Donor Controls
            when (request.status) {
                RequestStatus.OPEN -> {
                    Button(
                        onClick = onAcceptClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPerformingAction,
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                    ) {
                        if (isPerformingAction) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept Request & Volunteer", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onDeclineClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPerformingAction
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Decline Request")
                    }
                }

                RequestStatus.ACCEPTED -> {
                    if (isAssignedDonor) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1976D2))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You have committed to fulfilling this blood request. The hospital or requester can reach you.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEEEEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This request has already been accepted by another volunteer donor.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                RequestStatus.COMPLETED -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = AvailableGreenBg
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AvailableGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This blood request has been fulfilled and closed.",
                                fontSize = 13.sp,
                                color = AvailableGreen
                            )
                        }
                    }
                }

                RequestStatus.CANCELLED -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEEEEEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This request was cancelled by the requester.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                RequestStatus.REJECTED -> {}
            }
        }
    }
}
