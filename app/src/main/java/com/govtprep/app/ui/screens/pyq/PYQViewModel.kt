package com.govtprep.app.ui.screens.pyq

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.model.*
import com.govtprep.app.data.remote.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PYQLevel { EXAMS, SUB_EXAMS, YEARS, TESTS }

data class PYQSubExam(
    val name: String,
    val nameHindi: String? = null,
    val icon: String?,
    val isActive: Boolean,
    val paperCount: Int
)

data class PYQState(
    val isLoading: Boolean = true,
    val level: PYQLevel = PYQLevel.EXAMS,
    val exams: List<Exam> = emptyList(),
    val selectedExam: Exam? = null,
    val subExams: List<PYQSubExam> = emptyList(),
    val isFlat: Boolean = false,
    val selectedSubExam: String? = null,
    val allPyqTests: List<TestSet> = emptyList(),
    val filteredTests: List<TestSet> = emptyList(),
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val testsForYear: List<TestSet> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class PYQViewModel @Inject constructor(
    private val examRepo: ExamRepository
) : ViewModel() {
    private val TAG = "PYQVM"
    private val _state = MutableStateFlow(PYQState())
    val state = _state.asStateFlow()

    init { loadExams() }

    private fun loadExams() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            examRepo.getActiveExams()
                .onSuccess { exams -> _state.update { it.copy(exams = exams) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun selectExam(exam: Exam) {
        _state.update { it.copy(selectedExam = exam, isLoading = true) }
        viewModelScope.launch {
            // Fetch PYQ test_sets directly by exam_id — no subjects involved
            examRepo.getPYQTestSetsByExam(exam.id).onSuccess { tests ->
                _state.update { it.copy(allPyqTests = tests) }

                // Group by sub_exam_name to build sub-exam picker
                val groups = tests.groupBy { it.subExamName ?: "General" }

                if (groups.size > 1 || (groups.size == 1 && groups.keys.first() != "General")) {
                    val subExams = groups.map { (name, papers) ->
                        PYQSubExam(
                            name = name,
                            icon = null,
                            isActive = papers.isNotEmpty(),
                            paperCount = papers.size
                        )
                    }.sortedByDescending { it.paperCount }
                    _state.update { it.copy(subExams = subExams, isFlat = false, level = PYQLevel.SUB_EXAMS) }
                } else {
                    // No sub-exam grouping — skip to years
                    val years = tests.mapNotNull { it.year }.distinct().sortedDescending()
                    _state.update { it.copy(
                        filteredTests = tests, years = years,
                        isFlat = true, level = PYQLevel.YEARS
                    ) }
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message, level = PYQLevel.SUB_EXAMS) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun selectSubExam(name: String) {
        val tests = _state.value.allPyqTests.filter { (it.subExamName ?: "General") == name }
        val years = tests.mapNotNull { it.year }.distinct().sortedDescending()
        _state.update { it.copy(
            selectedSubExam = name, filteredTests = tests,
            years = years, level = PYQLevel.YEARS
        ) }
    }

    fun selectYear(year: Int) {
        val tests = _state.value.filteredTests.filter { it.year == year }
        _state.update { it.copy(selectedYear = year, testsForYear = tests, level = PYQLevel.TESTS) }
    }

    fun goBack() {
        when (_state.value.level) {
            PYQLevel.TESTS -> _state.update { it.copy(
                selectedYear = null, testsForYear = emptyList(), level = PYQLevel.YEARS
            )}
            PYQLevel.YEARS -> {
                if (_state.value.isFlat) {
                    _state.update { it.copy(
                        selectedExam = null, allPyqTests = emptyList(), filteredTests = emptyList(),
                        years = emptyList(), isFlat = false, level = PYQLevel.EXAMS
                    )}
                } else {
                    _state.update { it.copy(
                        selectedSubExam = null, filteredTests = emptyList(),
                        years = emptyList(), level = PYQLevel.SUB_EXAMS
                    )}
                }
            }
            PYQLevel.SUB_EXAMS -> {
                _state.update { it.copy(
                    selectedExam = null, subExams = emptyList(),
                    allPyqTests = emptyList(), level = PYQLevel.EXAMS
                )}
            }
            else -> {}
        }
    }
}
