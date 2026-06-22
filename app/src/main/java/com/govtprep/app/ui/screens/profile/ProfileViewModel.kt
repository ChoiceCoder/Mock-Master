package com.govtprep.app.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.remote.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileEditState(
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {
    private val TAG = "ProfileVM"
    private val _state = MutableStateFlow(ProfileEditState())
    val state = _state.asStateFlow()

    fun saveProfile(name: String, phone: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            try {
                val updates = mutableMapOf<String, String>()
                if (name.isNotBlank()) updates["full_name"] = name
                if (phone.isNotBlank()) updates["phone"] = phone

                authRepo.updateProfile(updates)
                    .onSuccess {
                        Log.d(TAG, "Profile updated")
                        _state.update { it.copy(isSaving = false, message = "Profile updated successfully") }
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Update failed: ${e.message}")
                        _state.update { it.copy(isSaving = false, message = "Failed: ${e.message}") }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, message = "Error: ${e.message}") }
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            authRepo.changePassword(newPassword)
                .onSuccess {
                    Log.d(TAG, "Password changed")
                    _state.update { it.copy(isSaving = false, message = "Password changed successfully") }
                }
                .onFailure { e ->
                    Log.e(TAG, "Password change failed: ${e.message}")
                    _state.update { it.copy(isSaving = false, message = "Failed: ${e.message}") }
                }
        }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}
