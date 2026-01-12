package com.mehchow.letyoucook.ui.viewmodel

import com.mehchow.letyoucook.data.model.AuthResponse

// sealed class lets you define a closed set of subtypes and use "when" exhaustively
sealed class AuthUiState {
    data object Idle: AuthUiState()

    data object Loading: AuthUiState()

    data class Success(
        val authResponse: AuthResponse
    ): AuthUiState()

    data class Error(
        val message: String
    ): AuthUiState()
}