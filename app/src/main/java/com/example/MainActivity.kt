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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.AppPreferences
import com.example.data.repository.TradingRepository
import com.example.ui.EdgeLogApp
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.signOutFromGoogle
import com.example.ui.state.EdgeLogViewModel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EdgeLogTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = AppPreferences(applicationContext)

        setContent {
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.DARK)
            EdgeLogTheme(themeMode = themeMode) {
                EdgeLogRoot(preferences = preferences)
            }
        }
    }
}

@Composable
private fun EdgeLogRoot(preferences: AppPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val repository = remember(uid) {
                val appContext = context.applicationContext
                val db = FirebaseFirestore.getInstance(
                    appContext.getString(R.string.firestore_database_id)
                )
                TradingRepository(
                    context = appContext,
                    db = db,
                    preferences = preferences
                )
            }
            val vmFactory = remember(uid) {
                EdgeLogViewModel.Factory(
                    repository = repository,
                    currentUserId = uid,
                    userDisplayName = user.displayName,
                    userEmail = user.email
                )
            }
            val viewModel: EdgeLogViewModel = viewModel(
                key = uid,
                factory = vmFactory
            )
            EdgeLogApp(
                viewModel = viewModel,
                onSignOut = {
                    signOutFromGoogle(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}
