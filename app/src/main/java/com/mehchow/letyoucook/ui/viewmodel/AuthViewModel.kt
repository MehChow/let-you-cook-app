package com.mehchow.letyoucook.ui.viewmodel

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mehchow.letyoucook.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

// responsible for handling authentication logic and exposing state to the AuthScreen composable
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @Named("web_client_id") private val webClientId: String
): ViewModel() {
    // StateFlow<AuthUiState> is a state observable that when changed, triggers recomposition in Compose
    // ViewModel is the single source of truth for that screen's state. Read and write in ViewModel, and read-only outside ViewModel
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // Activity is needed here since CredentialManager requires it
    fun onGoogleSignIn(activity: ComponentActivity) {
        // avoid starting multiple login at once
        if (_uiState.value is AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading

        // starts an async effect that is automatically canceled when ViewModel is cleared (screen goes away)
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d("AuthViewModel", "Requesting Google credentials...")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Log.d("AuthViewModel", "Google token received: ${idToken.take(10)}...")

                // call repository (which wraps Retrofit)
                val repositoryResult = authRepository.loginWithGoogle(idToken)
                repositoryResult
                    .onSuccess { authResponse ->
                    Log.d("AuthViewModel", "Backend auth success: ${authResponse.username}")
                    _uiState.value = AuthUiState.Success(authResponse)
                }
                    .onFailure { throwable ->
                        Log.e("AuthViewModel", "Backend auth failed", throwable)
                        _uiState.value = AuthUiState.Error(
                            message = throwable.message ?: "Unknown error occurred"
                        )
                    }
            } catch (e: Exception) {
                if (e is GetCredentialCancellationException) {
                    Log.d("AuthViewModel", "User cancelled the Google Sign-In")
                    // Just reset to Idle so the button becomes clickable again
                    // without showing a Toast
                    _uiState.value = AuthUiState.Idle
                } else {
                    Log.e("AuthViewModel", "Unexpected auth error", e)
                    _uiState.value = AuthUiState.Error(
                        message = "Login failed: ${e.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}