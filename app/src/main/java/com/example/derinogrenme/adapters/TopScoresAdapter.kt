package com.example.derinogrenme.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.derinogrenme.databinding.ItemTopScoreBinding
import com.example.derinogrenme.models.GameResult
import java.text.SimpleDateFormat
import java.util.Locale

class TopScoresAdapter : RecyclerView.Adapter<TopScoresAdapter.ViewHolder>() {
    private var scores: List<GameResult> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun updateScores(newScores: List<GameResult>) {
        scores = newScores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopScoreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scores[position], position + 1)
    }

    override fun getItemCount() = scores.size

    class ViewHolder(private val binding: ItemTopScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gameResult: GameResult, position: Int) {
            binding.apply {
                rankTextView.text = "#$position"
                scoreTextView.text = "${gameResult.score} puan"
                statsTextView.text = "Doğru: ${gameResult.correctAnswers} | Yanlış: ${gameResult.wrongAnswers}"
                dateTextView.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(gameResult.timestamp.toDate())
            }
        }
    }
} 