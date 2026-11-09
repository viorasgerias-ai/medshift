package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CloudShiftRepository
import com.example.data.ShiftRepository
import com.example.firebase.configureAppCheck
import com.example.ui.AuthScreen
import com.example.ui.GovernanceCasesScreen
import com.example.ui.MarketingPortalScreen
import com.example.ui.MedicalShiftsScreen
import com.example.ui.ShiftViewModel
import com.example.ui.ShiftViewModelFactory
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class MedShiftDestination(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    SHIFTS("Plantões", Icons.Default.MedicalServices, "nav_tab_shifts"),
    GOVERNANCE("Governança TCRIA", Icons.Default.VerifiedUser, "nav_tab_governance"),
    PORTAL("Médicos a Postos", Icons.Default.Campaign, "nav_tab_portal")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            val database = remember(context) { AppDatabase.getDatabase(context.applicationContext) }
            val roomRepository = remember(database) { ShiftRepository(database.shiftDao()) }
            val cloudRepository = remember(context) {
                val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
                CloudShiftRepository(FirebaseFirestore.getInstance(databaseId))
            }
            val shiftViewModel: ShiftViewModel = viewModel(
                key = uid,
                factory = ShiftViewModelFactory(roomRepository, cloudRepository)
            )

            var currentTab by rememberSaveable { mutableStateOf(MedShiftDestination.SHIFTS) }

            BackHandler(enabled = currentTab != MedShiftDestination.SHIFTS) {
                currentTab = MedShiftDestination.SHIFTS
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("main_bottom_navigation")
                    ) {
                        MedShiftDestination.entries.forEach { destination ->
                            val selected = currentTab == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF087F67),
                                    selectedTextColor = Color(0xFF087F67),
                                    indicatorColor = Color(0xFFD9F3EC)
                                ),
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    when (currentTab) {
                        MedShiftDestination.SHIFTS -> {
                            MedicalShiftsScreen(
                                viewModel = shiftViewModel,
                                onSignOut = {
                                    signOut(
                                        credentialManager = credentialManager,
                                        scope = coroutineScope,
                                        onSignOutComplete = { currentUser = null }
                                    )
                                }
                            )
                        }
                        MedShiftDestination.GOVERNANCE -> {
                            GovernanceCasesScreen(
                                viewModel = shiftViewModel
                            )
                        }
                        MedShiftDestination.PORTAL -> {
                            MarketingPortalScreen(
                                onNavigateToGovernance = { currentTab = MedShiftDestination.GOVERNANCE },
                                onNavigateToShifts = { currentTab = MedShiftDestination.SHIFTS }
                            )
                        }
                    }
                }
            }
        }
    }
}

