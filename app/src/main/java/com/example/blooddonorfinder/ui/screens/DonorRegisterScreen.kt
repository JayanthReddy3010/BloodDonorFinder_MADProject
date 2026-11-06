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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.blooddonorfinder.model.Donor
import com.example.blooddonorfinder.ui.theme.AvailableGreen
import com.example.blooddonorfinder.ui.theme.BloodRed
import com.example.blooddonorfinder.ui.theme.BloodRedContainer
import com.example.blooddonorfinder.ui.theme.BloodRedDark
import com.example.blooddonorfinder.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private val INDIAN_MOBILE_REGEX = Regex("^[6-9]\\d{9}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorRegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onRegisterDonor: suspend (Donor) -> Result<String>
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Form field states
    var name by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var isAvailable by remember { mutableStateOf(true) }

    // Dropdown state
    var bloodGroupDropdownExpanded by remember { mutableStateOf(false) }

    // Validation error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var bloodGroupError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var contactError by remember { mutableStateOf<String?>(null) }

    // Submission states to prevent accidental duplicate submission
    var isSubmitting by remember { mutableStateOf(false) }
    var submittedDonor by remember { mutableStateOf<Donor?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var submissionError by remember { mutableStateOf<String?>(null) }
    var isCloudSynced by remember { mutableStateOf(false) }

    fun validateForm(): Boolean {
        var isValid = true

        // 1. Validate Full Name
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            nameError = "Full name is required"
            isValid = false
        } else if (trimmedName.length < 2) {
            nameError = "Name must be at least 2 characters long"
            isValid = false
        } else {
            nameError = null
        }

        // 2. Validate Blood Group
        if (selectedBloodGroup.isBlank()) {
            bloodGroupError = "Please select a blood group"
            isValid = false
        } else {
            bloodGroupError = null
        }

        // 3. Validate Location
        val trimmedLocation = location.trim()
        if (trimmedLocation.isEmpty()) {
            locationError = "Location (city or area) is required"
            isValid = false
        } else if (trimmedLocation.length < 2) {
            locationError = "Please enter a valid city or area"
            isValid = false
        } else {
            locationError = null
        }

        // 4. Validate Contact Number (10-digit Indian Mobile)
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

        return isValid
    }

    fun handleRegister() {
        // Prevent accidental duplicate submission while processing
        if (isSubmitting) return

        focusManager.clearFocus()
        if (!validateForm()) return

        isSubmitting = true
        submissionError = null

        coroutineScope.launch {
            val newDonor = Donor(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                bloodGroup = selectedBloodGroup.trim(),
                location = location.trim(),
                contactNumber = contactNumber.trim(),
                isAvailable = isAvailable
            )

            // Execute registration via repository
            val result = onRegisterDonor(newDonor)
            isSubmitting = false

            if (result.isSuccess) {
                val assignedId = result.getOrNull()
                submittedDonor = if (!assignedId.isNullOrBlank()) newDonor.copy(id = assignedId) else newDonor
                isCloudSynced = true
                showSuccessDialog = true
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Registration failed."
                submissionError = errorMsg
                submittedDonor = newDonor
            }
        }
    }

    fun resetForm() {
        name = ""
        selectedBloodGroup = ""
        location = ""
        contactNumber = ""
        isAvailable = true
        nameError = null
        bloodGroupError = null
        locationError = null
        contactError = null
        submissionError = null
        isCloudSynced = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Register as Donor",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BloodRed
                )
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
            // Informative Header Banner
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Volunteer Registration",
                            fontWeight = FontWeight.Bold,
                            color = BloodRedDark,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Register yourself in the in-memory donor directory. Patients in emergency need can find your contact information.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Main Registration Form Card
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
                        text = "Donor Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. Full Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError != null) nameError = null
                        },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Ramesh Kumar") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BloodRed
                            )
                        },
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { name = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear name"
                                    )
                                }
                            }
                        },
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) {
                                Text(text = nameError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 2. Blood Group Dropdown (8 standard groups)
                    ExposedDropdownMenuBox(
                        expanded = bloodGroupDropdownExpanded,
                        onExpandedChange = { bloodGroupDropdownExpanded = !bloodGroupDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group *") },
                            placeholder = { Text("Select Blood Group") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupDropdownExpanded)
                            },
                            isError = bloodGroupError != null,
                            supportingText = {
                                if (bloodGroupError != null) {
                                    Text(text = bloodGroupError!!, color = MaterialTheme.colorScheme.error)
                                }
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

                    // 3. Location Field
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            if (locationError != null) locationError = null
                        },
                        label = { Text("Location (City / Area) *") },
                        placeholder = { Text("e.g. Anna Nagar, Chennai") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = BloodRed
                            )
                        },
                        trailingIcon = {
                            if (location.isNotEmpty()) {
                                IconButton(onClick = { location = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear location"
                                    )
                                }
                            }
                        },
                        isError = locationError != null,
                        supportingText = {
                            if (locationError != null) {
                                Text(text = locationError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 4. Contact Number Field (Indian 10-digit mobile number)
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { input ->
                            // Allow digits only and restrict to 10 characters
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 10) {
                                contactNumber = digits
                                if (contactError != null) contactError = null
                            }
                        },
                        label = { Text("Contact Number (10 digits) *") },
                        placeholder = { Text("e.g. 9840123456") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = BloodRed
                            )
                        },
                        trailingIcon = {
                            if (contactNumber.isNotEmpty()) {
                                IconButton(onClick = { contactNumber = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear contact"
                                    )
                                }
                            }
                        },
                        isError = contactError != null,
                        supportingText = {
                            if (contactError != null) {
                                Text(text = contactError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(text = "${contactNumber.length}/10 digits (format: 6XXXXXXXXX - 9XXXXXXXXX)")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleRegister() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // 5. Availability Status Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAvailable) "Available to Donate" else "Currently Unavailable",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAvailable) AvailableGreen else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isAvailable) "Your profile will appear in donor search results" else "Hidden from active search results",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAvailable,
                                onCheckedChange = { isAvailable = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = AvailableGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button with Duplicate Submission Prevention & Loading State
                    Button(
                        onClick = { handleRegister() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloodRed,
                            contentColor = PureWhite,
                            disabledContainerColor = BloodRed.copy(alpha = 0.6f),
                            disabledContentColor = PureWhite.copy(alpha = 0.8f)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PureWhite,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Saving Donor Record...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Register Donor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Reset Form Button
                    OutlinedButton(
                        onClick = { resetForm() },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Clear Form")
                    }
                }
            }

            // Student Laboratory Note Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "In-memory repository: Registered donors persist during the current app session and can be immediately queried on the Search screen.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Success Confirmation Dialog with Navigation Options
    if (showSuccessDialog && submittedDonor != null) {
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
                    text = "Donor Registered Successfully",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isCloudSynced) {
                            "Successfully saved to Cloud Firestore and local session registry:"
                        } else {
                            "Saved to local session registry:"
                        },
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
                                text = "Name: ${submittedDonor?.name}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Blood Group: ${submittedDonor?.bloodGroup}",
                                fontWeight = FontWeight.Bold,
                                color = BloodRed
                            )
                            Text(
                                text = "Location: ${submittedDonor?.location}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Contact: ${submittedDonor?.contactNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Status: ${if (submittedDonor?.isAvailable == true) "Available" else "Unavailable"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (submittedDonor?.isAvailable == true) AvailableGreen else MaterialTheme.colorScheme.error
                            )
                            if (submittedDonor?.id?.isNotBlank() == true) {
                                Text(
                                    text = "Doc ID: ${submittedDonor?.id}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "You can now navigate to the search screen to verify or query this donor record.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToSearch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("View in Search")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            resetForm()
                        }
                    ) {
                        Text("Add Another")
                    }
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Home")
                    }
                }
            }
        )
    }

    // Submission Error / Notice Dialog (Handles Firebase exceptions gracefully)
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
                    text = "Firebase Registration Notice",
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
                        text = "Tip: The record has been preserved in the local session repository so you can proceed with testing.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        submissionError = null
                        onNavigateToSearch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("Proceed to Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { submissionError = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}
