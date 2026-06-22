package com.govtprep.app.ui.screens.exam

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.model.*
import com.govtprep.app.data.remote.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Represents a sub-exam card (e.g. "SSC GD Constable", "SSC MTS")
data class SubExamInfo(
    val name: String,
    val nameHindi: String?,
    val icon: String?,
    val isActive: Boolean,
    val subjectCount: Int,
    val eligibility: String?
)

data class ExamState(
    val isLoading: Boolean = true,
    val exam: Exam? = null,
    // All subjects (active + inactive) grouped by sub_exam
    val subExams: List<SubExamInfo> = emptyList(),
    // Currently selected sub-exam
    val selectedSubExam: String? = null,
    // Active subjects under selected sub-exam
    val activeSubjects: List<Subject> = emptyList(),
    // Tests grouped by subject
    val testSetsBySubject: Map<String, List<TestSet>> = emptyMap(),
    // Full mock tests
    val mockTests: List<TestSet> = emptyList(),
    // If exam has no sub_exam grouping (flat subjects)
    val isFlat: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExamViewModel @Inject constructor(private val examRepo: ExamRepository) : ViewModel() {
    private val TAG = "ExamVM"
    private val _state = MutableStateFlow(ExamState())
    val state = _state.asStateFlow()
    private var allSubjects: List<Subject> = emptyList()

    fun loadExam(slug: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                examRepo.getExamBySlug(slug).onSuccess { exam ->
                    _state.update { it.copy(exam = exam) }

                    // Fetch ALL subjects (active + inactive) to build sub-exam list
                    examRepo.getAllSubjects(exam.id).onSuccess { subjects ->
                        allSubjects = subjects
                        Log.d(TAG, "All subjects: ${subjects.size}")

                        // Group by sub_exam
                        val grouped = subjects
                            .filter { it.subExam != null }
                            .groupBy { it.subExam!! }

                        if (grouped.isNotEmpty()) {
                            // Build sub-exam info cards
                            val subExams = grouped.map { (name, subs) ->
                                val first = subs.first()
                                SubExamInfo(
                                    name = name,
                                    nameHindi = first.subExamHindi,
                                    icon = first.icon,
                                    isActive = subs.any { it.isActive },
                                    subjectCount = subs.count { it.isActive },
                                    eligibility = null
                                )
                            }.sortedByDescending { it.isActive }

                            Log.d(TAG, "Sub-exams: ${subExams.map { "${it.name} (active=${it.isActive})" }}")
                            _state.update { it.copy(subExams = subExams, isFlat = false) }
                        } else {
                            // No sub_exam grouping — flat list of subjects
                            val active = subjects.filter { it.isActive }
                            _state.update { it.copy(activeSubjects = active, isFlat = true) }
                            loadTestsForSubjects(active)
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "getAllSubjects failed: ${e.message}")
                        _state.update { it.copy(error = e.message ?: "Failed to load subjects") }
                    }
                }.onFailure { e ->
                    Log.e(TAG, "loadExam failed: ${e.message}")
                    _state.update { it.copy(error = e.message ?: "Failed to load exam") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in loadExam: ${e.message}", e)
                _state.update { it.copy(error = e.message ?: "Something went wrong") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectSubExam(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedSubExam = name, isLoading = true) }

            try {
                // Get active subjects for this sub-exam
                val subjects = allSubjects.filter { it.subExam == name && it.isActive }
                Log.d(TAG, "Selected '$name': ${subjects.size} active subjects")
                _state.update { it.copy(activeSubjects = subjects) }

                loadTestsForSubjects(subjects)
            } catch (e: Exception) {
                Log.e(TAG, "selectSubExam failed: ${e.message}", e)
                _state.update { it.copy(error = e.message ?: "Failed to load tests") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearSubExam() {
        _state.update { it.copy(
            selectedSubExam = null,
            activeSubjects = emptyList(),
            testSetsBySubject = emptyMap(),
            mockTests = emptyList()
        )}
    }

    private suspend fun loadTestsForSubjects(subjects: List<Subject>) {
        val testMap = mutableMapOf<String, List<TestSet>>()
        val mocks = mutableListOf<TestSet>()

        subjects.forEach { s ->
            examRepo.getTestSets(s.id).onSuccess { tests ->
                testMap[s.id] = tests.filter { it.testType != "full_mock" }
                mocks.addAll(tests.filter { it.testType == "full_mock" })
                Log.d(TAG, "${s.name}: ${testMap[s.id]?.size} tests, ${mocks.size} mocks total")
            }
        }

        _state.update { it.copy(testSetsBySubject = testMap, mockTests = mocks) }
    }
}
