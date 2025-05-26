package com.example.derinogrenme.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.derinogrenme.databinding.FragmentHistoryBinding
import com.example.derinogrenme.adapters.PredictionAdapter
import com.example.derinogrenme.services.FirestoreService
import com.example.derinogrenme.services.StorageService
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.google.android.material.chip.Chip
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.derinogrenme.models.Prediction
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.TextView
import com.example.derinogrenme.R

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestoreService: FirestoreService
    private lateinit var storageService: StorageService
    private lateinit var predictionAdapter: PredictionAdapter
    private var allPredictions: List<Prediction> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("tr"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        firestoreService = FirestoreService()
        storageService = StorageService(requireContext())
        
        setupRecyclerView()
        setupFilterChips()
        loadPredictions()
    }

    private fun setupRecyclerView() {
        predictionAdapter = PredictionAdapter(emptyList(), storageService)
        binding.predictionsRecyclerView.apply {
            adapter = predictionAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupFilterChips() {
        // Filtreleri temizle butonu
        binding.clearFiltersButton.setOnClickListener {
            binding.resultFilterChipGroup.clearCheck()
            binding.dateFilterChipGroup.clearCheck()
            binding.allChip.isChecked = true
            applyFilters()
        }

        // Sonuç filtresi için chip listener'ları
        binding.resultFilterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                // Hiçbir chip seçili değilse "Tümü" chip'ini seç
                binding.allChip.isChecked = true
            } else {
                // Diğer chip'lerden biri seçilirse "Tümü" chip'ini kaldır
                if (checkedIds.contains(binding.allChip.id)) {
                    group.clearCheck()
                    binding.allChip.isChecked = true
                }
            }
            applyFilters()
        }

        // Tarih filtresi için chip listener'ları
        binding.dateFilterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.size > 1) {
                // Birden fazla seçim varsa, en son seçileni tut
                val lastCheckedId = checkedIds.last()
                group.clearCheck()
                group.check(lastCheckedId)
            }
            applyFilters()
        }
    }

    private fun loadPredictions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.isVisible = true
                binding.emptyStateLayout.isVisible = false
                
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Log.e("HistoryFragment", "Kullanıcı oturumu bulunamadı")
                    showError("Oturum hatası. Lütfen tekrar giriş yapın.")
                    return@launch
                }

                Log.d("HistoryFragment", "Tahminler yükleniyor... UserId: $userId")
                allPredictions = firestoreService.getRecentPredictions(userId, 100)
                Log.d("HistoryFragment", "${allPredictions.size} tahmin yüklendi")
                
                if (allPredictions.isEmpty()) {
                    Log.d("HistoryFragment", "Hiç tahmin bulunamadı")
                    binding.emptyStateLayout.isVisible = true
                    binding.predictionsRecyclerView.isVisible = false
                } else {
                    Log.d("HistoryFragment", "Tahminler başarıyla yüklendi")
                    binding.emptyStateLayout.isVisible = false
                    binding.predictionsRecyclerView.isVisible = true
                    applyFilters()
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Tahminler yüklenirken hata oluştu", e)
                showError("Tahminler yüklenirken bir hata oluştu: ${e.message}")
            } finally {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun applyFilters() {
        var filteredPredictions = allPredictions

        // Sonuç filtresi uygula
        val selectedResultChip = binding.resultFilterChipGroup.findViewById<Chip>(
            binding.resultFilterChipGroup.checkedChipId
        )
        if (selectedResultChip != null && selectedResultChip.id != binding.allChip.id) {
            filteredPredictions = filteredPredictions.filter { prediction -> 
                prediction.result == selectedResultChip.text.toString() 
            }
        }

        // Tarih filtresi uygula
        val selectedDateChip = binding.dateFilterChipGroup.findViewById<Chip>(
            binding.dateFilterChipGroup.checkedChipId
        )
        if (selectedDateChip != null) {
            val calendar = Calendar.getInstance()
            val currentTime = calendar.time

            filteredPredictions = filteredPredictions.filter { prediction ->
                val predictionDate = dateFormat.parse(prediction.date)
                when (selectedDateChip.id) {
                    binding.todayChip.id -> {
                        calendar.time = currentTime
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        predictionDate?.after(calendar.time) ?: false
                    }
                    binding.weekChip.id -> {
                        calendar.time = currentTime
                        calendar.add(Calendar.WEEK_OF_YEAR, -1)
                        predictionDate?.after(calendar.time) ?: false
                    }
                    binding.monthChip.id -> {
                        calendar.time = currentTime
                        calendar.add(Calendar.MONTH, -1)
                        predictionDate?.after(calendar.time) ?: false
                    }
                    else -> true
                }
            }
        }

        // Sonuçları güncelle
        updateUI(filteredPredictions)
    }

    private fun updateUI(predictions: List<Prediction>) {
        if (predictions.isEmpty()) {
            binding.emptyStateLayout.isVisible = true
            binding.predictionsRecyclerView.isVisible = false
            binding.emptyStateTitle.text = 
                if (allPredictions.isEmpty()) "Henüz tahmin yapılmamış" else "Filtrelere uygun tahmin bulunamadı"
        } else {
            binding.emptyStateLayout.isVisible = false
            binding.predictionsRecyclerView.isVisible = true
            predictionAdapter = PredictionAdapter(predictions, storageService)
            binding.predictionsRecyclerView.adapter = predictionAdapter
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        binding.emptyStateLayout.isVisible = true
        binding.predictionsRecyclerView.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 