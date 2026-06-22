package com.govtprep.app.ui.screens.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.model.*
import com.govtprep.app.data.remote.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject

// Drill-down: EXAMS → SUB_EXAMS → SUBJECTS → TEST_SETS → QUESTIONS
enum class AdminLevel { EXAMS, SUB_EXAMS, SUBJECTS, TEST_SETS, QUESTIONS }

data class SubExamGroup(
    val name: String,
    val nameHindi: String?,
    val icon: String?,
    val isActive: Boolean,
    val subjectCount: Int
)

data class AdminState(
    val isLoading: Boolean = false,
    val level: AdminLevel = AdminLevel.EXAMS,
    val searchQuery: String = "",

    // Data
    val exams: List<Exam> = emptyList(),
    val subExamGroups: List<SubExamGroup> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val testSets: List<TestSet> = emptyList(),
    val fullMockTests: List<TestSet> = emptyList(),
    val questions: List<AdminQuestion> = emptyList(),

    // All subjects cache (for grouping)
    val allSubjectsForExam: List<Subject> = emptyList(),

    // Selection breadcrumbs
    val selectedExam: Exam? = null,
    val selectedSubExam: String? = null,
    val selectedSubject: Subject? = null,
    val selectedTestSet: TestSet? = null,

    // Dialog
    val showDialog: Boolean = false,
    val dialogType: DialogType = DialogType.NONE,
    val editingItem: Any? = null,

    // Delete confirm
    val showDeleteConfirm: Boolean = false,
    val deleteItemId: String? = null,
    val deleteItemName: String = "",
    val deleteIsMock: Boolean = false,

    // Feedback
    val snackbarMessage: String? = null,
    val error: String? = null,

    // Feedback
    val showFeedback: Boolean = false,
    val feedbackList: List<com.govtprep.app.data.model.Feedback> = emptyList(),

    // PYQ Management
    val showPYQ: Boolean = false,
    val pyqTestSets: List<TestSet> = emptyList(),
    val pyqSelectedTestSet: TestSet? = null,
    val pyqQuestions: List<AdminQuestion> = emptyList(),
)

