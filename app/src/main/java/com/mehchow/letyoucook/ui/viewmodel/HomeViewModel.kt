package com.mehchow.letyoucook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun onLogoutClick() {
        viewModelScope.launch {
            authRepository.logout()
            // The AppNavigation will automatically react to the cleared DataStore
        }
    }
}