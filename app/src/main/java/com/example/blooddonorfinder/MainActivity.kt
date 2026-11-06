package com.example.blooddonorfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.blooddonorfinder.model.BloodRequest
import com.example.blooddonorfinder.repository.DonorRepository
import com.example.blooddonorfinder.repository.FirebaseAuthRepository
import com.example.blooddonorfinder.repository.FirebaseBloodRequestRepository
import com.example.blooddonorfinder.repository.FirebaseDonorRepository
import com.example.blooddonorfinder.ui.navigation.Screen
import com.example.blooddonorfinder.ui.screens.CreateRequestScreen
import com.example.blooddonorfinder.ui.screens.DonorRegisterScreen
import com.example.blooddonorfinder.ui.screens.DonorSearchScreen
import com.example.blooddonorfinder.ui.screens.HomeScreen
import com.example.blooddonorfinder.ui.screens.RequestDetailScreen
import com.example.blooddonorfinder.ui.screens.RequestListScreen
import com.example.blooddonorfinder.ui.theme.BloodDonorFinderTheme
import com.example.blooddonorfinder.ui.theme.BloodRed
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloodDonorFinderTheme {
                BloodDonorApp()
            }
        }
    }
}

@Composable
fun BloodDonorApp(
    donorRepository: DonorRepository = remember { DonorRepository() },
    firebaseDonorRepository: FirebaseDonorRepository = remember { FirebaseDonorRepository() },
    firebaseBloodRequestRepository: FirebaseBloodRequestRepository = remember { FirebaseBloodRequestRepository() },
    firebaseAuthRepository: FirebaseAuthRepository = remember { FirebaseAuthRepository() }
) {
    val coroutineScope = rememberCoroutineScope()

    // Current navigation state
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }

    // Selected blood request for details view
    var selectedRequest by remember { mutableStateOf<BloodRequest?>(null) }

    // Authenticated user UID state
    var currentUid by rememberSaveable {
        mutableStateOf(firebaseAuthRepository.currentUserId ?: "")
    }

    // Ensure user has an active authenticated session for Firestore operations
    LaunchedEffect(Unit) {
        if (currentUid.isBlank()) {
            val authResult = firebaseAuthRepository.ensureAuthenticated()
            if (authResult.isSuccess) {
                currentUid = authResult.getOrNull() ?: ""
            }
        }
    }

    // Dialog state for Firebase connection verification test
    var connectionDialogMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingConnection by remember { mutableStateOf(false) }

    // Handle system back button
    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = when (currentScreen) {
            Screen.REQUEST_DETAIL -> Screen.REQUEST_LIST
            else -> Screen.HOME
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = currentScreen,
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                Screen.HOME -> {
                    HomeScreen(
                        registeredCount = donorRepository.getDonorCount(),
                        onNavigateToSearch = { currentScreen = Screen.SEARCH },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER },
                        onNavigateToCreateRequest = { currentScreen = Screen.CREATE_REQUEST },
                        onNavigateToRequests = { currentScreen = Screen.REQUEST_LIST },
                        onVerifyFirebaseConnection = {
                            if (!isCheckingConnection) {
                                isCheckingConnection = true
                                coroutineScope.launch {
                                    val result = firebaseDonorRepository.checkConnection()
                                    isCheckingConnection = false
                                    connectionDialogMessage = if (result.isSuccess) {
                                        "Firebase Cloud Firestore is successfully connected and responding to queries."
                                    } else {
                                        result.exceptionOrNull()?.localizedMessage
                                            ?: "Unable to connect to Cloud Firestore."
                                    }
                                }
                            }
                        }
                    )
                }

                Screen.SEARCH -> {
                    DonorSearchScreen(
                        donorRepository = donorRepository,
                        onNavigateBack = { currentScreen = Screen.HOME },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER },
                        onCloudSearch = { bloodGroup, location ->
                            // 1. Query Cloud Firestore
                            val cloudResult = firebaseDonorRepository.searchDonors(
                                bloodGroup = bloodGroup,
                                locationQuery = location,
                                requireAvailable = true
                            )

                            // 2. If cloud query returns records, sync with local session
                            if (cloudResult.isSuccess) {
                                val cloudDonors = cloudResult.getOrNull().orEmpty()
                                cloudDonors.forEach { cloudDonor ->
                                    if (donorRepository.getAllDonors().none { it.id == cloudDonor.id }) {
                                        donorRepository.addDonor(cloudDonor)
                                    }
                                }
                            }
                            cloudResult
                        }
                    )
                }

                Screen.REGISTER -> {
                    DonorRegisterScreen(
                        onNavigateBack = { currentScreen = Screen.HOME },
                        onNavigateToSearch = { currentScreen = Screen.SEARCH },
                        onRegisterDonor = { newDonor ->
                            // 1. Save to in-memory repository to guarantee instant session responsiveness
                            donorRepository.addDonor(newDonor)

                            // 2. Create donor document in Cloud Firestore
                            val cloudResult = firebaseDonorRepository.createDonor(newDonor)
                            cloudResult
                        }
                    )
                }

                Screen.CREATE_REQUEST -> {
                    CreateRequestScreen(
                        currentRequesterUid = currentUid,
                        onNavigateBack = { currentScreen = Screen.HOME },
                        onNavigateToMyRequests = { currentScreen = Screen.REQUEST_LIST },
                        onSubmitRequest = { newRequest ->
                            firebaseBloodRequestRepository.createBloodRequest(newRequest)
                        }
                    )
                }

                Screen.REQUEST_LIST -> {
                    RequestListScreen(
                        currentUid = currentUid,
                        bloodRequestRepository = firebaseBloodRequestRepository,
                        onNavigateBack = { currentScreen = Screen.HOME },
                        onCreateRequestClick = { currentScreen = Screen.CREATE_REQUEST },
                        onRequestClick = { request ->
                            selectedRequest = request
                            currentScreen = Screen.REQUEST_DETAIL
                        }
                    )
                }

                Screen.REQUEST_DETAIL -> {
                    val req = selectedRequest
                    if (req != null) {
                        RequestDetailScreen(
                            initialRequest = req,
                            currentUid = currentUid,
                            bloodRequestRepository = firebaseBloodRequestRepository,
                            onNavigateBack = { currentScreen = Screen.REQUEST_LIST }
                        )
                    } else {
                        currentScreen = Screen.REQUEST_LIST
                    }
                }
            }
        }
    }

    // Firebase Connection Status Verification Dialog
    if (connectionDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { connectionDialogMessage = null },
            title = {
                Text(
                    text = "Firebase Connection Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = connectionDialogMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { connectionDialogMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BloodDonorAppPreview() {
    BloodDonorFinderTheme {
        BloodDonorApp()
    }
}