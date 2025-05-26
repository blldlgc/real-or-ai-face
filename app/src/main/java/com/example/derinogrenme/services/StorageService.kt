package com.example.derinogrenme.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max
import kotlin.math.min

class StorageService(private val context: Context) {
    private val imagesDir: File
        get() = File(context.filesDir, "prediction_images").apply { mkdirs() }

    companion object {
        private const val MAX_IMAGE_DIMENSION = 1200 // Maksimum boyut
        private const val MIN_IMAGE_DIMENSION = 800  // Minimum boyut
        private const val QUALITY = 85 // JPEG kalitesi (0-100)
    }

    suspend fun saveImage(imageUri: Uri, userId: String): String? {
        return try {
            // Benzersiz bir dosya adı oluştur
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"
            val imageFile = File(imagesDir, fileName)

            // Resmi oku ve EXIF oryantasyonunu düzelt
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            // EXIF oryantasyonunu al ve düzelt
            val exifOrientation = getExifOrientation(imageUri)
            val rotatedBitmap = rotateBitmap(originalBitmap, exifOrientation)
            
            // Resmi akıllı bir şekilde boyutlandır
            val resizedBitmap = resizeBitmap(rotatedBitmap)

            // Resmi kaydet
            FileOutputStream(imageFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }

            // Orijinal bitmap'leri temizle
            originalBitmap.recycle()
            rotatedBitmap.recycle()
            resizedBitmap.recycle()

            // Dosya yolunu döndür
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e("StorageService", "Resim kaydedilirken hata oluştu", e)
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Eğer resim zaten yeterince küçükse, boyutlandırma yapma
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }

        // En-boy oranını koru
        val ratio = width.toFloat() / height.toFloat()
        var newWidth = width
        var newHeight = height

        if (width > height) {
            newWidth = MAX_IMAGE_DIMENSION
            newHeight = (MAX_IMAGE_DIMENSION / ratio).toInt()
        } else {
            newHeight = MAX_IMAGE_DIMENSION
            newWidth = (MAX_IMAGE_DIMENSION * ratio).toInt()
        }

        // Minimum boyuttan küçük olmamasını sağla
        newWidth = max(newWidth, MIN_IMAGE_DIMENSION)
        newHeight = max(newHeight, MIN_IMAGE_DIMENSION)

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            Log.e("StorageService", "EXIF oryantasyonu alınırken hata oluştu", e)
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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