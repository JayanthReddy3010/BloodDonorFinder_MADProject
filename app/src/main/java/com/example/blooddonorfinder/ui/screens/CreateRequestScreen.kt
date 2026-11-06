package com.example.blooddonorfinder.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blooddonorfinder.model.BloodGroups
import com.example.blooddonorfinder.model.BloodRequest
import com.example.blooddonorfinder.ui.theme.AvailableGreen
import com.example.blooddonorfinder.ui.theme.BloodRed
import com.example.blooddonorfinder.ui.theme.BloodRedContainer
import com.example.blooddonorfinder.ui.theme.BloodRedDark
import com.example.blooddonorfinder.ui.theme.PureWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val INDIAN_MOBILE_REGEX = Regex("^[6-9]\\d{9}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    currentRequesterUid: String,
    onNavigateBack: () -> Unit,
    onNavigateToMyRequests: () -> Unit,
    onSubmitRequest: suspend (BloodRequest) -> Result<String>
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Form field states
    var patientName by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("") }
    var unitsRequiredText by remember { mutableStateOf("1") }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalLocation by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var requiredDate by remember {
        val defaultDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        mutableStateOf(defaultDate)
    }
    var additionalInformation by remember { mutableStateOf("") }

    // Dropdown state
    var bloodGroupDropdownExpanded by remember { mutableStateOf(false) }

    // Validation error states
    var patientError by remember { mutableStateOf<String?>(null) }
    var bloodGroupError by remember { mutableStateOf<String?>(null) }
    var unitsError by remember { mutableStateOf<String?>(null) }
    var hospitalNameError by remember { mutableStateOf<String?>(null) }
    var hospitalLocationError by remember { mutableStateOf<String?>(null) }
    var contactError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    // Submission states
    var isSubmitting by remember { mutableStateOf(false) }
    var createdRequestId by remember { mutableStateOf<String?>(null) }
    var submissionError by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    fun validateForm(): Boolean {
        var isValid = true

        // 1. Patient Name / Identifier
        val trimmedPatient = patientName.trim()
        if (trimmedPatient.isEmpty()) {
            patientError = "Patient name or identifier is required"
            isValid = false
        } else if (trimmedPatient.length < 2) {
            patientError = "Patient identifier must be at least 2 characters"
            isValid = false
        } else {
            patientError = null
        }

        // 2. Blood Group
        if (selectedBloodGroup.isBlank() || !BloodGroups.contains(selectedBloodGroup)) {
            bloodGroupError = "Please select a valid blood group"
            isValid = false
        } else {
            bloodGroupError = null
        }

        // 3. Units Required (1..10)
        val units = unitsRequiredText.trim().toIntOrNull()
        if (units == null || units <= 0) {
            unitsError = "Units must be at least 1"
            isValid = false
        } else if (units > 10) {
            unitsError = "Units cannot exceed 10 in a single request"
            isValid = false
        } else {
            unitsError = null
        }

        // 4. Hospital Name
        val trimmedHospital = hospitalName.trim()
        if (trimmedHospital.isEmpty()) {
            hospitalNameError = "Hospital name is required"
            isValid = false
        } else {
            hospitalNameError = null
        }

        // 5. Hospital Location
        val trimmedLoc = hospitalLocation.trim()
        if (trimmedLoc.isEmpty()) {
            hospitalLocationError = "Hospital city/area is required"
            isValid = false
        } else {
            hospitalLocationError = null
        }

        // 6. Contact Number
        val trimmedContact = contactNumber.trim()
        if (trimmedContact.isEmpty()) {
            contactError = "Contact number is required"
            isValid = false
        } else if (trimmedContact.length != 10) {
            contactError = "Contact number must be exactly 10 digits"
            isValid = false
        } else if (!INDIAN_MOBILE_REGEX.matches(trimmedContact)) {
            contactError = "Enter a valid Indian mobile number starting with 6-9"
            isValid = false
        } else {
            contactError = null
        }

        // 7. Required Date
        val trimmedDate = requiredDate.trim()
        if (trimmedDate.isEmpty()) {
            dateError = "Required date cannot be blank"
            isValid = false
        } else {
            dateError = null
        }

        return isValid
    }

    fun handleSubmit() {
        if (isSubmitting) return

        focusManager.clearFocus()
        if (!validateForm()) return

        isSubmitting = true
        submissionError = null

        coroutineScope.launch {
            val newRequest = BloodRequest(
                requestId = UUID.randomUUID().toString(),
                requesterUid = currentRequesterUid,
                patientIdentifier = patientName.trim(),
                bloodGroup = selectedBloodGroup.trim(),
                unitsRequired = unitsRequiredText.trim().toIntOrNull() ?: 1,
                hospitalName = hospitalName.trim(),
                hospitalLocation = hospitalLocation.trim(),
                contactNumber = contactNumber.trim(),
                requiredDate = requiredDate.trim(),
                additionalInformation = additionalInformation.trim()
            )

            val result = onSubmitRequest(newRequest)
            isSubmitting = false

            if (result.isSuccess) {
                createdRequestId = result.getOrNull() ?: newRequest.requestId
                showSuccessDialog = true
            } else {
                submissionError = result.exceptionOrNull()?.localizedMessage ?: "Failed to save request to Firestore."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Blood Request",
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
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BloodRedContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BloodRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Emergency Request Portal",
                            fontWeight = FontWeight.Bold,
                            color = BloodRedDark,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Submit a validated blood requirement. Registered voluntary donors will be notified in the open directory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Request Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Patient & Blood Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. Patient Name / Identifier
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = {
                            patientName = it
                            if (patientError != null) patientError = null
                        },
                        label = { Text("Patient Identifier / Name *") },
                        placeholder = { Text("e.g. Rohan Sharma (Ward 4B)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = BloodRed)
                        },
                        trailingIcon = {
                            if (patientName.isNotEmpty()) {
                                IconButton(onClick = { patientName = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        isError = patientError != null,
                        supportingText = {
                            patientError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 2. Blood Group Dropdown
                    ExposedDropdownMenuBox(
                        expanded = bloodGroupDropdownExpanded,
                        onExpandedChange = { bloodGroupDropdownExpanded = !bloodGroupDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Required Blood Group *") },
                            placeholder = { Text("Select blood type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupDropdownExpanded)
                            },
                            isError = bloodGroupError != null,
                            supportingText = {
                                bloodGroupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BloodRed,
                                focusedLabelColor = BloodRed
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = bloodGroupDropdownExpanded,
                            onDismissRequest = { bloodGroupDropdownExpanded = false }
                        ) {
                            BloodGroups.forEach { group ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BloodRedContainer,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = group,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BloodRed,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(text = "Blood Group $group")
                                        }
                                    },
                                    onClick = {
                                        selectedBloodGroup = group
                                        bloodGroupDropdownExpanded = false
                                        bloodGroupError = null
                                    }
                                )
                            }
                        }
                    }

                    // 3. Units Required (1..10)
                    OutlinedTextField(
                        value = unitsRequiredText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 2) {
                                unitsRequiredText = digits
                                if (unitsError != null) unitsError = null
                            }
                        },
                        label = { Text("Units of Blood Required (1 - 10) *") },
                        placeholder = { Text("e.g. 2") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = BloodRed)
                        },
                        isError = unitsError != null,
                        supportingText = {
                            unitsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 4. Hospital Name
                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = {
                            hospitalName = it
                            if (hospitalNameError != null) hospitalNameError = null
                        },
                        label = { Text("Hospital Name *") },
                        placeholder = { Text("e.g. Apollo Hospital") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = BloodRed)
                        },
                        isError = hospitalNameError != null,
                        supportingText = {
                            hospitalNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 5. Hospital City / Location
                    OutlinedTextField(
                        value = hospitalLocation,
                        onValueChange = {
                            hospitalLocation = it
                            if (hospitalLocationError != null) hospitalLocationError = null
                        },
                        label = { Text("Hospital City / Area *") },
                        placeholder = { Text("e.g. Greams Road, Chennai") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = BloodRed)
                        },
                        isError = hospitalLocationError != null,
                        supportingText = {
                            hospitalLocationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 6. Contact Number (10 digits)
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 10) {
                                contactNumber = digits
                                if (contactError != null) contactError = null
                            }
                        },
                        label = { Text("Emergency Contact Number *") },
                        placeholder = { Text("10-digit mobile number") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = BloodRed)
                        },
                        isError = contactError != null,
                        supportingText = {
                            if (contactError != null) {
                                Text(contactError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("${contactNumber.length}/10 digits (6XXXXXXXXX - 9XXXXXXXXX)")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 7. Required Date
                    OutlinedTextField(
                        value = requiredDate,
                        onValueChange = {
                            requiredDate = it
                            if (dateError != null) dateError = null
                        },
                        label = { Text("Required Date (YYYY-MM-DD) *") },
                        placeholder = { Text("e.g. 2026-10-15") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = BloodRed)
                        },
                        isError = dateError != null,
                        supportingText = {
                            dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 8. Additional Information (Optional)
                    OutlinedTextField(
                        value = additionalInformation,
                        onValueChange = { additionalInformation = it },
                        label = { Text("Additional Information (Optional)") },
                        placeholder = { Text("e.g. Scheduled surgery at 10 AM, ask for Dr. Rao") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BloodRed)
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button with Duplicate Submission Prevention & Loading State
                    Button(
                        onClick = { handleSubmit() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloodRed,
                            contentColor = PureWhite,
                            disabledContainerColor = BloodRed.copy(alpha = 0.6f)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PureWhite,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Saving Request to Firestore...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Submit Blood Request",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Success Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AvailableGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Blood Request Created!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your blood request has been validated and persisted in Cloud Firestore:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BloodRedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Blood Group: $selectedBloodGroup (${unitsRequiredText} unit(s))",
                                fontWeight = FontWeight.Bold,
                                color = BloodRed
                            )
                            Text(
                                text = "Patient: $patientName",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Hospital: $hospitalName ($hospitalLocation)",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Required Date: $requiredDate",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Status: OPEN",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BloodRed
                            )
                            if (!createdRequestId.isNullOrBlank()) {
                                Text(
                                    text = "Request ID: $createdRequestId",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Eligible voluntary donors will now be able to view this request and accept it in real time.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToMyRequests()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("Track Requests")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Home")
                }
            }
        )
    }

    // Submission Failure Dialog
    if (submissionError != null) {
        AlertDialog(
            onDismissRequest = { submissionError = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Request Submission Failed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = submissionError ?: "An unexpected error occurred.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tip: Verify your Firestore database security rules in the Firebase Console (test mode) and ensure your device has network connectivity.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { submissionError = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("OK")
                }
            }
        )
    }
}
