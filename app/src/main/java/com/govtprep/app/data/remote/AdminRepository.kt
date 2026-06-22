package com.govtprep.app.data.remote

import android.util.Log
import com.govtprep.app.data.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor() {
    private val client = SupabaseModule.client
    private val TAG = "AdminRepo"

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // EXAMS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun getAllExams(): Result<List<Exam>> {
        return try {
            val exams = client.postgrest.from("exams")
                .select { order("display_order", Order.ASCENDING) }
                .decodeList<Exam>()
            Result.success(exams)
        } catch (e: Exception) {
            Log.e(TAG, "getAllExams: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertExam(data: Map<String, JsonElement>): Result<Unit> {
        return try {
            if (data.containsKey("id")) {
                val id = data["id"]?.jsonPrimitive?.content ?: ""
                val updates = data.filterKeys { it != "id" }
                client.postgrest.from("exams").update(updates) { filter { eq("id", id) } }
            } else {
                client.postgrest.from("exams").insert(data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertExam: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExam(id: String): Result<Unit> {
        return try {
            client.postgrest.from("exams").delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteExam: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // SUBJECTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun getSubjectsForExam(examId: String): Result<List<Subject>> {
        return try {
            val subjects = client.postgrest.from("subjects")
                .select {
                    filter { eq("exam_id", examId) }
                    order("display_order", Order.ASCENDING)
                }
                .decodeList<Subject>()
            Result.success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "getSubjects: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertSubject(data: Map<String, JsonElement>): Result<Unit> {
        return try {
            if (data.containsKey("id")) {
                val id = data["id"]?.jsonPrimitive?.content ?: ""
                val updates = data.filterKeys { it != "id" }
                client.postgrest.from("subjects").update(updates) { filter { eq("id", id) } }
            } else {
                client.postgrest.from("subjects").insert(data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertSubject: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(id: String): Result<Unit> {
        return try {
            client.postgrest.from("subjects").delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteSubject: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TEST SETS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun getTestSetsForSubject(subjectId: String): Result<List<TestSet>> {
        return try {
            val tests = client.postgrest.from("test_sets")
                .select {
                    filter { eq("subject_id", subjectId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TestSet>()
            Result.success(tests)
        } catch (e: Exception) {
            Log.e(TAG, "getTestSets: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertTestSet(data: Map<String, JsonElement>): Result<Unit> {
        return try {
            if (data.containsKey("id")) {
                val id = data["id"]?.jsonPrimitive?.content ?: ""
                val updates = data.filterKeys { it != "id" }
                client.postgrest.from("test_sets").update(updates) { filter { eq("id", id) } }
            } else {
                client.postgrest.from("test_sets").insert(data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertTestSet: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTestSet(id: String): Result<Unit> {
        return try {
            client.postgrest.from("test_sets").delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTestSet: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // QUESTIONS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun getQuestionsForTestSet(testSetId: String): Result<List<AdminQuestion>> {
        return try {
            val questions = client.postgrest.from("questions")
                .select {
                    filter { eq("test_set_id", testSetId) }
                    order("question_number", Order.ASCENDING)
                }
                .decodeList<AdminQuestion>()
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "getQuestions: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun upsertQuestion(data: Map<String, JsonElement>): Result<Unit> {
        return try {
            if (data.containsKey("id")) {
                val id = data["id"]?.jsonPrimitive?.content ?: ""
                val updates = data.filterKeys { it != "id" }
                client.postgrest.from("questions").update(updates) { filter { eq("id", id) } }
            } else {
                client.postgrest.from("questions").insert(data)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "upsertQuestion: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(id: String): Result<Unit> {
        return try {
            client.postgrest.from("questions").delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteQuestion: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Update test_set question count after add/delete
    suspend fun updateTestSetQuestionCount(testSetId: String, count: Int, totalMarks: Double): Result<Unit> {
        return try {
            client.postgrest.from("test_sets").update(
                buildMap {
                    put("total_questions", JsonPrimitive(count))
                    put("total_marks", JsonPrimitive(totalMarks))
                }
            ) { filter { eq("id", testSetId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateTestSetCount: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FEEDBACK
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun submitFeedback(data: Map<String, JsonElement>): Result<Unit> {
        return try {
            // Only send simple text fields — no UUID FK that could trigger RLS issues
            val safeData = buildMap<String, JsonElement> {
                data["user_name"]?.let { put("user_name", it) }
                data["user_email"]?.let { put("user_email", it) }
                data["message"]?.let { put("message", it) }
                put("status", JsonPrimitive("new"))
            }
            client.postgrest.from("feedback").insert(safeData)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "submitFeedback: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllFeedback(): Result<List<com.govtprep.app.data.model.Feedback>> {
        return try {
            val feedback = client.postgrest.from("feedback")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<com.govtprep.app.data.model.Feedback>()
            Result.success(feedback)
        } catch (e: Exception) {
            Log.e(TAG, "getAllFeedback: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateFeedbackStatus(id: String, status: String): Result<Unit> {
        return try {
            client.postgrest.from("feedback").update(
                mapOf("status" to JsonPrimitive(status))
            ) { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateFeedbackStatus: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFeedback(id: String): Result<Unit> {
        return try {
            client.postgrest.from("feedback").delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteFeedback: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PYQ — Previous Year Questions
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun getAllPYQTestSets(): Result<List<TestSet>> {
        return try {
            val tests = client.postgrest.from("test_sets")
                .select {
                    filter { eq("test_type", "previous_year") }
                    order("year", Order.DESCENDING)
                }
                .decodeList<TestSet>()
            Log.d(TAG, "Loaded ${tests.size} PYQ test sets")
            Result.success(tests)
        } catch (e: Exception) {
            Log.e(TAG, "getAllPYQTestSets: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSubjectById(subjectId: String): Result<Subject> {
        return try {
            val subject = client.postgrest.from("subjects")
                .select { filter { eq("id", subjectId) } }
                .decodeSingle<Subject>()
            Result.success(subject)
        } catch (e: Exception) {
            Log.e(TAG, "getSubjectById: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getExamById(examId: String): Result<Exam> {
        return try {
            val exam = client.postgrest.from("exams")
                .select { filter { eq("id", examId) } }
                .decodeSingle<Exam>()
            Result.success(exam)
        } catch (e: Exception) {
            Log.e(TAG, "getExamById: ${e.message}", e)
            Result.failure(e)
        }
    }
}
