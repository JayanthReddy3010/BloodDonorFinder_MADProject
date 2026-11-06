package com.example.blooddonorfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blooddonorfinder.model.BloodRequest
import com.example.blooddonorfinder.model.RequestStatus
import com.example.blooddonorfinder.repository.FirebaseBloodRequestRepository
import com.example.blooddonorfinder.ui.theme.AvailableGreen
import com.example.blooddonorfinder.ui.theme.AvailableGreenBg
import com.example.blooddonorfinder.ui.theme.BloodRed
import com.example.blooddonorfinder.ui.theme.BloodRedContainer
import com.example.blooddonorfinder.ui.theme.PureWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestListScreen(
    currentUid: String,
    bloodRequestRepository: FirebaseBloodRequestRepository,
    onNavigateBack: () -> Unit,
    onCreateRequestClick: () -> Unit,
    onRequestClick: (BloodRequest) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 0: My Created Requests, 1: Open Requests for Donors
    var selectedTab by remember { mutableIntStateOf(0) }

    // State for My Requests
    var myRequests by remember { mutableStateOf<List<BloodRequest>>(emptyList()) }
    var isLoadingMyRequests by remember { mutableStateOf(false) }
    var myRequestsError by remember { mutableStateOf<String?>(null) }

    // State for Open Requests
    var openRequests by remember { mutableStateOf<List<BloodRequest>>(emptyList()) }
    var isLoadingOpenRequests by remember { mutableStateOf(false) }
    var openRequestsError by remember { mutableStateOf<String?>(null) }

    fun loadMyRequests() {
        isLoadingMyRequests = true
        myRequestsError = null
        coroutineScope.launch {
            val result = bloodRequestRepository.getRequestsByRequester(currentUid)
            isLoadingMyRequests = false
            if (result.isSuccess) {
                myRequests = result.getOrNull().orEmpty()
            } else {
                myRequestsError = result.exceptionOrNull()?.localizedMessage ?: "Failed to load requests"
            }
        }
    }

    fun loadOpenRequests() {
        isLoadingOpenRequests = true
        openRequestsError = null
        coroutineScope.launch {
            val result = bloodRequestRepository.getOpenRequestsForDonor(
                donorBloodGroup = null,
                donorLocation = null,
                donorUid = currentUid
            )
            isLoadingOpenRequests = false
            if (result.isSuccess) {
                openRequests = result.getOrNull().orEmpty()
            } else {
                openRequestsError = result.exceptionOrNull()?.localizedMessage ?: "Failed to load open requests"
            }
        }
    }

    LaunchedEffect(selectedTab, currentUid) {
        if (selectedTab == 0) {
            loadMyRequests()
        } else {
            loadOpenRequests()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Blood Requests",
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
                    IconButton(onClick = {
                        if (selectedTab == 0) loadMyRequests() else loadOpenRequests()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BloodRed)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRequestClick,
                containerColor = BloodRed,
                contentColor = PureWhite
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Request")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selection Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BloodRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BloodRed
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "My Requests (${myRequests.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Open Requests (${openRequests.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                // Tab 1: My Created Requests
                when {
                    isLoadingMyRequests -> {
                        LoadingView(message = "Loading your blood requests...")
                    }
                    myRequestsError != null -> {
                        ErrorView(
                            errorMessage = myRequestsError!!,
                            onRetry = { loadMyRequests() }
                        )
                    }
                    myRequests.isEmpty() -> {
                        EmptyView(
                            title = "No Requests Created",
                            description = "You haven't submitted any emergency blood requests yet.",
                            actionLabel = "Create Request Now",
                            onActionClick = onCreateRequestClick
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(myRequests, key = { it.requestId }) { request ->
                                BloodRequestItemCard(
                                    request = request,
                                    isRequesterView = true,
                                    onClick = { onRequestClick(request) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 2: Open Requests For Donors
                when {
                    isLoadingOpenRequests -> {
                        LoadingView(message = "Loading available open requests...")
                    }
                    openRequestsError != null -> {
                        ErrorView(
                            errorMessage = openRequestsError!!,
                            onRetry = { loadOpenRequests() }
                        )
                    }
                    openRequests.isEmpty() -> {
                        EmptyView(
                            title = "No Open Requests",
                            description = "There are currently no open blood requests matching your criteria.",
                            actionLabel = "Refresh Directory",
                            onActionClick = { loadOpenRequests() }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BloodRedContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "Tap any open request below to review details and accept or decline.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BloodRed,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            items(openRequests, key = { it.requestId }) { request ->
                                BloodRequestItemCard(
                                    request = request,
                                    isRequesterView = false,
                                    onClick = { onRequestClick(request) }
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
fun BloodRequestItemCard(
    request: BloodRequest,
    isRequesterView: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BloodRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = request.bloodGroup,
                            color = PureWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (request.patientIdentifier.isNotBlank()) request.patientIdentifier else "Patient Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${request.unitsRequired} unit(s) required",
                            style = MaterialTheme.typography.bodySmall,
                            color = BloodRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                StatusBadge(status = request.status)
            }

            // Hospital & Location Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${request.hospitalName}, ${request.hospitalLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Required Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Needed by: ${request.requiredDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val formattedCreated = remember(request.createdAt) {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(request.createdAt))
                }
                Text(
                    text = "Posted: $formattedCreated",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Assigned Donor note if accepted
            if (request.status == RequestStatus.ACCEPTED && !request.assignedDonorName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AvailableGreenBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assigned Donor: ${request.assignedDonorName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AvailableGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: RequestStatus) {
    val (bgColor, textColor) = when (status) {
        RequestStatus.OPEN -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        RequestStatus.ACCEPTED -> AvailableGreenBg to AvailableGreen
        RequestStatus.COMPLETED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        RequestStatus.CANCELLED -> Color(0xFFEEEEEE) to Color(0xFF757575)
        RequestStatus.REJECTED -> Color(0xFFFFEBEE) to BloodRed
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LoadingView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = BloodRed, strokeWidth = 3.dp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Unable to Load Requests",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun EmptyView(
    title: String,
    description: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BloodRedContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
