package com.govtprep.app.ui.screens.test

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.govtprep.app.data.model.*
import com.govtprep.app.data.remote.TestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TestPhase { LOADING, INSTRUCTIONS, ACTIVE, SUBMITTING, RESULTS }
enum class QuestionStatus { UNANSWERED, ANSWERED, MARKED, ANSWERED_MARKED }

data class TestState(
    val phase: TestPhase = TestPhase.LOADING, val testSet: TestSet? = null,
    val questions: List<Question> = emptyList(), val currentIndex: Int = 0,
    val answers: Map<String, String?> = emptyMap(), val markedForReview: Set<String> = emptySet(),
    val timeRemainingSeconds: Int = 0, val totalTimeSeconds: Int = 0,
    val showPalette: Boolean = false, val showSubmitDialog: Boolean = false,
    val submitResult: SubmitResult? = null, val reviewQuestions: List<ReviewQuestion> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TestViewModel @Inject constructor(private val testRepo: TestRepository) : ViewModel() {
    private val TAG = "TestVM"
    private val _state = MutableStateFlow(TestState())
    val state = _state.asStateFlow()
    private var timer: CountDownTimer? = null

    fun loadTest(testSetId: String) {
        viewModelScope.launch {
            _state.update { it.copy(phase = TestPhase.LOADING) }
            testRepo.getTestSet(testSetId).onSuccess { ts -> _state.update { it.copy(testSet = ts) } }
            testRepo.getTestQuestions(testSetId).onSuccess { qs ->
                val total = (_state.value.testSet?.durationMinutes ?: 30) * 60
                _state.update { it.copy(questions = qs, timeRemainingSeconds = total, totalTimeSeconds = total, phase = TestPhase.INSTRUCTIONS) }
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun startTest() { _state.update { it.copy(phase = TestPhase.ACTIVE) }; startTimer() }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(_state.value.timeRemainingSeconds * 1000L, 1000L) {
            override fun onTick(millis: Long) { _state.update { it.copy(timeRemainingSeconds = (millis / 1000).toInt()) } }
            override fun onFinish() { _state.update { it.copy(timeRemainingSeconds = 0) }; submitTest() }
        }.start()
    }

    fun selectOption(qId: String, optId: String) {
        val cur = _state.value.answers[qId]
        val m = _state.value.answers.toMutableMap()
        m[qId] = if (cur == optId) null else optId
        _state.update { it.copy(answers = m) }
    }

    fun goToQuestion(i: Int) { _state.update { it.copy(currentIndex = i.coerceIn(0, it.questions.size - 1)) } }
    fun nextQuestion() = goToQuestion(_state.value.currentIndex + 1)
    fun prevQuestion() = goToQuestion(_state.value.currentIndex - 1)

    fun toggleMarkForReview(qId: String) {
        val m = _state.value.markedForReview.toMutableSet()
        if (m.contains(qId)) m.remove(qId) else m.add(qId)
        _state.update { it.copy(markedForReview = m) }
    }

    fun togglePalette() { _state.update { it.copy(showPalette = !it.showPalette) } }
    fun showSubmitDialog() { _state.update { it.copy(showSubmitDialog = true) } }
    fun hideSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }

    fun submitTest() {
        timer?.cancel()
        val s = _state.value
        _state.update { it.copy(phase = TestPhase.SUBMITTING, showSubmitDialog = false) }
        val timeSpent = s.totalTimeSeconds - s.timeRemainingSeconds
        val answerMap = mutableMapOf<String, AnswerEntry>()
        s.questions.forEach { q -> answerMap[q.id] = AnswerEntry(s.answers[q.id], s.markedForReview.contains(q.id)) }

        viewModelScope.launch {
            val testSetId = s.testSet?.id ?: return@launch

            // Try server-side submit first
            testRepo.submitAnswers(testSetId, answerMap, timeSpent)
                .onSuccess { r ->
                    Log.d(TAG, "Server submit OK: score=${r.score}")
                    _state.update { it.copy(phase = TestPhase.RESULTS, submitResult = r, reviewQuestions = r.questions) }
                }
                .onFailure { e ->
                    Log.w(TAG, "Server submit failed: ${e.message}, trying local scoring...")
                    // Fallback: fetch questions with correct answers and score locally
                    scoreLocally(testSetId, s, timeSpent)
                }
        }
    }

    private suspend fun scoreLocally(testSetId: String, s: TestState, timeSpent: Int) {
        testRepo.getQuestionsWithAnswers(testSetId)
            .onSuccess { reviewQs ->
                Log.d(TAG, "Local scoring with ${reviewQs.size} questions")
                var correct = 0
                var incorrect = 0
                var unattempted = 0
                var score = 0.0
                val totalMarks = s.testSet?.totalMarks ?: (reviewQs.size * 2.0)

                reviewQs.forEach { q ->
                    val userAns = s.answers[q.id]
                    when {
                        userAns == null -> unattempted++
                        userAns == q.correctOptionId -> { correct++; score += q.marks }
                        else -> { incorrect++; score -= q.negativeMarks }
                    }
                }
                if (score < 0) score = 0.0
                val pct = if (totalMarks > 0) (score / totalMarks * 100) else 0.0

                val result = SubmitResult(
                    attemptId = "local",
                    correct = correct,
                    incorrect = incorrect,
                    unattempted = unattempted,
                    score = score,
                    totalMarks = totalMarks,
                    percentage = pct,
                    timeTaken = timeSpent,
                    questions = reviewQs
                )
                _state.update { it.copy(phase = TestPhase.RESULTS, submitResult = result, reviewQuestions = reviewQs) }
            }
            .onFailure { e2 ->
                Log.e(TAG, "Local scoring also failed: ${e2.message}")
                // Last resort: show results with what we know (no review questions)
                val answered = s.answers.count { it.value != null }
                val result = SubmitResult(
                    attemptId = "local",
                    correct = 0, incorrect = 0, unattempted = s.questions.size,
                    score = 0.0, totalMarks = s.testSet?.totalMarks ?: 0.0,
                    percentage = 0.0, timeTaken = timeSpent,
                    questions = emptyList()
                )
                _state.update { it.copy(
                    phase = TestPhase.RESULTS, submitResult = result,
                    error = "Could not score your test. Please check your internet connection."
                )}
            }
    }

    val answeredCount: Int get() = _state.value.answers.count { it.value != null }
    val unansweredCount: Int get() = _state.value.questions.size - answeredCount
    val markedCount: Int get() = _state.value.markedForReview.size

    fun getQuestionStatus(qId: String): QuestionStatus {
        val a = _state.value.answers[qId] != null; val m = _state.value.markedForReview.contains(qId)
        return when { a && m -> QuestionStatus.ANSWERED_MARKED; a -> QuestionStatus.ANSWERED; m -> QuestionStatus.MARKED; else -> QuestionStatus.UNANSWERED }
    }

    override fun onCleared() { timer?.cancel(); super.onCleared() }
}
