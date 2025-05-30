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
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

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
        setupCharts()
        loadStats()
    }

    private fun getThemeColor(colorType: String): Int {
        val isDarkMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        return when (colorType) {
            "text" -> if (isDarkMode) android.R.color.white else android.R.color.black
            "grid" -> android.R.color.darker_gray
            "error" -> android.R.color.holo_red_light
            "primary" -> android.R.color.holo_blue_dark
            else -> if (isDarkMode) android.R.color.white else android.R.color.black
        }
    }

    private fun setupCharts() {
        // Günlük dağılım grafiği ayarları
        binding.dailyStatsChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(android.R.color.transparent)
            setTransparentCircleRadius(0f)
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = requireContext().getColor(getThemeColor("text"))
            setEntryLabelTextSize(12f)
            setEntryLabelColor(requireContext().getColor(getThemeColor("text")))
        }

        // Haftalık trend grafiği ayarları
        binding.weeklyStatsChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
            legend.textColor = requireContext().getColor(getThemeColor("text"))
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 10f
                textColor = requireContext().getColor(getThemeColor("text"))
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
                textColor = requireContext().getColor(getThemeColor("text"))
                gridColor = requireContext().getColor(getThemeColor("grid"))
            }

            axisRight.isEnabled = false
        }
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
                updateDailyChart(predictions)
                updateWeeklyChart(predictions)
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

        val lastPrediction = predictions.maxByOrNull { it.timestamp }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("tr"))
        val predictionDate = Date(lastPrediction?.timestamp?.seconds?.let { it * 1000 } ?: 0)
        binding.statsLastPrediction.text = lastPrediction?.let { dateFormat.format(predictionDate) } ?: "-"
    }

    private fun updateDailyChart(predictions: List<Prediction>) {
        if (predictions.isEmpty()) return

        val today = LocalDate.now()
        val todayPredictions = predictions.filter {
            val predictionDate = Date(it.timestamp.seconds * 1000).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            predictionDate == today
        }

        val fakeCount = todayPredictions.count { it.result == "FAKE" }
        val realCount = todayPredictions.count { it.result == "REAL" }

        val entries = listOf(
            PieEntry(fakeCount.toFloat(), "FAKE"),
            PieEntry(realCount.toFloat(), "REAL")
        )

        val dataSet = PieDataSet(entries, "Bugünkü Tahminler")
        dataSet.colors = listOf(
            requireContext().getColor(getThemeColor("error")),
            requireContext().getColor(getThemeColor("primary"))
        )
        dataSet.valueTextColor = requireContext().getColor(getThemeColor("text"))

        val data = PieData(dataSet)
        data.setValueTextSize(12f)
        data.setValueFormatter(PercentFormatter(binding.dailyStatsChart))
        data.setValueTextColor(requireContext().getColor(getThemeColor("text")))

        binding.dailyStatsChart.data = data
        binding.dailyStatsChart.invalidate()
    }

    private fun updateWeeklyChart(predictions: List<Prediction>) {
        if (predictions.isEmpty()) return

        val today = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        val currentWeek = today.get(weekFields.weekOfWeekBasedYear())

        val weeklyData = mutableMapOf<Int, Int>()
        for (i in 0..6) {
            weeklyData[i] = 0
        }

        predictions.forEach { prediction ->
            val predictionDate = Date(prediction.timestamp.seconds * 1000).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val predictionWeek = predictionDate.get(weekFields.weekOfWeekBasedYear())
            
            if (predictionWeek == currentWeek) {
                val dayOfWeek = predictionDate.dayOfWeek.value - 1 // 0-6 arası
                weeklyData[dayOfWeek] = weeklyData[dayOfWeek]!! + 1
            }
        }

        val entries = weeklyData.map { Entry(it.key.toFloat(), it.value.toFloat()) }
        val dataSet = LineDataSet(entries, "Haftalık Tahmin Sayısı")
        
        dataSet.apply {
            color = requireContext().getColor(getThemeColor("primary"))
            setCircleColor(requireContext().getColor(getThemeColor("primary")))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
            valueTextColor = requireContext().getColor(getThemeColor("text"))
        }

        val data = LineData(dataSet)
        binding.weeklyStatsChart.data = data

        // X ekseni etiketleri
        val days = arrayOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        binding.weeklyStatsChart.xAxis.valueFormatter = IndexAxisValueFormatter(days)

        binding.weeklyStatsChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 