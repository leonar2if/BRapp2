package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.Profile
import com.example.data.repository.AuthRepository
import com.example.utils.ErrorTranslator
import com.example.utils.Validators
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val profile: Profile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepo = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // False until an app-start session restore attempt has finished (whether or not
    // there was a session to restore). AppNavigation should wait for this before
    // deciding whether to route to login or straight to a home screen, otherwise it
    // may navigate to a "logged in" screen whose API calls have no valid token yet.
    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady

    init {
        viewModelScope.launch {
            val hasPersistedSession = authRepo.userId.first().isNotEmpty()
            if (hasPersistedSession) {
                authRepo.restoreSession()
            }
            _sessionReady.value = true
        }
    }

    val isDarkMode: StateFlow<Boolean> = authRepo.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userRole: StateFlow<String> = authRepo.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userId: StateFlow<String> = authRepo.userId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userPhone: StateFlow<String> = authRepo.userPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userFullName: StateFlow<String> = authRepo.userFullName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun login(phone: String, pass: String) {
        if (!Validators.isValidPhone(phone)) {
            _authState.value = AuthState.Error("Por favor, introduce un número de teléfono válido.")
            return
        }
        if (!Validators.isValidPassword(pass)) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val res = authRepo.login(phone, pass)
            val profile = res.getOrNull()
            _authState.value = if (res.isSuccess && profile != null) {
                AuthState.Success(profile)
            } else {
                AuthState.Error(ErrorTranslator.toHumanMessage(res.exceptionOrNull()))
            }
        }
    }

    fun register(phone: String, fullName: String, pass: String, confirmPass: String) {
        if (!Validators.isValidPhone(phone)) {
            _authState.value = AuthState.Error("Por favor, introduce un número de teléfono válido.")
            return
        }
        if (!Validators.isValidName(fullName)) {
            _authState.value = AuthState.Error("Por favor, introduce tu nombre completo.")
            return
        }
        if (!Validators.isValidPassword(pass)) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        if (pass != confirmPass) {
            _authState.value = AuthState.Error("Las contraseñas no coinciden.")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val res = authRepo.register(phone, fullName, pass)
            val profile = res.getOrNull()
            _authState.value = if (res.isSuccess && profile != null) {
                AuthState.Success(profile)
            } else {
                AuthState.Error(ErrorTranslator.toHumanMessage(res.exceptionOrNull()))
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            authRepo.setDarkMode(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
