package com.example.derinogrenme.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.derinogrenme.databinding.ItemPredictionBinding
import com.example.derinogrenme.models.Prediction
import com.example.derinogrenme.services.StorageService

class PredictionAdapter(
    private val predictions: List<Prediction>,
    private val storageService: StorageService
) : ListAdapter<Prediction, PredictionAdapter.PredictionViewHolder>(PredictionDiffCallback()) {

    init {
        submitList(predictions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val binding = ItemPredictionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PredictionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PredictionViewHolder(
        private val binding: ItemPredictionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(prediction: Prediction) {
            binding.apply {
                resultTextView.text = "Sonuç: ${prediction.result}"
                confidenceTextView.text = "Güven Oranı: ${String.format("%.1f", prediction.confidence * 100)}%"
                dateTextView.text = prediction.date

                // Görseli yükle
                prediction.imageUrl?.let { imageUrl ->
                    val bitmap = storageService.loadImage(imageUrl)
                    bitmap?.let { predictionImageView.setImageBitmap(it) }
                }
            }
        }
    }

    private class PredictionDiffCallback : DiffUtil.ItemCallback<Prediction>() {
        override fun areItemsTheSame(oldItem: Prediction, newItem: Prediction): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Prediction, newItem: Prediction): Boolean {
            return oldItem == newItem
        }
    }
} 