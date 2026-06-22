package com.govtprep.app.data.remote

import android.util.Log
import com.govtprep.app.data.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor() {
    private val client = SupabaseModule.client
    private val TAG = "ExamRepository"

    suspend fun getActiveExams(): Result<List<Exam>> {
        return try {
            Log.d(TAG, "Fetching active exams...")
            val exams = client.postgrest.from("exams")
                .select {
                    filter { eq("is_active", true) }
                    order("display_order", Order.ASCENDING)
                }
                .decodeList<Exam>()
            Log.d(TAG, "Loaded ${exams.size} exams")
            Result.success(exams)
        } catch (e: Exception) {
            Log.e(TAG, "getActiveExams failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getExamBySlug(slug: String): Result<Exam> {
        return try {
            Log.d(TAG, "Fetching exam by slug: $slug")
            val exam = client.postgrest.from("exams")
                .select { filter { eq("slug", slug) } }
                .decodeSingle<Exam>()
            Result.success(exam)
        } catch (e: Exception) {
            Log.e(TAG, "getExamBySlug failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fetch ONLY active subjects (for loading tests)
    suspend fun getSubjects(examId: String): Result<List<Subject>> {
        return try {
            Log.d(TAG, "Fetching active subjects for exam: $examId")
            val subjects = client.postgrest.from("subjects")
                .select {
                    filter {
                        eq("exam_id", examId)
                        eq("is_active", true)
                    }
                    order("display_order", Order.ASCENDING)
                }
                .decodeList<Subject>()
            Log.d(TAG, "Loaded ${subjects.size} active subjects")
            Result.success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "getSubjects failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fetch ALL subjects including inactive (for sub-exam picker with "Coming Soon")
    suspend fun getAllSubjects(examId: String): Result<List<Subject>> {
        return try {
            Log.d(TAG, "Fetching ALL subjects for exam: $examId")
            val subjects = client.postgrest.from("subjects")
                .select {
                    filter { eq("exam_id", examId) }
                    order("display_order", Order.ASCENDING)
                }
                .decodeList<Subject>()
            Log.d(TAG, "Loaded ${subjects.size} total subjects")
            Result.success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "getAllSubjects failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getTestSets(subjectId: String, testType: String? = null): Result<List<TestSet>> {
        return try {
            val tests = client.postgrest.from("test_sets")
                .select {
                    filter {
                        eq("subject_id", subjectId)
                        eq("is_active", true)
                        if (testType != null) eq("test_type", testType)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TestSet>()
            Result.success(tests)
        } catch (e: Exception) {
            Log.e(TAG, "getTestSets failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fetch full mock tests across all active subjects of an exam
    suspend fun getMockTests(examId: String): Result<List<TestSet>> {
        return try {
            val subjects = getSubjects(examId).getOrElse { return Result.failure(it) }
            val subjectIds = subjects.map { it.id }
            if (subjectIds.isEmpty()) return Result.success(emptyList())

            val tests = client.postgrest.from("test_sets")
                .select {
                    filter {
                        isIn("subject_id", subjectIds)
                        eq("is_active", true)
                        eq("test_type", "full_mock")
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TestSet>()
            Result.success(tests)
        } catch (e: Exception) {
            Log.e(TAG, "getMockTests failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PYQ — Previous Year Questions
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    // Fetch all PYQ test sets for a given exam (across all its subjects)
    suspend fun getPYQTestSets(examId: String): Result<List<TestSet>> {
        return try {
            Log.d(TAG, "Fetching PYQ tests for exam: $examId")
            val subjects = getSubjects(examId).getOrElse { return Result.failure(it) }
            val subjectIds = subjects.map { it.id }
            if (subjectIds.isEmpty()) return Result.success(emptyList())

            val tests = client.postgrest.from("test_sets")
                .select {
                    filter {
                        isIn("subject_id", subjectIds)
                        eq("is_active", true)
                        eq("test_type", "previous_year")
                    }
                    order("year", Order.DESCENDING)
                }
                .decodeList<TestSet>()
            Log.d(TAG, "Loaded ${tests.size} PYQ tests")
            Result.success(tests)
        } catch (e: Exception) {
            Log.e(TAG, "getPYQTestSets failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Direct PYQ fetch by exam_id — no subjects involved
    suspend fun getPYQTestSetsByExam(examId: String): Result<List<TestSet>> {
        return try {
            Log.d(TAG, "Fetching PYQ by exam_id: $examId")
            val tests = client.postgrest.from("test_sets")
                .select {
                    filter {
                        eq("exam_id", examId)
                        eq("is_active", true)
                        eq("test_type", "previous_year")
                    }
                    order("year", Order.DESCENDING)
                }
                .decodeList<TestSet>()
            Log.d(TAG, "Loaded ${tests.size} PYQ tests for exam $examId")
            Result.success(tests)
        } catch (e: Exception) {
            Log.e(TAG, "getPYQTestSetsByExam failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
