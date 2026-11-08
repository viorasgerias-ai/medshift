package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CloudShiftRepository
import com.example.data.ShiftRepository
import com.example.firebase.configureAppCheck
import com.example.ui.AuthScreen
import com.example.ui.MedicalShiftsScreen
import com.example.ui.ShiftViewModel
import com.example.ui.ShiftViewModelFactory
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

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
    }
}
