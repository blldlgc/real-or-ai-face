package com.example.derinogrenme.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.derinogrenme.R
import com.example.derinogrenme.adapters.PredictionAdapter
import com.example.derinogrenme.models.Prediction
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class HistoryFragment : Fragment() {
    private lateinit var predictionsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: View
    private lateinit var emptyStateTitle: TextView
    private lateinit var selectedFiltersChipGroup: ChipGroup
    private lateinit var selectedResultChip: Chip
    private lateinit var selectedDateChip: Chip
    private lateinit var filterBottomSheet: BottomSheetDialog
    private lateinit var predictionAdapter: PredictionAdapter
    private val predictions = mutableListOf<Prediction>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupRecyclerView()
        setupFilterBottomSheet()
        loadPredictions()
    }

    private fun setupViews(view: View) {
        predictionsRecyclerView = view.findViewById(R.id.predictionsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        emptyStateTitle = view.findViewById(R.id.emptyStateTitle)
        selectedFiltersChipGroup = view.findViewById(R.id.selectedFiltersChipGroup)
        selectedResultChip = view.findViewById(R.id.selectedResultChip)
        selectedDateChip = view.findViewById(R.id.selectedDateChip)

        // Seçili filtre chip'lerinin kapatma işlevselliği
        selectedResultChip.setOnCloseIconClickListener {
            selectedResultChip.visibility = View.GONE
            applyFilters()
        }

        selectedDateChip.setOnCloseIconClickListener {
            selectedDateChip.visibility = View.GONE
            applyFilters()
        }
    }

    private fun setupRecyclerView() {
        predictionAdapter = PredictionAdapter(predictions)
        predictionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = predictionAdapter
        }
    }

    private fun setupFilterBottomSheet() {
        filterBottomSheet = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_filter, null)
        filterBottomSheet.setContentView(bottomSheetView)

        val resultChipGroup = bottomSheetView.findViewById<ChipGroup>(R.id.resultFilterChipGroup)
        val dateChipGroup = bottomSheetView.findViewById<ChipGroup>(R.id.dateFilterChipGroup)
        val clearFiltersButton = bottomSheetView.findViewById<View>(R.id.clearFiltersButton)
        val applyFiltersButton = bottomSheetView.findViewById<View>(R.id.applyFiltersButton)

        clearFiltersButton.setOnClickListener {
            resultChipGroup.clearCheck()
            dateChipGroup.clearCheck()
            resultChipGroup.findViewById<Chip>(R.id.allChip)?.isChecked = true
        }

        applyFiltersButton.setOnClickListener {
            val selectedResult = resultChipGroup.findViewById<Chip>(resultChipGroup.checkedChipId)?.text.toString()
            val selectedDate = dateChipGroup.findViewById<Chip>(dateChipGroup.checkedChipId)?.text.toString()

            selectedResultChip.text = "Sonuç: $selectedResult"
            selectedDateChip.text = "Tarih: $selectedDate"
            selectedResultChip.visibility = View.VISIBLE
            selectedDateChip.visibility = View.VISIBLE

            applyFilters()
            filterBottomSheet.dismiss()
        }
    }

    fun showFilterBottomSheet() {
        filterBottomSheet.show()
    }

    private fun loadPredictions() {
        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        val userId = auth.currentUser?.uid ?: return
        db.collection("predictions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                predictions.clear()
                for (document in documents) {
                    val prediction = document.toObject(Prediction::class.java)
                    predictions.add(prediction)
                }
                applyFilters()
            }
            .addOnFailureListener {
                // Hata durumunda kullanıcıya bilgi ver
            }
    }

    private fun applyFilters() {
        val selectedResult = selectedResultChip.text.toString().replace("Sonuç: ", "")
        val selectedDate = selectedDateChip.text.toString().replace("Tarih: ", "")

        val filteredPredictions = predictions.filter { prediction ->
            var matchesResult = true
            var matchesDate = true

            if (selectedResult != "Tümü") {
                matchesResult = prediction.result == selectedResult
            }

            if (selectedDate != "Tümü") {
                val predictionDate = Date(prediction.timestamp.seconds * 1000)
                val calendar = Calendar.getInstance()
                val today = calendar.time

                matchesDate = when (selectedDate) {
                    "Bugün" -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        predictionDate.after(calendar.time)
                    }
                    "Bu Hafta" -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, -1)
                        predictionDate.after(calendar.time)
                    }
                    "Bu Ay" -> {
                        calendar.add(Calendar.MONTH, -1)
                        predictionDate.after(calendar.time)
                    }
                    else -> true
                }
            }

            matchesResult && matchesDate
        }

        predictionAdapter.updatePredictions(filteredPredictions)
        updateEmptyState(filteredPredictions.isEmpty())
        progressBar.visibility = View.GONE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        predictionsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyStateTitle.text = if (isEmpty) "Filtrelere uygun tahmin bulunamadı" else "Henüz tahmin yapılmamış"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (filterBottomSheet.isShowing) {
            filterBottomSheet.dismiss()
        }
    }
} 