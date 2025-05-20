package com.example.derinogrenme.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.derinogrenme.R
import com.example.derinogrenme.models.Prediction
import com.example.derinogrenme.services.StorageService

class PredictionAdapter(
    private val predictions: List<Prediction>,
    private val storageService: StorageService
) : RecyclerView.Adapter<PredictionAdapter.PredictionViewHolder>() {

    class PredictionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val resultText: TextView = view.findViewById(R.id.predictionResultText)
        val confidenceText: TextView = view.findViewById(R.id.predictionConfidenceText)
        val dateText: TextView = view.findViewById(R.id.predictionDateText)
        val predictionImage: ImageView = view.findViewById(R.id.predictionImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prediction, parent, false)
        return PredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        val prediction = predictions[position]
        
        holder.resultText.apply {
            text = "Sonuç: ${prediction.result}"
            setTextColor(android.graphics.Color.parseColor(
                if (prediction.result == "REAL") "#4CAF50" else "#F44336"
            ))
        }
        
        holder.confidenceText.text = "Güven: ${String.format("%.1f", prediction.confidence * 100)}%"
        holder.dateText.text = prediction.date

        // Resmi yükle
        prediction.imageUrl?.let { path ->
            holder.predictionImage.visibility = View.VISIBLE
            storageService.loadImage(path)?.let { bitmap ->
                holder.predictionImage.setImageBitmap(bitmap)
            } ?: run {
                holder.predictionImage.visibility = View.GONE
            }
        } ?: run {
            holder.predictionImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = predictions.size
} 