enum class DialogType { NONE, EXAM, SUBJECT, TEST_SET, QUESTION }

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepo: AdminRepository
) : ViewModel() {
    private val TAG = "AdminVM"
    private val _state = MutableStateFlow(AdminState())
    val state = _state.asStateFlow()

    init {
        loadExams()
        // Pre-load feedback for badge count
        viewModelScope.launch {
            adminRepo.getAllFeedback().onSuccess { list ->
                _state.update { it.copy(feedbackList = list) }
            }
        }
        // Pre-load PYQ count for badge
        viewModelScope.launch {
            adminRepo.getAllPYQTestSets().onSuccess { tests ->
                _state.update { it.copy(pyqTestSets = tests) }
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Navigation
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun selectExam(exam: Exam) {
        _state.update { it.copy(selectedExam = exam, level = AdminLevel.SUB_EXAMS, searchQuery = "") }
        loadSubExams(exam.id)
    }

    fun selectSubExam(name: String) {
        val allSubs = _state.value.allSubjectsForExam
        val subjects = allSubs.filter { it.subExam == name }
        _state.update { it.copy(
            selectedSubExam = name,
            level = AdminLevel.SUBJECTS,
            subjects = subjects,
            searchQuery = ""
        ) }
        // Also load full mock test sets for these subjects
        loadFullMockTests(subjects.map { it.id })
    }

    private fun loadFullMockTests(subjectIds: List<String>) {
        viewModelScope.launch {
            val mocks = mutableListOf<TestSet>()
            subjectIds.forEach { id ->
                adminRepo.getTestSetsForSubject(id).onSuccess { tests ->
                    mocks.addAll(tests.filter { it.testType == "full_mock" })
                }
            }
            _state.update { it.copy(fullMockTests = mocks) }
        }
    }

    fun selectSubject(subject: Subject) {
        _state.update { it.copy(selectedSubject = subject, level = AdminLevel.TEST_SETS, searchQuery = "") }
        loadTestSets(subject.id)
    }

    fun selectTestSet(testSet: TestSet) {
        _state.update { it.copy(selectedTestSet = testSet, level = AdminLevel.QUESTIONS, searchQuery = "") }
        loadQuestions(testSet.id)
    }

    fun goBack() {
        val s = _state.value
        when (s.level) {
            AdminLevel.QUESTIONS -> {
                _state.update { it.copy(selectedTestSet = null, level = if (s.selectedSubject != null) AdminLevel.TEST_SETS else AdminLevel.SUBJECTS,
                    searchQuery = "", questions = emptyList()) }
                // If returning to subjects, re-filter and reload mocks
                if (s.selectedSubject == null && s.selectedSubExam != null) {
                    val subjects = s.allSubjectsForExam.filter { it.subExam == s.selectedSubExam }
                    _state.update { it.copy(subjects = subjects) }
                    loadFullMockTests(subjects.map { it.id })
                }
            }
            AdminLevel.TEST_SETS -> {
                _state.update { it.copy(selectedSubject = null, level = AdminLevel.SUBJECTS, searchQuery = "", testSets = emptyList()) }
                // Re-filter subjects for current sub-exam
                val subExam = s.selectedSubExam
                if (subExam != null) {
                    val subjects = s.allSubjectsForExam.filter { it.subExam == subExam }
                    _state.update { it.copy(subjects = subjects) }
                }
            }
            AdminLevel.SUBJECTS -> {
                _state.update { it.copy(selectedSubExam = null, level = AdminLevel.SUB_EXAMS, searchQuery = "", subjects = emptyList(), fullMockTests = emptyList()) }
            }
            AdminLevel.SUB_EXAMS -> {
                _state.update { it.copy(selectedExam = null, level = AdminLevel.EXAMS, searchQuery = "",
                    subExamGroups = emptyList(), allSubjectsForExam = emptyList()) }
            }
            else -> {}
        }
    }

    fun updateSearch(q: String) { _state.update { it.copy(searchQuery = q) } }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Loading
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun loadExams() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getAllExams().onSuccess { exams ->
                _state.update { it.copy(exams = exams) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadSubExams(examId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getSubjectsForExam(examId).onSuccess { allSubs ->
                _state.update { it.copy(allSubjectsForExam = allSubs) }

                val grouped = allSubs
                    .filter { it.subExam != null }
                    .groupBy { it.subExam!! }

                if (grouped.isNotEmpty()) {
                    val groups = grouped.map { (name, subs) ->
                        val first = subs.first()
                        SubExamGroup(
                            name = name,
                            nameHindi = first.subExamHindi,
                            icon = first.icon,
                            isActive = subs.any { it.isActive },
                            subjectCount = subs.size
                        )
                    }.sortedBy { if (it.isActive) 0 else 1 }
                    _state.update { it.copy(subExamGroups = groups) }
                } else {
                    // No sub-exam grouping — go directly to subjects
                    _state.update { it.copy(
                        level = AdminLevel.SUBJECTS,
                        subjects = allSubs,
                        selectedSubExam = "All"
                    ) }
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadTestSets(subjectId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getTestSetsForSubject(subjectId).onSuccess { tests ->
                _state.update { it.copy(testSets = tests) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun loadQuestions(testSetId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getQuestionsForTestSet(testSetId).onSuccess { questions ->
                _state.update { it.copy(questions = questions) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Dialog management
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun showAddDialog() {
        val type = when (_state.value.level) {
            AdminLevel.EXAMS -> DialogType.EXAM
            AdminLevel.SUB_EXAMS -> DialogType.SUBJECT // adding a subject under this exam
            AdminLevel.SUBJECTS -> DialogType.SUBJECT
            AdminLevel.TEST_SETS -> DialogType.TEST_SET
            AdminLevel.QUESTIONS -> DialogType.QUESTION
        }
        _state.update { it.copy(showDialog = true, dialogType = type, editingItem = null) }
    }

    fun showEditDialog(item: Any) {
        val type = when (item) {
            is Exam -> DialogType.EXAM
            is Subject -> DialogType.SUBJECT
            is TestSet -> DialogType.TEST_SET
            is AdminQuestion -> DialogType.QUESTION
            else -> return
        }
        _state.update { it.copy(showDialog = true, dialogType = type, editingItem = item) }
    }

    fun dismissDialog() {
        _state.update { it.copy(showDialog = false, editingItem = null, dialogType = DialogType.NONE) }
    }

    fun showDeleteConfirmation(id: String, name: String) {
        _state.update { it.copy(showDeleteConfirm = true, deleteItemId = id, deleteItemName = name, deleteIsMock = false) }
    }

    fun showDeleteMockConfirmation(id: String, name: String) {
        _state.update { it.copy(showDeleteConfirm = true, deleteItemId = id, deleteItemName = name, deleteIsMock = true) }
    }

    fun dismissDeleteConfirm() {
        _state.update { it.copy(showDeleteConfirm = false, deleteItemId = null, deleteIsMock = false) }
    }

    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null, error = null) } }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CRUD — Save
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun saveExam(id: String?, name: String, slug: String, icon: String?, description: String?, isActive: Boolean, displayOrder: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val data = buildMap<String, JsonElement> {
                if (id != null) put("id", JsonPrimitive(id))
                put("name", JsonPrimitive(name))
                put("slug", JsonPrimitive(slug))
                if (!icon.isNullOrBlank()) put("icon", JsonPrimitive(icon))
                if (!description.isNullOrBlank()) put("description", JsonPrimitive(description))
                put("is_active", JsonPrimitive(isActive))
                put("display_order", JsonPrimitive(displayOrder))
            }
            adminRepo.upsertExam(data).onSuccess {
                _state.update { it.copy(snackbarMessage = if (id != null) "Exam updated" else "Exam created") }
                dismissDialog(); loadExams()
            }.onFailure { _state.update { it.copy(error = it.error ?: "Failed") } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun saveSubject(id: String?, name: String, slug: String?, icon: String?, subExam: String?, subExamHindi: String?, isActive: Boolean, isFree: Boolean, displayOrder: Int, totalQuestions: Int, totalMarks: Double, durationMinutes: Int, negativeMarking: Double) {
        val examId = _state.value.selectedExam?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val data = buildMap<String, JsonElement> {
                if (id != null) put("id", JsonPrimitive(id))
                put("exam_id", JsonPrimitive(examId))
                put("name", JsonPrimitive(name))
                if (!slug.isNullOrBlank()) put("slug", JsonPrimitive(slug))
                if (!icon.isNullOrBlank()) put("icon", JsonPrimitive(icon))
                if (!subExam.isNullOrBlank()) put("sub_exam", JsonPrimitive(subExam))
                if (!subExamHindi.isNullOrBlank()) put("sub_exam_hindi", JsonPrimitive(subExamHindi))
                put("is_active", JsonPrimitive(isActive))
                put("is_free", JsonPrimitive(isFree))
                put("display_order", JsonPrimitive(displayOrder))
                put("total_questions", JsonPrimitive(totalQuestions))
                put("total_marks", JsonPrimitive(totalMarks))
                put("duration_minutes", JsonPrimitive(durationMinutes))
                put("negative_marking", JsonPrimitive(negativeMarking))
            }
            adminRepo.upsertSubject(data).onSuccess {
                _state.update { it.copy(snackbarMessage = if (id != null) "Subject updated" else "Subject created") }
                dismissDialog(); refreshCurrentLevel()
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun saveTestSet(id: String?, title: String, testType: String, totalQuestions: Int, totalMarks: Double, durationMinutes: Int, negativeMarking: Double, isActive: Boolean, isFree: Boolean, difficulty: String?, year: Int?, examDate: String?, shift: Int?) {
        // For new test sets, need selectedSubject. For edits, get subjectId from the editing item.
        val subjectId = _state.value.selectedSubject?.id
            ?: (_state.value.editingItem as? TestSet)?.subjectId
            ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val data = buildMap<String, JsonElement> {
                if (id != null) put("id", JsonPrimitive(id))
                put("subject_id", JsonPrimitive(subjectId))
                put("title", JsonPrimitive(title))
                put("test_type", JsonPrimitive(testType))
                put("total_questions", JsonPrimitive(totalQuestions))
                put("total_marks", JsonPrimitive(totalMarks))
                put("duration_minutes", JsonPrimitive(durationMinutes))
                put("negative_marking", JsonPrimitive(negativeMarking))
                put("is_active", JsonPrimitive(isActive))
                put("is_free", JsonPrimitive(isFree))
                if (!difficulty.isNullOrBlank()) put("difficulty", JsonPrimitive(difficulty))
                if (year != null) put("year", JsonPrimitive(year))
                if (!examDate.isNullOrBlank()) put("exam_date", JsonPrimitive(examDate))
                if (shift != null) put("shift", JsonPrimitive(shift))
            }
            adminRepo.upsertTestSet(data).onSuccess {
                _state.update { it.copy(snackbarMessage = if (id != null) "Test updated" else "Test created") }
                dismissDialog()
                // Refresh: if we're at TEST_SETS level, reload that subject's tests
                // If at SUBJECTS level (editing a full mock), reload full mocks
                if (_state.value.selectedSubject != null) {
                    loadTestSets(_state.value.selectedSubject!!.id)
                } else {
                    loadFullMockTests(_state.value.subjects.map { it.id })
                }
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun saveQuestion(id: String?, questionNumber: Int, questionText: String, questionTextHindi: String?, options: List<QuestionOption>, correctOptionId: String, explanation: String?, explanationHindi: String?, marks: Double, negativeMarks: Double, topic: String?, difficulty: String?, questionImageUrl: String?) {
        val testSetId = _state.value.selectedTestSet?.id
            ?: _state.value.pyqSelectedTestSet?.id
            ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val optionsJson = JsonArray(options.map { opt ->
                buildJsonObject {
                    put("id", opt.id); put("text", opt.text)
                    if (opt.textHindi != null) put("textHindi", opt.textHindi)
                    if (opt.label != null) put("label", opt.label)
                    if (!opt.imageUrl.isNullOrBlank()) put("image_url", opt.imageUrl)
                }
            })
            val data = buildMap<String, JsonElement> {
                if (id != null) put("id", JsonPrimitive(id))
                put("test_set_id", JsonPrimitive(testSetId))
                put("question_number", JsonPrimitive(questionNumber))
                put("question_text", JsonPrimitive(questionText))
                if (!questionTextHindi.isNullOrBlank()) put("question_text_hindi", JsonPrimitive(questionTextHindi))
                if (!questionImageUrl.isNullOrBlank()) put("question_image_url", JsonPrimitive(questionImageUrl))
                put("options", optionsJson)
                put("correct_option_id", JsonPrimitive(correctOptionId))
                if (!explanation.isNullOrBlank()) put("explanation", JsonPrimitive(explanation))
                if (!explanationHindi.isNullOrBlank()) put("explanation_hindi", JsonPrimitive(explanationHindi))
                put("marks", JsonPrimitive(marks)); put("negative_marks", JsonPrimitive(negativeMarks))
                if (!topic.isNullOrBlank()) put("topic", JsonPrimitive(topic))
                if (!difficulty.isNullOrBlank()) put("difficulty", JsonPrimitive(difficulty))
            }
            adminRepo.upsertQuestion(data).onSuccess {
                _state.update { it.copy(snackbarMessage = if (id != null) "Question updated" else "Question added") }
                dismissDialog(); loadQuestions(testSetId)
                val questions = _state.value.questions
                adminRepo.updateTestSetQuestionCount(testSetId, questions.size, questions.sumOf { it.marks })
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Delete
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun confirmDelete() {
        val id = _state.value.deleteItemId ?: return
        val isMock = _state.value.deleteIsMock
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = if (isMock) {
                adminRepo.deleteTestSet(id)
            } else {
                when (_state.value.level) {
                    AdminLevel.EXAMS -> adminRepo.deleteExam(id)
                    AdminLevel.SUB_EXAMS -> adminRepo.deleteSubject(id)
                    AdminLevel.SUBJECTS -> adminRepo.deleteSubject(id)
                    AdminLevel.TEST_SETS -> adminRepo.deleteTestSet(id)
                    AdminLevel.QUESTIONS -> adminRepo.deleteQuestion(id)
                }
            }
            result.onSuccess {
                _state.update { it.copy(snackbarMessage = "Deleted") }
                dismissDeleteConfirm()
                if (isMock) loadFullMockTests(_state.value.subjects.map { it.id })
                else refreshCurrentLevel()
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun toggleActive(id: String, currentActive: Boolean) {
        viewModelScope.launch {
            val data = mapOf<String, JsonElement>("id" to JsonPrimitive(id), "is_active" to JsonPrimitive(!currentActive))
            val result = when (_state.value.level) {
                AdminLevel.EXAMS -> adminRepo.upsertExam(data)
                AdminLevel.SUBJECTS, AdminLevel.SUB_EXAMS -> adminRepo.upsertSubject(data)
                AdminLevel.TEST_SETS -> adminRepo.upsertTestSet(data)
                else -> return@launch
            }
            result.onSuccess {
                _state.update { it.copy(snackbarMessage = if (!currentActive) "Activated" else "Deactivated") }
                refreshCurrentLevel()
            }
        }
    }

    // Toggle for full mock tests shown at subjects level
    fun toggleMockActive(id: String, currentActive: Boolean) {
        viewModelScope.launch {
            val data = mapOf<String, JsonElement>("id" to JsonPrimitive(id), "is_active" to JsonPrimitive(!currentActive))
            adminRepo.upsertTestSet(data).onSuccess {
                _state.update { it.copy(snackbarMessage = if (!currentActive) "Activated" else "Deactivated") }
                // Reload mocks
                val subjects = _state.value.subjects
                loadFullMockTests(subjects.map { it.id })
            }
        }
    }

    private fun refreshCurrentLevel() {
        when (_state.value.level) {
            AdminLevel.EXAMS -> loadExams()
            AdminLevel.SUB_EXAMS -> _state.value.selectedExam?.id?.let { loadSubExams(it) }
            AdminLevel.SUBJECTS -> {
                // Reload all subjects, re-filter
                _state.value.selectedExam?.id?.let { examId ->
                    viewModelScope.launch {
                        adminRepo.getSubjectsForExam(examId).onSuccess { allSubs ->
                            _state.update { it.copy(allSubjectsForExam = allSubs) }
                            val subExam = _state.value.selectedSubExam
                            if (subExam != null) {
                                _state.update { it.copy(subjects = allSubs.filter { s -> s.subExam == subExam }) }
                            }
                        }
                    }
                }
            }
            AdminLevel.TEST_SETS -> _state.value.selectedSubject?.id?.let { loadTestSets(it) }
            AdminLevel.QUESTIONS -> _state.value.selectedTestSet?.id?.let { loadQuestions(it) }
        }
    }

    fun generateOptionId(): String = UUID.randomUUID().toString().take(8)

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FEEDBACK
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun toggleFeedbackView() {
        val showing = _state.value.showFeedback
        if (!showing) loadFeedback()
        _state.update { it.copy(showFeedback = !showing) }
    }

    private fun loadFeedback() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getAllFeedback().onSuccess { list ->
                _state.update { it.copy(feedbackList = list) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun markFeedback(id: String, status: String) {
        viewModelScope.launch {
            adminRepo.updateFeedbackStatus(id, status).onSuccess {
                _state.update { it.copy(snackbarMessage = "Marked as $status") }
                loadFeedback()
            }
        }
    }

    fun deleteFeedbackItem(id: String) {
        viewModelScope.launch {
            adminRepo.deleteFeedback(id).onSuccess {
                _state.update { it.copy(snackbarMessage = "Feedback deleted") }
                loadFeedback()
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PYQ Management
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun togglePYQView() {
        val showing = _state.value.showPYQ
        if (!showing) loadPYQTestSets()
        _state.update { it.copy(showPYQ = !showing, pyqSelectedTestSet = null, pyqQuestions = emptyList()) }
    }

    fun loadPYQTestSets() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getAllPYQTestSets().onSuccess { tests ->
                _state.update { it.copy(pyqTestSets = tests) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun selectPYQTestSet(testSet: TestSet) {
        _state.update { it.copy(pyqSelectedTestSet = testSet) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            adminRepo.getQuestionsForTestSet(testSet.id).onSuccess { qs ->
                _state.update { it.copy(pyqQuestions = qs) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun pyqGoBack() {
        if (_state.value.pyqSelectedTestSet != null) {
            _state.update { it.copy(pyqSelectedTestSet = null, pyqQuestions = emptyList()) }
        } else {
            _state.update { it.copy(showPYQ = false) }
        }
    }

    fun editPYQTestSet(testSet: TestSet) {
        _state.update { it.copy(showDialog = true, dialogType = DialogType.TEST_SET, editingItem = testSet) }
    }

    fun deletePYQTestSet(id: String, title: String) {
        showDeleteConfirmation(id, title)
    }
}
