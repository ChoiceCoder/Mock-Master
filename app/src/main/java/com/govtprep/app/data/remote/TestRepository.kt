package com.govtprep.app.data.remote

import android.util.Log
import com.govtprep.app.data.model.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRepository @Inject constructor() {
    private val client = SupabaseModule.client
    private val TAG = "TestRepository"

    suspend fun getTestSet(testSetId: String): Result<TestSet> {
        return try {
            val test = client.postgrest.from("test_sets")
                .select { filter { eq("id", testSetId) } }
                .decodeSingle<TestSet>()
            Result.success(test)
        } catch (e: Exception) {
            Log.e(TAG, "getTestSet failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fetch questions WITHOUT correct answers (anti-cheat)
    suspend fun getTestQuestions(testSetId: String): Result<List<Question>> {
        return try {
            Log.d(TAG, "Fetching questions for test: $testSetId")
            val result = client.postgrest.rpc(
                "get_test_questions",
                buildJsonObject { put("p_test_set_id", testSetId) }
            )
            val questions = result.decodeAs<List<Question>>()
            Log.d(TAG, "Loaded ${questions.size} questions")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "getTestQuestions failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Submit answers — server scores and returns results WITH correct answers
    suspend fun submitAnswers(
        testSetId: String,
        answers: Map<String, AnswerEntry>,
        timeSpentSeconds: Int
    ): Result<SubmitResult> {
        return try {
            Log.d(TAG, "Submitting answers for test: $testSetId")
            val answersJson = buildJsonObject {
                answers.forEach { (qId, entry) ->
                    put(qId, buildJsonObject {
                        put("selectedOptionId", entry.selectedOptionId)
                    })
                }
            }

            val result = client.postgrest.rpc(
                "submit_test_answers",
                buildJsonObject {
                    put("p_test_set_id", testSetId)
                    put("p_answers", answersJson)
                    put("p_time_spent_seconds", timeSpentSeconds)
                }
            )
            // RPC returning JSONB comes as a single-element array
            val list = result.decodeAs<List<SubmitResult>>()
            val submitResult = list.firstOrNull() ?: result.decodeAs<SubmitResult>()
            Log.d(TAG, "Submit successful: score=${submitResult.score}")
            Result.success(submitResult)
        } catch (e: Exception) {
            Log.e(TAG, "submitAnswers failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Fetch questions WITH correct answers (for local scoring after submit fails)
    suspend fun getQuestionsWithAnswers(testSetId: String): Result<List<ReviewQuestion>> {
        return try {
            Log.d(TAG, "Fetching questions with answers for: $testSetId")
            val questions = client.postgrest.from("questions")
                .select {
                    filter { eq("test_set_id", testSetId) }
                    order("question_number", Order.ASCENDING)
                }
                .decodeList<ReviewQuestion>()
            Log.d(TAG, "Loaded ${questions.size} questions with answers")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "getQuestionsWithAnswers failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get user's recent attempts
    suspend fun getRecentAttempts(limit: Int = 20): Result<List<Attempt>> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            Log.d(TAG, "Fetching recent attempts for: $userId")
            val attempts = client.postgrest.from("attempts")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("*, test_sets(title, title_hindi, total_marks)")) {
                    filter {
                        eq("user_id", userId)
                        eq("status", "completed")
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<Attempt>()
            Log.d(TAG, "Loaded ${attempts.size} attempts")
            Result.success(attempts)
        } catch (e: Exception) {
            Log.e(TAG, "getRecentAttempts failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get user stats via RPC
    suspend fun getUserStats(): Result<UserStats> {
        return try {
            val userId = client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not logged in"))

            Log.d(TAG, "Fetching user stats for: $userId")
            val result = client.postgrest.rpc(
                "get_user_stats",
                buildJsonObject { put("p_user_id", userId) }
            )
            val stats = result.decodeAs<List<UserStats>>()
            Log.d(TAG, "Stats loaded: ${stats.firstOrNull()}")
            Result.success(stats.firstOrNull() ?: UserStats())
        } catch (e: Exception) {
            Log.e(TAG, "getUserStats failed: ${e.message}", e)
            // Return empty stats instead of failing — stats RPC might not exist yet
            Result.success(UserStats())
        }
    }
}
