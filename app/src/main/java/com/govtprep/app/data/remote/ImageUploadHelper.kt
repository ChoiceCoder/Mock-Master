package com.govtprep.app.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.storage.storage
import java.util.UUID

object ImageUploadHelper {
    private const val TAG = "ImageUpload"
    private const val BUCKET = "question-images"

    /**
     * Upload image from URI to Supabase Storage.
     * Returns the public URL on success, null on failure.
     */
    suspend fun uploadImage(context: Context, uri: Uri, folder: String = "questions"): String? {
        return try {
            val client = SupabaseModule.client
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: return null
            val ext = getExtension(context, uri)
            val fileName = "$folder/${UUID.randomUUID()}.$ext"

            Log.d(TAG, "Uploading $fileName (${bytes.size} bytes)")
            client.storage.from(BUCKET).upload(fileName, bytes) { upsert = true }

            val publicUrl = client.storage.from(BUCKET).publicUrl(fileName)
            Log.d(TAG, "Upload success: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}", e)
            null
        }
    }

    private fun getExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return "jpg"
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            else -> "jpg"
        }
    }
}
