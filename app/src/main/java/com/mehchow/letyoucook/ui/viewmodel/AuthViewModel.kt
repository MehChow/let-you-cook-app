package com.mehchow.letyoucook.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// responsible for handling authentication logic and exposing state to the AuthScreen composable
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
): ViewModel() {
    // StateFlow<AuthUiState> is a state observable that when changed, triggers recomposition in Compose
    // ViewModel is the single source of truth for that screen's state. Read and write in ViewModel, and read-only outside ViewModel
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun authenticateWithBackend(idToken: String) {
        if (_uiState.value is AuthUiState.Loading) return

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(idToken)
            result
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
        }
    }
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}