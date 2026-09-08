package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth? = initFirebaseAuth(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    private fun initFirebaseAuth(context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.financetracker.pqxzwy")
                    .setApiKey("AIzaSyExampleKeyFinanceTrackerSafeDefault")
                    .setProjectId("finance-tracker-app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            try {
                FirebaseAuth.getInstance()
            } catch (ex: Exception) {
                null
            }
        }
    }

    fun isAuthAvailable(): Boolean = auth != null

    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val fbAuth = auth ?: run {
            onError("Firebase Auth not initialized")
            return
        }
        fbAuth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                result.user?.let {
                    _currentUser.value = it
                    onSuccess(it)
                } ?: onError("User session not found")
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Authentication failed")
            }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val fbAuth = auth ?: run {
            onError("Firebase Auth not initialized")
            return
        }
        fbAuth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                result.user?.let {
                    _currentUser.value = it
                    onSuccess(it)
                } ?: onError("Account creation failed")
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Registration failed")
            }
    }

    fun signInAnonymously(
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val fbAuth = auth ?: run {
            onError("Firebase Auth not initialized")
            return
        }
        fbAuth.signInAnonymously()
            .addOnSuccessListener { result ->
                result.user?.let {
                    _currentUser.value = it
                    onSuccess(it)
                } ?: onError("Anonymous authentication failed")
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Failed to sign in anonymously")
            }
    }

    suspend fun signInWithGoogleCredentialManager(
        context: Context,
        serverClientId: String = "YOUR_WEB_CLIENT_ID",
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val fbAuth = auth ?: run {
            onError("Firebase Auth not initialized")
            return
        }

        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                fbAuth.signInWithCredential(authCredential)
                    .addOnSuccessListener { authResult ->
                        authResult.user?.let {
                            _currentUser.value = it
                            onSuccess(it)
                        } ?: onError("Google authentication failed")
                    }
                    .addOnFailureListener { err ->
                        onError(err.localizedMessage ?: "Firebase Google sign-in failed")
                    }
            } else {
                onError("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialException) {
            onError(e.localizedMessage ?: "Credential Manager error")
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Google Sign-In failed")
        }
    }

    fun signOut() {
        auth?.signOut()
        _currentUser.value = null
    }
}
