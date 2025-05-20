package com.example.derinogrenme

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.derinogrenme.adapters.PredictionAdapter
import com.example.derinogrenme.databinding.ActivityHistoryBinding
import com.example.derinogrenme.services.FirestoreService
import com.example.derinogrenme.services.StorageService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreService: FirestoreService
    private lateinit var storageService: StorageService
    private lateinit var predictionAdapter: PredictionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase servislerini başlat
        auth = Firebase.auth
        firestoreService = FirestoreService()
        storageService = StorageService(this)

        // Toolbar ayarları
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Geçmiş Tahminler"

        // RecyclerView ayarları
        setupRecyclerView()

        // Tahminleri yükle
        loadPredictions()
    }

    private fun setupRecyclerView() {
        predictionAdapter = PredictionAdapter(emptyList(), storageService)
        binding.predictionsRecyclerView.apply {
            adapter = predictionAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@HistoryActivity)
        }
    }

    private fun loadPredictions() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Log.e("HistoryActivity", "Kullanıcı oturumu bulunamadı")
                    binding.emptyView.visibility = View.VISIBLE
                    binding.emptyView.text = "Oturum açmanız gerekiyor"
                    return@launch
                }

                Log.d("HistoryActivity", "Tahminler yükleniyor... UserId: ${currentUser.uid}")
                val predictions = firestoreService.getRecentPredictions(currentUser.uid, 10000)
                Log.d("HistoryActivity", "Yüklenen tahmin sayısı: ${predictions.size}")

                predictionAdapter = PredictionAdapter(predictions, storageService)
                binding.predictionsRecyclerView.adapter = predictionAdapter

                // Sonuç yoksa mesaj göster
                binding.emptyView.visibility = if (predictions.isEmpty()) {
                    Log.d("HistoryActivity", "Tahmin bulunamadı")
                    View.VISIBLE
                } else {
                    Log.d("HistoryActivity", "Tahminler başarıyla yüklendi")
                    View.GONE
                }
            } catch (e: Exception) {
                Log.e("HistoryActivity", "Tahminler yüklenirken hata oluştu", e)
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = "Tahminler yüklenirken hata oluştu: ${e.message}"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 