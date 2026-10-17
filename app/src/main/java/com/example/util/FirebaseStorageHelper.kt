package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Central helper for uploading images to Firebase Storage and retrieving permanent https:// download URLs.
 */
object FirebaseStorageHelper {
    private const val TAG = "FirebaseStorageHelper"

    /**
     * Uploads a receipt image to: receipts/${timestamp}_${userId}.jpg
     * Returns the permanent https:// download URL.
     */
    suspend fun uploadReceiptImage(
        context: Context,
        uri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val validUserId = userId.ifBlank { "user_${System.currentTimeMillis()}" }
            val fileName = "receipts/${System.currentTimeMillis()}_${validUserId}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("تعذر قراءة بيانات الصورة المحددة من الجهاز"))

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Receipt uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload receipt: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "فشل رفع صورة الإشعار للسحابة"))
        }
    }

    /**
     * Uploads an order reference attachment image to: order_attachments/${timestamp}_${userId}.jpg
     * Returns the permanent https:// download URL.
     */
    suspend fun uploadOrderAttachment(
        context: Context,
        uri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val validUserId = userId.ifBlank { "user_${System.currentTimeMillis()}" }
            val fileName = "order_attachments/${System.currentTimeMillis()}_${validUserId}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("تعذر قراءة بيانات الصورة المحددة من الجهاز"))

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Order attachment uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload order attachment: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "فشل رفع الصورة المرفقة للسحابة"))
        }
    }
}
