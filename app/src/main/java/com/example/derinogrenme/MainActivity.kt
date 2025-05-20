package com.example.derinogrenme

import org.tensorflow.lite.Interpreter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.nio.MappedByteBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import android.graphics.BitmapFactory
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.DataType


class MainActivity : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private lateinit var resultTextView: TextView
    private lateinit var confidenceTextView: TextView
    private lateinit var selectImageButton: Button
    private lateinit var selectedImageView: ImageView
    private val CAMERA_PERMISSION_CODE = 1001
    private val IMG_SIZE = 224

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val imageUri = result.data?.data
                imageUri?.let {
                    // Seçilen resmi ImageView'da göster
                    selectedImageView.setImageURI(it)
                    
                    // Resmi işle ve tahmin yap
                    val inputStream = contentResolver.openInputStream(it)
                    val imageBitmap = BitmapFactory.decodeStream(inputStream)
                    val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(imageBitmap, IMG_SIZE, IMG_SIZE, true)
                    
                    // ByteBuffer oluştur
                    val byteBuffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3)
                    byteBuffer.order(ByteOrder.nativeOrder())
                    
                    // Piksel değerlerini al ve normalize et
                    val pixels = IntArray(IMG_SIZE * IMG_SIZE)
                    resizedBitmap.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

                    val tensorImage = TensorImage(DataType.FLOAT32)
                    tensorImage.load(resizedBitmap)


                    val imageProcessor = ImageProcessor.Builder()
                        .add(ResizeOp(IMG_SIZE, IMG_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                        .add(NormalizeOp(0f, 1f))  // veya gerekiyorsa NormalizeOp(127.5f, 127.5f)
                        .build()

                    val processedImage = imageProcessor.process(tensorImage)
                    val inputBuffer = processedImage.buffer

                    val output = Array(1) { FloatArray(1) }
                    tflite.run(inputBuffer, output)




                    // Model çalıştır
                   /*byteBuffer.rewind()
                    val output = Array(1) { FloatArray(1) }
                    tflite.run(byteBuffer, output)
                    */

                    val outputShape = tflite.getOutputTensor(0).shape()
                    Log.d("TFLiteDebug", "Model output shape: ${outputShape.contentToString()}")


                    // Tahmin sonucunu göster
                    val prediction = output[0][0]
                    Log.d("Prediction", "Raw prediction value: $prediction")

                    val isReal = prediction > 0.5f
                    val confidence = if (isReal) prediction else 1 - prediction

                    val resultText = if (isReal) "REAL" else "FAKE"
                    val resultColor = if (isReal) "#4CAF50" else "#F44336"

                    resultTextView.apply {
                        text = resultText
                        setTextColor(android.graphics.Color.parseColor(resultColor))
                    }

                    confidenceTextView.text = "Güven Oranı: ${String.format("%.1f", confidence * 100)}%"


                } ?: run {
                    resultTextView.text = "Resim seçilemedi"
                    Toast.makeText(this, "Resim seçilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                resultTextView.text = "Hata: ${e.message}"
                Toast.makeText(this, "Resim işlenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            resultTextView.text = "Resim seçme işlemi iptal edildi"
            Toast.makeText(this, "Resim seçme işlemi iptal edildi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI elemanlarını başlat
        resultTextView = findViewById(R.id.resultTextView)
        confidenceTextView = findViewById(R.id.confidenceTextView)
        selectImageButton = findViewById(R.id.selectImageButton)
        selectedImageView = findViewById(R.id.selectedImageView)

        // Buton tıklama olayını ayarla
        selectImageButton.setOnClickListener {
            openGallery()
        }

        try {
            // Modeli yükle
            tflite = Interpreter(loadModelFile())
            Log.d("Model", "Model başarıyla yüklendi")
        } catch (e: Exception) {
            resultTextView.text = "Model yüklenirken hata oluştu: ${e.message}"
            Toast.makeText(this, "Model yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
            resultTextView.text = "Resim seçici açılırken hata oluştu: ${e.message}"
            Toast.makeText(this, "Resim seçici açılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("efficientnet_model.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    override fun onDestroy() {
        super.onDestroy()
        tflite.close()
    }
}
