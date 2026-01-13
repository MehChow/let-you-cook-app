package com.mehchow.letyoucook.ui.viewmodel

import com.mehchow.letyoucook.data.model.AuthResponse

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: AuthResponse) : AuthState()
    object Unauthenticated : AuthState()
}