package com.example.derinogrenme.adapters

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.derinogrenme.R
import com.example.derinogrenme.databinding.ItemPredictionBinding
import com.example.derinogrenme.databinding.DialogImageViewerBinding
import com.example.derinogrenme.models.Prediction
import com.example.derinogrenme.services.StorageService
import java.text.SimpleDateFormat
import java.util.*

class PredictionAdapter(
    private var predictions: List<Prediction>
) : RecyclerView.Adapter<PredictionAdapter.PredictionViewHolder>() {

    class PredictionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.predictionImage)
        val resultText: TextView = view.findViewById(R.id.resultText)
        val confidenceText: TextView = view.findViewById(R.id.confidenceText)
        val dateText: TextView = view.findViewById(R.id.dateText)

        fun showFullImageDialog(bitmap: android.graphics.Bitmap) {
            val dialog = Dialog(itemView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val dialogBinding = DialogImageViewerBinding.inflate(LayoutInflater.from(itemView.context))
            
            dialog.setContentView(dialogBinding.root)
            
            dialogBinding.fullImageView.setImageBitmap(bitmap)
            dialogBinding.closeButton.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prediction, parent, false)
        return PredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        val prediction = predictions[position]
        
        // Resmi yükle
        Glide.with(holder.imageView.context)
            .load(prediction.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .into(holder.imageView)

        // Sonuç ve güven oranını ayarla
        holder.resultText.text = prediction.result
        holder.confidenceText.text = "Güven: ${(prediction.confidence * 100).toInt()}%"

        // Tarihi formatla
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("tr"))
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("tr"))
            .format(Date(prediction.timestamp.seconds * 1000))
        holder.dateText.text = date
    }

    override fun getItemCount() = predictions.size

    fun updatePredictions(newPredictions: List<Prediction>) {
        predictions = newPredictions
        notifyDataSetChanged()
    }
} 