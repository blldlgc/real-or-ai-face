package com.example.derinogrenme.fragments

import android.app.DatePickerDialog
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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
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
    private lateinit var customDateLayout: View
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private val predictions = mutableListOf<Prediction>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var startDate: Date? = null
    private var endDate: Date? = null

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

        // Filtre butonu tıklama işlevselliği
        view.findViewById<View>(R.id.filterButton).setOnClickListener {
            showFilterBottomSheet()
        }

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
        customDateLayout = bottomSheetView.findViewById(R.id.customDateLayout)
        startDateInput = bottomSheetView.findViewById(R.id.startDateInput)
        endDateInput = bottomSheetView.findViewById(R.id.endDateInput)

        setupDatePickers()

        // Mevcut filtreleri bottom sheet'te göster
        selectedResultChip.text.toString().replace("Sonuç: ", "").let { selectedResult ->
            if (selectedResult != "Tümü") {
                resultChipGroup.findViewById<Chip>(R.id.allChip)?.isChecked = false
                when (selectedResult) {
                    "REAL" -> resultChipGroup.findViewById<Chip>(R.id.realChip)?.isChecked = true
                    "FAKE" -> resultChipGroup.findViewById<Chip>(R.id.fakeChip)?.isChecked = true
                }
            } else {
                resultChipGroup.findViewById<Chip>(R.id.allChip)?.isChecked = true
            }
        }

        selectedDateChip.text.toString().replace("Tarih: ", "").let { selectedDate ->
            if (selectedDate != "Tümü") {
                dateChipGroup.findViewById<Chip>(R.id.dateAllChip)?.isChecked = false
                when (selectedDate) {
                    "Son 24 Saat" -> dateChipGroup.findViewById<Chip>(R.id.lastDayChip)?.isChecked = true
                    "Son 7 Gün" -> dateChipGroup.findViewById<Chip>(R.id.lastWeekChip)?.isChecked = true
                    "Son 30 Gün" -> dateChipGroup.findViewById<Chip>(R.id.lastMonthChip)?.isChecked = true
                    "Özel Tarih" -> {
                        dateChipGroup.findViewById<Chip>(R.id.customDateChip)?.isChecked = true
                        customDateLayout.visibility = View.VISIBLE
                    }
                }
            } else {
                dateChipGroup.findViewById<Chip>(R.id.dateAllChip)?.isChecked = true
            }
        }

        dateChipGroup.setOnCheckedChangeListener { group, checkedId ->
            customDateLayout.visibility = if (checkedId == R.id.customDateChip) View.VISIBLE else View.GONE
        }

        clearFiltersButton.setOnClickListener {
            resultChipGroup.clearCheck()
            dateChipGroup.clearCheck()
            resultChipGroup.findViewById<Chip>(R.id.allChip)?.isChecked = true
            dateChipGroup.findViewById<Chip>(R.id.dateAllChip)?.isChecked = true
            customDateLayout.visibility = View.GONE
            startDate = null
            endDate = null
            startDateInput.text?.clear()
            endDateInput.text?.clear()
            
            selectedResultChip.visibility = View.GONE
            selectedDateChip.visibility = View.GONE
            
            applyFilters()
        }

        applyFiltersButton.setOnClickListener {
            val selectedResult = resultChipGroup.findViewById<Chip>(resultChipGroup.checkedChipId)?.text.toString()
            val selectedDate = dateChipGroup.findViewById<Chip>(dateChipGroup.checkedChipId)?.text.toString()

            if (selectedResult != "Tümü") {
                selectedResultChip.text = "Sonuç: $selectedResult"
                selectedResultChip.visibility = View.VISIBLE
            } else {
                selectedResultChip.visibility = View.GONE
            }

            if (selectedDate != "Tümü") {
                selectedDateChip.text = "Tarih: $selectedDate"
                selectedDateChip.visibility = View.VISIBLE
            } else {
                selectedDateChip.visibility = View.GONE
            }

            applyFilters()
            filterBottomSheet.dismiss()
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        
        startDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    startDate = calendar.time
                    startDateInput.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        endDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    endDate = calendar.time
                    endDateInput.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
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
        val selectedResult = if (selectedResultChip.visibility == View.VISIBLE) {
            selectedResultChip.text.toString().replace("Sonuç: ", "")
        } else {
            "Tümü"
        }

        val selectedDate = if (selectedDateChip.visibility == View.VISIBLE) {
            selectedDateChip.text.toString().replace("Tarih: ", "")
        } else {
            "Tümü"
        }

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
                    "Son 24 Saat" -> {
                        calendar.add(Calendar.HOUR_OF_DAY, -24)
                        predictionDate.after(calendar.time)
                    }
                    "Son 7 Gün" -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -7)
                        predictionDate.after(calendar.time)
                    }
                    "Son 30 Gün" -> {
                        calendar.add(Calendar.DAY_OF_YEAR, -30)
                        predictionDate.after(calendar.time)
                    }
                    "Özel Tarih" -> {
                        if (startDate != null && endDate != null) {
                            predictionDate.after(startDate) && predictionDate.before(endDate)
                        } else {
                            true
                        }
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