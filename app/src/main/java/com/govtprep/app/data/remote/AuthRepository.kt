package com.govtprep.app.data.remote

import android.util.Log
import com.govtprep.app.data.model.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val client = SupabaseModule.client
    private val TAG = "AuthRepository"

    val isLoggedIn: Boolean
        get() = client.auth.currentSessionOrNull() != null

    val currentUserId: String?
        get() = client.auth.currentUserOrNull()?.id

    fun sessionFlow(): Flow<Boolean> =
        client.auth.sessionStatus.map { it.toString().contains("Authenticated") }

    suspend fun signUp(email: String, password: String, fullName: String, referralCode: String = ""): Result<Unit> {
        return try {
            Log.d(TAG, "Signing up: $email")
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject { put("full_name", fullName) }
            }
            Log.d(TAG, "Signup successful")
            // Apply referral non-critically — failure must never block signup
            if (referralCode.isNotBlank()) {
                try {
                    client.postgrest.rpc(
                        "apply_referral",
                        buildJsonObject { put("p_referral_code", referralCode.trim().uppercase()) }
                    )
                    Log.d(TAG, "Referral applied: $referralCode")
                } catch (e: Exception) {
                    Log.w(TAG, "Referral apply failed (non-critical): ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Signup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "Signing in: $email")
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "Sign in successful, userId: ${currentUserId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<Profile> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
            Log.d(TAG, "Fetching profile for: $userId")
            val profile = client.postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<Profile>()
            Log.d(TAG, "Profile loaded: ${profile.fullName}")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "getProfile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(updates: Map<String, String>): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
            client.postgrest.from("profiles")
                .update(updates) { filter { eq("id", userId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getCurrentUser(): UserInfo? = client.auth.currentUserOrNull()

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            Log.d(TAG, "Changing password...")
            client.auth.updateUser { password = newPassword }
            Log.d(TAG, "Password changed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "changePassword failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
