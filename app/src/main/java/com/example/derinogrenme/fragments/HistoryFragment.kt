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

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestoreService: FirestoreService
    private lateinit var storageService: StorageService
    private lateinit var predictionAdapter: PredictionAdapter

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
        loadPredictions()
    }

    private fun setupRecyclerView() {
        predictionAdapter = PredictionAdapter(emptyList(), storageService)
        binding.predictionsRecyclerView.apply {
            adapter = predictionAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        }
    }

    private fun loadPredictions() {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.e("HistoryFragment", "Kullanıcı oturumu bulunamadı")
                    return@launch
                }

                Log.d("HistoryFragment", "Tahminler yükleniyor... Kullanıcı ID: ${currentUser.uid}")
                val predictions = firestoreService.getRecentPredictions(currentUser.uid, 10000)
                Log.d("HistoryFragment", "Yüklenen tahmin sayısı: ${predictions.size}")

                if (predictions.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.predictionsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.predictionsRecyclerView.visibility = View.VISIBLE
                    predictionAdapter = PredictionAdapter(predictions, storageService)
                    binding.predictionsRecyclerView.adapter = predictionAdapter
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Tahminler yüklenirken hata oluştu", e)
                android.widget.Toast.makeText(context, "Tahminler yüklenirken hata oluştu: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 