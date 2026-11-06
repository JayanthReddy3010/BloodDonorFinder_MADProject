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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blooddonorfinder.model.BloodGroups
import com.example.blooddonorfinder.model.Donor
import com.example.blooddonorfinder.repository.DonorRepository
import com.example.blooddonorfinder.ui.theme.AvailableGreen
import com.example.blooddonorfinder.ui.theme.AvailableGreenBg
import com.example.blooddonorfinder.ui.theme.BloodRed
import com.example.blooddonorfinder.ui.theme.BloodRedContainer
import com.example.blooddonorfinder.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ALL_GROUPS = "All Blood Groups"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorSearchScreen(
    donorRepository: DonorRepository,
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onCloudSearch: (suspend (bloodGroup: String, location: String) -> Result<List<Donor>>)? = null
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Search filter inputs
    var selectedBloodGroup by remember { mutableStateOf(ALL_GROUPS) }
    var locationQuery by remember { mutableStateOf("") }
    var bloodGroupDropdownExpanded by remember { mutableStateOf(false) }

    // Search results state
    var searchResults by remember { mutableStateOf<List<Donor>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasExecutedSearch by remember { mutableStateOf(false) }
    var cloudNotice by remember { mutableStateOf<String?>(null) }

    val bloodGroupFilterOptions = remember { listOf(ALL_GROUPS) + BloodGroups }

    // Initial search load: queries currently registered available donors
    LaunchedEffect(donorRepository.donors.size) {
        if (onCloudSearch != null) {
            val cloudResult = onCloudSearch(selectedBloodGroup, locationQuery)
            if (cloudResult.isSuccess) {
                searchResults = cloudResult.getOrNull().orEmpty()
            } else {
                cloudNotice = cloudResult.exceptionOrNull()?.localizedMessage
                searchResults = donorRepository.searchDonors(
                    bloodGroup = selectedBloodGroup,
                    locationQuery = locationQuery,
                    requireAvailable = true
                )
            }
        } else {
            searchResults = donorRepository.searchDonors(
                bloodGroup = selectedBloodGroup,
                locationQuery = locationQuery,
                requireAvailable = true
            )
        }
    }

    fun executeSearch() {
        focusManager.clearFocus()
        isSearching = true
        cloudNotice = null
        coroutineScope.launch {
            if (onCloudSearch != null) {
                val cloudResult = onCloudSearch(selectedBloodGroup, locationQuery)
                if (cloudResult.isSuccess) {
                    searchResults = cloudResult.getOrNull().orEmpty()
                } else {
                    cloudNotice = cloudResult.exceptionOrNull()?.localizedMessage
                    // Fallback to local session repository
                    searchResults = donorRepository.searchDonors(
                        bloodGroup = selectedBloodGroup,
                        locationQuery = locationQuery,
                        requireAvailable = true
                    )
                }
            } else {
                delay(300)
                searchResults = donorRepository.searchDonors(
                    bloodGroup = selectedBloodGroup,
                    locationQuery = locationQuery,
                    requireAvailable = true
                )
            }
            isSearching = false
            hasExecutedSearch = true
        }
    }

    fun resetFilters() {
        focusManager.clearFocus()
        selectedBloodGroup = ALL_GROUPS
        locationQuery = ""
        cloudNotice = null
        coroutineScope.launch {
            if (onCloudSearch != null) {
                val cloudResult = onCloudSearch(ALL_GROUPS, "")
                if (cloudResult.isSuccess) {
                    searchResults = cloudResult.getOrNull().orEmpty()
                } else {
                    searchResults = donorRepository.searchDonors(
                        bloodGroup = ALL_GROUPS,
                        locationQuery = "",
                        requireAvailable = true
                    )
                }
            } else {
                searchResults = donorRepository.searchDonors(
                    bloodGroup = ALL_GROUPS,
                    locationQuery = "",
                    requireAvailable = true
                )
            }
            hasExecutedSearch = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Find a Donor",
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
        ) {
            // Search Filters Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Search Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. Blood Group Dropdown Selector
                    ExposedDropdownMenuBox(
                        expanded = bloodGroupDropdownExpanded,
                        onExpandedChange = { bloodGroupDropdownExpanded = !bloodGroupDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBloodGroup,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupDropdownExpanded)
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
                            bloodGroupFilterOptions.forEach { group ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = group,
                                            fontWeight = if (group == selectedBloodGroup) FontWeight.Bold else FontWeight.Normal,
                                            color = if (group == selectedBloodGroup) BloodRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedBloodGroup = group
                                        bloodGroupDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Location Input Field
                    OutlinedTextField(
                        value = locationQuery,
                        onValueChange = { locationQuery = it },
                        label = { Text("Location (City or Area)") },
                        placeholder = { Text("e.g. Chennai, Bangalore") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = BloodRed
                            )
                        },
                        trailingIcon = {
                            if (locationQuery.isNotEmpty()) {
                                IconButton(onClick = { locationQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear location"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { executeSearch() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed,
                            focusedLabelColor = BloodRed
                        )
                    )

                    // Action Buttons (Search & Reset)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { executeSearch() },
                            enabled = !isSearching,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BloodRed,
                                contentColor = PureWhite
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Search Donors",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { resetFilters() },
                            enabled = !isSearching,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Reset")
                        }
                    }
                }
            }

            // Results / Empty State / Loading State Content
            if (cloudNotice != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BloodRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud notice: $cloudNotice (Showing local session records)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isSearching) {
                // Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BloodRed,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Searching donors...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (donorRepository.isEmpty() && searchResults.isEmpty()) {
                // Entire Repository is Empty (No fabricated data)
                EmptyRepositoryView(
                    onRegisterClick = onNavigateToRegister
                )
            } else if (searchResults.isEmpty()) {
                // Donors exist, but no match for this specific query
                NoMatchesView(
                    selectedGroup = selectedBloodGroup,
                    locationQuery = locationQuery,
                    onReset = { resetFilters() }
                )
            } else {
                // Matching Donors Found
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Matching Donors (${searchResults.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BloodRedContainer
                            ) {
                                Text(
                                    text = if (hasExecutedSearch) "Filtered" else "All Available",
                                    color = BloodRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(searchResults, key = { it.id }) { donor ->
                        DonorResultCard(
                            donor = donor,
                            onCallClick = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Simulated call to ${donor.name} (${donor.contactNumber})"
                                    )
                                }
                            }
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Active session records: Retrieved from in-memory repository without fabricated data.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonorResultCard(
    donor: Donor,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blood Group Badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BloodRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = donor.bloodGroup,
                    color = PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Donor Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = donor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = donor.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (donor.isAvailable) AvailableGreenBg else BloodRedContainer
                    ) {
                        Text(
                            text = if (donor.isAvailable) "● Available" else "● Unavailable",
                            color = if (donor.isAvailable) AvailableGreen else BloodRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Ph: ${donor.contactNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick Call Button
            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BloodRedContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Simulate Call to ${donor.name}",
                    tint = BloodRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyRepositoryView(
    onRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "No Donors Registered Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "The in-memory registry is currently empty in this session. Register as a donor first, and your record will immediately be searchable here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onRegisterClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text(
                        text = "Register as Donor",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NoMatchesView(
    selectedGroup: String,
    locationQuery: String,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "No Matching Donors Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val criteriaText = buildString {
                    append("No available donors matched ")
                    if (selectedGroup != ALL_GROUPS) {
                        append("blood group '$selectedGroup' ")
                    }
                    if (locationQuery.isNotBlank()) {
                        append("in '$locationQuery'")
                    }
                    if (selectedGroup == ALL_GROUPS && locationQuery.isBlank()) {
                        append("the selected criteria")
                    }
                }

                Text(
                    text = "$criteriaText. Verify the location spelling or reset filters to view all available registered donors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text(
                        text = "Reset All Filters",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
