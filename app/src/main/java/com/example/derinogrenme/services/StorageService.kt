package com.example.derinogrenme.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class StorageService(private val context: Context) {
    private val imagesDir: File
        get() = File(context.filesDir, "prediction_images").apply { mkdirs() }

    suspend fun saveImage(imageUri: Uri, userId: String): String? {
        return try {
            // Benzersiz bir dosya adı oluştur
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"
            val imageFile = File(imagesDir, fileName)

            // Resmi oku ve boyutlandır
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 800, 800, true)

            // Resmi kaydet
            FileOutputStream(imageFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            // Orijinal bitmap'leri temizle
            originalBitmap.recycle()
            resizedBitmap.recycle()

            // Dosya yolunu döndür
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e("StorageService", "Resim kaydedilirken hata oluştu", e)
            null
        }
    }

    fun loadImage(imagePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(imagePath)
        } catch (e: Exception) {
            Log.e("StorageService", "Resim yüklenirken hata oluştu", e)
            null
        }
    }

    fun deleteImage(imagePath: String): Boolean {
        return try {
            File(imagePath).delete()
        } catch (e: Exception) {
            Log.e("StorageService", "Resim silinirken hata oluştu", e)
            false
        }
    }
} 