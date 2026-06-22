package com.govtprep.app.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.remote.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import com.govtprep.app.data.remote.SupabaseModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

enum class SessionCheck { LOADING, LOGGED_IN, NOT_LOGGED_IN }

@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepo: AuthRepository) : ViewModel() {
    private val TAG = "AuthVM"
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _sessionCheck = MutableStateFlow(SessionCheck.LOADING)
    val sessionCheck = _sessionCheck.asStateFlow()

    val isLoggedIn = MutableStateFlow(false)
    val isAdmin = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            SupabaseModule.client.auth.sessionStatus.collect { status ->
                Log.d(TAG, "Session status: $status")
                when (status) {
                    is SessionStatus.Authenticated -> {
                        isLoggedIn.value = true
                        _sessionCheck.value = SessionCheck.LOGGED_IN
                        Log.d(TAG, "Authenticated: ${status.session.user?.id}")
                        checkAdminStatus()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        isLoggedIn.value = false
                        isAdmin.value = false
                        _sessionCheck.value = SessionCheck.NOT_LOGGED_IN
                        Log.d(TAG, "Not authenticated")
                    }
                    is SessionStatus.Initializing -> {
                        _sessionCheck.value = SessionCheck.LOADING
                        Log.d(TAG, "Initializing session...")
                    }
                    is SessionStatus.RefreshFailure -> {
                        isLoggedIn.value = false
                        isAdmin.value = false
                        _sessionCheck.value = SessionCheck.NOT_LOGGED_IN
                        Log.d(TAG, "Refresh failed")
                    }
                }
            }
        }
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            try {
                authRepo.getProfile().onSuccess { profile ->
                    isAdmin.value = profile.isAdmin
                    Log.d(TAG, "Admin status: ${profile.isAdmin}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkAdmin failed: ${e.message}")
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields"); return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepo.signIn(email, password)
                .onSuccess { _authState.value = AuthState.Success; isLoggedIn.value = true }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Login failed") }
        }
    }

    fun signUp(email: String, password: String, fullName: String, referralCode: String = "") {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields"); return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters"); return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepo.signUp(email, password, fullName, referralCode)
                .onSuccess { _authState.value = AuthState.Success; isLoggedIn.value = true }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Signup failed") }
        }
    }

    fun signOut() {
        _sessionCheck.value = SessionCheck.NOT_LOGGED_IN
        _authState.value = AuthState.Idle
        isLoggedIn.value = false
        isAdmin.value = false
        viewModelScope.launch {
            try { authRepo.signOut() } catch (_: Exception) {}
        }
    }
}
