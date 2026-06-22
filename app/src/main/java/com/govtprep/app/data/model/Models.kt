package com.govtprep.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("target_exam") val targetExam: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("referral_code") val referralCode: String? = null,
    @SerialName("referral_count") val referralCount: Int = 0,
    val coins: Int = 0,
    // Subscription fields
    @SerialName("subscribed_until") val subscribedUntil: String? = null,  // ISO date e.g. "2025-06-14"
    @SerialName("plan_name") val planName: String? = null,                 // e.g. "3 Months"
)

@Serializable
data class Exam(
    val id: String,
    val name: String,
    @SerialName("name_hindi") val nameHindi: String? = null,
    val slug: String,
    val icon: String? = null,
    val description: String? = null,
    @SerialName("description_hindi") val descriptionHindi: String? = null,
    val color: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
)

@Serializable
data class Subject(
    val id: String,
    @SerialName("exam_id") val examId: String,
    val name: String,
    @SerialName("name_hindi") val nameHindi: String? = null,
    val slug: String? = null,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_free") val isFree: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("sub_exam") val subExam: String? = null,
    @SerialName("sub_exam_hindi") val subExamHindi: String? = null,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("negative_marking") val negativeMarking: Double = 0.0,
)

@Serializable
data class TestSet(
    val id: String,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("exam_id") val examId: String? = null,
    @SerialName("sub_exam_name") val subExamName: String? = null,
    val title: String,
    @SerialName("title_hindi") val titleHindi: String? = null,
    @SerialName("test_type") val testType: String = "subject",
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    @SerialName("negative_marking") val negativeMarking: Double = 0.5,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_free") val isFree: Boolean = true,
    @SerialName("is_premium") val isPremium: Boolean = false,
    val difficulty: String? = null,
    val year: Int? = null,
    @SerialName("exam_date") val examDate: String? = null,
    val shift: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Question(
    val id: String,
    @SerialName("test_set_id") val testSetId: String? = null,
    @SerialName("question_number") val questionNumber: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_text_hindi") val questionTextHindi: String? = null,
    @SerialName("question_image_url") val questionImageUrl: String? = null,
    val options: List<QuestionOption> = emptyList(),
    val marks: Double = 2.0,
    @SerialName("negative_marks") val negativeMarks: Double = 0.5,
    val topic: String? = null,
    val difficulty: String? = null,
)

@Serializable
data class QuestionOption(
    val id: String,
    val text: String,
    @SerialName("textHindi") val textHindi: String? = null,
    @SerialName("text_hindi") val textHindiAlt: String? = null,
    val label: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class ReviewQuestion(
    val id: String,
    @SerialName("question_number") val questionNumber: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_text_hindi") val questionTextHindi: String? = null,
    @SerialName("question_image_url") val questionImageUrl: String? = null,
    val options: List<QuestionOption> = emptyList(),
    @SerialName("correct_option_id") val correctOptionId: String,
    val explanation: String? = null,
    @SerialName("explanation_hindi") val explanationHindi: String? = null,
    val marks: Double = 2.0,
    @SerialName("negative_marks") val negativeMarks: Double = 0.5,
)

@Serializable
data class Attempt(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("test_set_id") val testSetId: String,
    val score: Double? = null,
    @SerialName("total_marks") val totalMarks: Double? = null,
    @SerialName("correct_count") val correctCount: Int? = null,
    @SerialName("incorrect_count") val incorrectCount: Int? = null,
    @SerialName("unattempted_count") val unattemptedCount: Int? = null,
    @SerialName("time_spent_seconds") val timeSpentSeconds: Int? = null,
    val status: String = "completed",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("test_sets") val testSet: TestSetBrief? = null,
)

@Serializable
data class TestSetBrief(
    val title: String,
    @SerialName("title_hindi") val titleHindi: String? = null,
    @SerialName("total_marks") val totalMarks: Double? = null,
)

@Serializable
data class SubmitResult(
    @SerialName("attempt_id") val attemptId: String,
    val correct: Int,
    val incorrect: Int,
    val unattempted: Int,
    val score: Double,
    @SerialName("total_marks") val totalMarks: Double,
    val percentage: Double,
    @SerialName("time_taken") val timeTaken: Int,
    val questions: List<ReviewQuestion> = emptyList(),
)

@Serializable
data class UserStats(
    @SerialName("total_attempts") val totalAttempts: Int = 0,
    @SerialName("average_score") val averageScore: Double = 0.0,
    @SerialName("total_time_minutes") val totalTimeMinutes: Int = 0,
    @SerialName("tests_this_week") val testsThisWeek: Int = 0,
)

@Serializable
data class AnswerEntry(
    val selectedOptionId: String?,
    val isMarkedForReview: Boolean = false,
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Admin models
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Serializable
data class AdminQuestion(
    val id: String,
    @SerialName("test_set_id") val testSetId: String,
    @SerialName("question_number") val questionNumber: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_text_hindi") val questionTextHindi: String? = null,
    @SerialName("question_image_url") val questionImageUrl: String? = null,
    val options: List<QuestionOption> = emptyList(),
    @SerialName("correct_option_id") val correctOptionId: String,
    val explanation: String? = null,
    @SerialName("explanation_hindi") val explanationHindi: String? = null,
    val marks: Double = 2.0,
    @SerialName("negative_marks") val negativeMarks: Double = 0.5,
    val topic: String? = null,
    val difficulty: String? = null,
)

@Serializable
data class Feedback(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    val message: String = "",
    val status: String = "new",
    @SerialName("created_at") val createdAt: String? = null,
)
