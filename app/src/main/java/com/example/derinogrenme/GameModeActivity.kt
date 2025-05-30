package com.example.derinogrenme

import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.derinogrenme.databinding.ActivityGameModeBinding
import com.example.derinogrenme.models.GameImage
import com.example.derinogrenme.services.GameImageService
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class GameModeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameModeBinding
    private lateinit var gameImageService: GameImageService
    private var currentImage: GameImage? = null
    private var score = 0
    private var correctAnswers = 0
    private var wrongAnswers = 0
    private var gameTimer: CountDownTimer? = null
    private var imageTimer: CountDownTimer? = null
    private var timeLeft = 60L
    private var imageTimeLeft = 0L
    private var isGameActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GameImageService'i başlat
        gameImageService = GameImageService(this)

        // Status bar rengini ayarla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }

        // Toolbar'ı ayarla
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Oyun Modu"

        // Resimleri yükle
        loadImages()

        // Butonları ayarla
        setupButtons()

        // Oyun süresini başlat
        startGameTimer()
    }

    private fun loadImages() {
        lifecycleScope.launch {
            try {
                gameImageService.initialize()
                loadNewImage()
            } catch (e: Exception) {
                Toast.makeText(this@GameModeActivity, "Resimler yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadNewImage() {
        lifecycleScope.launch {
            currentImage = gameImageService.getNextImage()
            currentImage?.let { image ->
                Glide.with(this@GameModeActivity)
                    .load(image.url)
                    .override(512, 512)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.gameImageView)
                
                startImageTimer()
            }
        }
    }

    private fun setupButtons() {
        binding.realButton.setOnClickListener {
            if (isGameActive) checkAnswer(true)
        }

        binding.fakeButton.setOnClickListener {
            if (isGameActive) checkAnswer(false)
        }

        binding.playAgainButton.setOnClickListener {
            resetGame()
        }
    }

    private fun checkAnswer(userGuess: Boolean) {
        imageTimer?.cancel()
        
        currentImage?.let { image ->
            val isCorrect = userGuess == image.isReal
            
            if (isCorrect) {
                correctAnswers++
                score += if (imageTimeLeft <= 3) { // İlk 3 saniye içinde
                    15
                } else {
                    10
                }
                Toast.makeText(this, "Doğru! +${if (imageTimeLeft <= 3) "15" else "10"} puan", Toast.LENGTH_SHORT).show()
            } else {
                wrongAnswers++
                Toast.makeText(this, "Yanlış! Doğru cevap: ${if (image.isReal) "Gerçek" else "Sahte"}", Toast.LENGTH_SHORT).show()
            }
            
            binding.scoreTextView.text = "Puan: $score"
            loadNewImage()
        }
    }

    private fun startGameTimer() {
        timeLeft = 60L
        binding.timerTextView.text = timeLeft.toString()
        
        gameTimer?.cancel()
        gameTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished / 1000
                binding.timerTextView.text = timeLeft.toString()
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun startImageTimer() {
        imageTimeLeft = 0L
        imageTimer?.cancel()
        imageTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                imageTimeLeft++
            }

            override fun onFinish() {
                // Bu timer süresiz çalışacak
            }
        }.start()
    }

    private fun endGame() {
        isGameActive = false
        gameTimer?.cancel()
        imageTimer?.cancel()
        
        binding.buttonContainer.visibility = View.GONE
        binding.gameOverContainer.visibility = View.VISIBLE
        
        binding.finalScoreTextView.text = "Toplam Puan: $score"
        binding.statsTextView.text = "Doğru: $correctAnswers\nYanlış: $wrongAnswers"
    }

    private fun resetGame() {
        score = 0
        correctAnswers = 0
        wrongAnswers = 0
        isGameActive = true
        
        binding.buttonContainer.visibility = View.VISIBLE
        binding.gameOverContainer.visibility = View.GONE
        binding.scoreTextView.text = "Puan: 0"
        
        startGameTimer()
        loadNewImage()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        gameTimer?.cancel()
        imageTimer?.cancel()
    }
} 