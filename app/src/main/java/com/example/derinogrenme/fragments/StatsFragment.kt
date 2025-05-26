package com.example.derinogrenme.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.derinogrenme.databinding.FragmentStatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.derinogrenme.services.FirestoreService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import com.example.derinogrenme.models.Prediction

class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestoreService: FirestoreService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestoreService = FirestoreService()
        loadStats()
    }

    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    Toast.makeText(requireContext(), "Kullanıcı oturumu bulunamadı", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val predictions = firestoreService.getRecentPredictions(user.uid, 10000)
                updateStats(predictions)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "İstatistikler yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats(predictions: List<Prediction>) {
        if (predictions.isEmpty()) {
            binding.statsFakeCount.text = "0 (0%)"
            binding.statsRealCount.text = "0 (0%)"
            binding.statsAvgConfidence.text = "0.00%"
            binding.statsLastPrediction.text = "-"
            binding.successRateProgress.progress = 0
            binding.successRateText.text = "0%"
            return
        }

        val totalPredictions = predictions.size
        val fakeCount = predictions.count { it.result == "FAKE" }
        val realCount = predictions.count { it.result == "REAL" }
        val fakePercentage = (fakeCount.toFloat() / totalPredictions * 100).toInt()
        val realPercentage = (realCount.toFloat() / totalPredictions * 100).toInt()

        binding.statsFakeCount.text = "$fakeCount ($fakePercentage%)"
        binding.statsRealCount.text = "$realCount ($realPercentage%)"

        val avgConfidence = predictions.map { it.confidence }.average()
        binding.statsAvgConfidence.text = String.format("%.2f%%", avgConfidence * 100)

        val lastPrediction = predictions.maxByOrNull { it.date }
        binding.statsLastPrediction.text = lastPrediction?.date ?: "-"

        binding.successRateProgress.progress = realPercentage
        binding.successRateText.text = "$realPercentage%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 