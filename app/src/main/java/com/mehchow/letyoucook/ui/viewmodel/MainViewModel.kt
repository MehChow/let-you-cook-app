package com.mehchow.letyoucook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.local.ThemePreferences
import com.mehchow.letyoucook.data.local.TokenManager
import com.mehchow.letyoucook.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    // Auth state - transforms the Flow from TokenManager into AuthState
    val authState: StateFlow<AuthState> = tokenManager.accessToken
        .map { token ->
            if (token != null) {
                // Since we only have the token here, we can fetch the full user
                // or just emit an Authenticated state with the token
                val user = authRepository.getCurrentUser()
                if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
            } else {
                AuthState.Unauthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    // Theme state - null means follow system, true = dark, false = light
    val isDarkTheme: StateFlow<Boolean?> = themePreferences.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Toggle theme between light and dark.
     * @param currentIsDark The current effective dark theme state (after applying system default)
     */
    fun toggleTheme(currentIsDark: Boolean) {
        viewModelScope.launch {
            themePreferences.setDarkTheme(!currentIsDark)
        }
    }
}