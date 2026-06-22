package com.govtprep.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.model.*
import com.govtprep.app.data.remote.AdminRepository
import com.govtprep.app.data.remote.AuthRepository
import com.govtprep.app.data.remote.ExamRepository
import com.govtprep.app.data.remote.TestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import java.util.Calendar
import javax.inject.Inject

data class HomeState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val exams: List<Exam> = emptyList(),
    val stats: UserStats? = null,
    val recentAttempts: List<Attempt> = emptyList(),
    val greetingHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val feedbackResult: String? = null,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val examRepo: ExamRepository,
    private val testRepo: TestRepository,
    private val adminRepo: AdminRepository,
) : ViewModel() {
    private val TAG = "HomeVM"
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, greetingHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

            authRepo.getProfile()
                .onSuccess { p -> _state.update { it.copy(profile = p) } }
                .onFailure { Log.e(TAG, "Profile: ${it.message}") }

            examRepo.getActiveExams()
                .onSuccess { e -> _state.update { it.copy(exams = e) } }
                .onFailure { _state.update { it.copy(error = "Could not load exams") } }

            testRepo.getUserStats()
                .onSuccess { s -> _state.update { it.copy(stats = s) } }
                .onFailure { _state.update { it.copy(stats = UserStats()) } }

            testRepo.getRecentAttempts(5)
                .onSuccess { a -> _state.update { it.copy(recentAttempts = a) } }
                .onFailure { Log.e(TAG, "Attempts: ${it.message}") }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun submitFeedback(message: String) {
        viewModelScope.launch {
            try {
                val profile = _state.value.profile
                val data = buildMap<String, kotlinx.serialization.json.JsonElement> {
                    put("user_name", JsonPrimitive(profile?.fullName ?: "Anonymous"))
                    put("user_email", JsonPrimitive(profile?.email ?: ""))
                    put("message", JsonPrimitive(message))
                    put("status", JsonPrimitive("new"))
                }
                adminRepo.submitFeedback(data).onSuccess {
                    _state.update { it.copy(feedbackResult = "success") }
                }.onFailure { e ->
                    android.util.Log.e(TAG, "Feedback failed: ${e.message}", e)
                    // Show actual error so user can report it
                    _state.update { it.copy(feedbackResult = "error:${e.message?.take(80) ?: "Unknown error"}") }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Feedback exception: ${e.message}", e)
                _state.update { it.copy(feedbackResult = "error:${e.message?.take(80) ?: "Unknown error"}") }
            }
        }
    }

    fun clearFeedbackResult() {
        _state.update { it.copy(feedbackResult = null) }
    }

}
