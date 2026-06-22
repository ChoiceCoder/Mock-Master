package com.govtprep.app.data.remote

import android.content.Context
import android.util.Log
import com.govtprep.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseModule {
    private const val TAG = "SupabaseModule"

    @Volatile
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("SupabaseModule not initialized. Call init(context) first.")

    fun init(context: Context) {
        if (_client != null) return

        Log.d(TAG, "Initializing Supabase client...")
        _client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "com.govtprep.app"
                host = "auth-callback"
            }
            install(Postgrest)
            install(Storage)
        }
        Log.d(TAG, "Supabase client initialized with context")
    }
}
