package com.example.derinogrenme.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.derinogrenme.databinding.FragmentHomeBinding
import com.example.derinogrenme.adapters.PredictionAdapter
import com.example.derinogrenme.services.FirestoreService
import com.example.derinogrenme.services.StorageService
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.graphics.BitmapFactory
import android.content.Intent
import android.widget.Toast
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.DataType
import android.util.Log
import android.graphics.Color
import android.widget.TextView
import com.example.derinogrenme.R
import com.example.derinogrenme.models.Prediction
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.example.derinogrenme.GameModeActivity
import android.graphics.Bitmap

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestoreService: FirestoreService
    private lateinit var storageService: StorageService
    private lateinit var predictionAdapter: PredictionAdapter
    private lateinit var tflite: Interpreter
    private val IMG_SIZE = 224
    private var currentPhotoPath: String? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val imageUri = result.data?.data
                imageUri?.let {
                    processImage(it)
                } ?: run {
                    binding.resultTextView.text = "Resim seçilemedi"
                    Toast.makeText(requireContext(), "Resim seçilemedi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.resultTextView.text = "Hata: ${e.message}"
                Toast.makeText(requireContext(), "Resim işlenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            binding.resultTextView.text = "Resim seçme işlemi iptal edildi"
            Toast.makeText(requireContext(), "Resim seçme işlemi iptal edildi", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                currentPhotoPath?.let { path ->
                    val imageUri = Uri.fromFile(File(path))
                    processImage(imageUri)
                }
            } catch (e: Exception) {
                binding.resultTextView.text = "Hata: ${e.message}"
                Toast.makeText(requireContext(), "Fotoğraf işlenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            binding.resultTextView.text = "Fotoğraf çekme işlemi iptal edildi"
            Toast.makeText(requireContext(), "Fotoğraf çekme işlemi iptal edildi", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Kamera izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        firestoreService = FirestoreService()
        storageService = StorageService(requireContext())
        
        setupRecyclerView()
        updateUserInfo()
        loadRecentPredictions()

        // Modeli yükle
        try {
            tflite = Interpreter(loadModelFile())
            Log.d("Model", "Model başarıyla yüklendi")
        } catch (e: Exception) {
            binding.resultTextView.text = "Model yüklenirken hata oluştu: ${e.message}"
            Toast.makeText(requireContext(), "Model yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

        binding.selectImageButton.setOnClickListener {
            openGallery()
        }

        binding.cameraButton.setOnClickListener {
            checkCameraPermission()
        }

        // Oyun başlatma butonu
        binding.startGameButton.setOnClickListener {
            val intent = Intent(requireContext(), GameModeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        predictionAdapter = PredictionAdapter(emptyList())
        binding.recentPredictionsRecyclerView.apply {
            adapter = predictionAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        }
    }

    private fun loadRecentPredictions() {
        lifecycleScope.launch {
            try {
                FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                    val predictions = firestoreService.getRecentPredictions(userId, 3)
                    predictionAdapter.updatePredictions(predictions)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Tahminler yüklenirken hata oluştu: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val welcomeMessage = view?.findViewById<TextView>(R.id.welcomeMessage)
            welcomeMessage?.text = "Hoş geldiniz, ${currentUser.displayName ?: "Kullanıcı"}!"
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
            binding.resultTextView.text = "Resim seçici açılırken hata oluştu: ${e.message}"
            Toast.makeText(requireContext(), "Resim seçici açılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd("efficientnet_model.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun updateRecentPredictions(predictions: List<Prediction>) {
        // Son 3 tahmini al
        val recentPredictions = predictions.takeLast(3)
        predictionAdapter.updatePredictions(recentPredictions)
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            cameraLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Kamera açılırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun processImage(imageUri: Uri) {
        try {
            // Seçilen resmi ImageView'da göster
            binding.selectedImageView.visibility = View.VISIBLE
            binding.selectedImageView.setImageURI(imageUri)

            // Resmi işle ve tahmin yap
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
            
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(IMG_SIZE, IMG_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resizedBitmap)
            val processedImage = imageProcessor.process(tensorImage)

            val outputBuffer = Array(1) { FloatArray(2) }
            tflite.run(processedImage.buffer, outputBuffer)

            val confidence = outputBuffer[0][0]
            val result = if (confidence > 0.5f) "Gerçek" else "Sahte"
            val confidencePercentage = (confidence * 100).toInt()

            // Sonuçları göster
            binding.resultTextView.text = "Sonuç: $result"
            binding.confidenceTextView.text = "Güven Oranı: %$confidencePercentage"

            // Overlay'i göster
            showPredictionOverlay(imageUri, result, confidencePercentage)

            // Tahmini kaydet
            savePrediction(result, confidencePercentage, imageUri)

        } catch (e: Exception) {
            binding.resultTextView.text = "Hata: ${e.message}"
            Toast.makeText(requireContext(), "Resim işlenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showPredictionOverlay(imageUri: Uri, result: String, confidence: Int) {
        // Overlay'i hazırla
        binding.predictionResultOverlay.apply {
            // Resmi yükle
            binding.resultImageView.setImageURI(imageUri)
            
            // Sonuç metinlerini ayarla
            binding.overlayResultText.text = "Sonuç: $result"
            binding.overlayConfidenceText.text = "Güven Oranı: %$confidence"
            
            // Kapatma butonunu ayarla
            binding.closeOverlayButton.setOnClickListener {
                hidePredictionOverlay()
            }
            
            // Overlay'i göster
            visibility = View.VISIBLE
            alpha = 0f
            translationY = 100f
            
            // Animasyonu başlat
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
        }
    }

    private fun hidePredictionOverlay() {
        binding.predictionResultOverlay.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(300)
            .withEndAction {
                binding.predictionResultOverlay.visibility = View.GONE
            }
            .start()
    }

    private fun savePrediction(result: String, confidence: Int, imageUri: Uri) {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    // Resmi Storage'a yükle
                    val imagePath = storageService.saveImage(imageUri, currentUser.uid)
                    
                    // Tahmini Firestore'a kaydet
                    val prediction = Prediction(
                        result = result,
                        confidence = confidence.toFloat() / 100f, // Int'i Float'a çevir ve 0-1 aralığına normalize et
                        imageUrl = imagePath ?: "",
                        timestamp = Timestamp.now()
                    )
                    
                    firestoreService.savePrediction(currentUser.uid, prediction)
                    
                    // Son tahminleri güncelle
                    loadRecentPredictions()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Tahmin kaydedilirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 