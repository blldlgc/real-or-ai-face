package com.example.derinogrenme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.derinogrenme.databinding.ActivityGameModeBinding
import com.example.derinogrenme.models.GameImage
import com.example.derinogrenme.services.GameImageService
import com.example.derinogrenme.services.GameResultService
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.util.Log

class GameModeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameModeBinding
    private lateinit var gameImageService: GameImageService
    private lateinit var gameResultService: GameResultService
    private var currentImage: GameImage? = null
    private var score = 0
    private var correctAnswers = 0
    private var wrongAnswers = 0
    private var gameTimer: CountDownTimer? = null
    private var imageTimer: CountDownTimer? = null
    private var timeLeft = 60L
    private var imageTimeLeft = 0L
    private var isGameActive = true
    private var isAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GameImageService'i başlat
        gameImageService = GameImageService(this)
        gameResultService = GameResultService()

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

        // En iyi skorları yükle
        loadTopScores()

        // Başlat butonunu ayarla
        binding.startGameButton.setOnClickListener {
            startGame()
        }

        // Kaydırma özelliğini ayarla
        setupSwipeGesture()
    }

    private fun loadTopScores() {
        lifecycleScope.launch {
            try {
                // Global en iyi 3 skoru al
                val globalResult = gameResultService.getTopScores(3)
                globalResult.fold(
                    onSuccess = { scores ->
                        Log.d("GameModeActivity", "Global skorlar başarıyla alındı: ${scores.map { it.score }}")
                        when (scores.size) {
                            3 -> {
                                binding.globalBestScore1.text = "1. ${scores[0].score}"
                                binding.globalBestScore2.text = "2. ${scores[1].score}"
                                binding.globalBestScore3.text = "3. ${scores[2].score}"
                            }
                            2 -> {
                                binding.globalBestScore1.text = "1. ${scores[0].score}"
                                binding.globalBestScore2.text = "2. ${scores[1].score}"
                                binding.globalBestScore3.text = "3. -"
                            }
                            1 -> {
                                binding.globalBestScore1.text = "1. ${scores[0].score}"
                                binding.globalBestScore2.text = "2. -"
                                binding.globalBestScore3.text = "3. -"
                            }
                            else -> {
                                binding.globalBestScore1.text = "1. -"
                                binding.globalBestScore2.text = "2. -"
                                binding.globalBestScore3.text = "3. -"
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("GameModeActivity", "Global en iyi skorlar yüklenirken hata oluştu", error)
                        Toast.makeText(
                            this@GameModeActivity,
                            "En iyi skorlar yüklenirken hata oluştu",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                // Kullanıcının en iyi 3 skorunu al
                val userResult = gameResultService.getUserTopScores(3)
                userResult.fold(
                    onSuccess = { scores ->
                        Log.d("GameModeActivity", "Kullanıcı skorları başarıyla alındı: ${scores.map { it.score }}")
                        when (scores.size) {
                            3 -> {
                                binding.userBestScore1.text = "1. ${scores[0].score}"
                                binding.userBestScore2.text = "2. ${scores[1].score}"
                                binding.userBestScore3.text = "3. ${scores[2].score}"
                            }
                            2 -> {
                                binding.userBestScore1.text = "1. ${scores[0].score}"
                                binding.userBestScore2.text = "2. ${scores[1].score}"
                                binding.userBestScore3.text = "3. -"
                            }
                            1 -> {
                                binding.userBestScore1.text = "1. ${scores[0].score}"
                                binding.userBestScore2.text = "2. -"
                                binding.userBestScore3.text = "3. -"
                            }
                            else -> {
                                binding.userBestScore1.text = "1. -"
                                binding.userBestScore2.text = "2. -"
                                binding.userBestScore3.text = "3. -"
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("GameModeActivity", "Kullanıcı skorları yüklenirken hata oluştu: ${error.message}", error)
                        Toast.makeText(
                            this@GameModeActivity,
                            "Kullanıcı skorları yüklenirken hata oluştu: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("GameModeActivity", "Beklenmeyen hata oluştu", e)
                Toast.makeText(
                    this@GameModeActivity,
                    "Beklenmeyen bir hata oluştu: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startGame() {
        // Önce oyunu sıfırla
        resetGame()
        
        // Resimleri yüklemeye başla
        loadImages()
        
        // Oyun ekranını göster
        binding.startScreenContainer.visibility = View.GONE
        binding.gameContainer.visibility = View.VISIBLE
        binding.gameOverContainer.visibility = View.GONE
        
        // Oyun zamanlayıcısını başlat
        startGameTimer()
    }

    private fun setupSwipeGesture() {
        var startX = 0f
        var startY = 0f
        var currentX = 0f
        val SWIPE_THRESHOLD = 100f
        val ROTATION_FACTOR = 0.03f
        val interpolator = AccelerateDecelerateInterpolator()

        binding.imageCardView.setOnTouchListener { view, event ->
            if (!isGameActive || isAnimating) return@setOnTouchListener false

            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    currentX = 0f
                    view.parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - startX
                    val deltaY = event.y - startY

                    // Yatay kaydırma eşiğini geçtiyse
                    if (abs(deltaX) > abs(deltaY)) {
                        // Kaydırma mesafesini sınırla
                        currentX = deltaX.coerceIn(-view.width.toFloat(), view.width.toFloat())
                        
                        // Kartı kaydır ve döndür
                        view.translationX = currentX
                        view.rotation = currentX * ROTATION_FACTOR
                        
                        // Opaklığı ayarla
                        val alpha = 1f - (abs(currentX) / (view.width.toFloat() * 2))
                        view.alpha = alpha.coerceIn(0.5f, 1f)
                        
                        true
                    } else {
                        false
                    }
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                    
                    if (abs(currentX) > SWIPE_THRESHOLD) {
                        // Kaydırma eşiğini geçtiyse animasyonu tamamla
                        animateCardSwipe(currentX > 0)
                    } else {
                        // Eşiği geçmediyse kartı geri getir
                        view.animate()
                            .translationX(0f)
                            .rotation(0f)
                            .alpha(1f)
                            .setDuration(300)
                            .setInterpolator(interpolator)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun animateCardSwipe(isRight: Boolean) {
        if (isAnimating) return
        isAnimating = true

        val targetX = if (isRight) binding.imageCardView.width.toFloat() * 1.5f else -binding.imageCardView.width.toFloat() * 1.5f
        val targetRotation = if (isRight) 10f else -10f
        
        // Kartı kaydır ve döndür
        binding.imageCardView.animate()
            .translationX(targetX)
            .rotation(targetRotation)
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Tahmini kontrol et
                checkAnswer(isRight)
                
                // Kartı sıfırla
                binding.imageCardView.translationX = 0f
                binding.imageCardView.rotation = 0f
                binding.imageCardView.alpha = 1f
                isAnimating = false
            }
            .start()
    }

    private fun showResult(isCorrect: Boolean, points: Int) {
        binding.resultTextView.apply {
            text = if (isCorrect) {
                setTextColor(Color.GREEN)
                "+$points puan"
            } else {
                setTextColor(Color.RED)
                "Yanlış!"
            }
            visibility = View.VISIBLE
            alpha = 0f
            
            // Fade in
            animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction {
                    // Fade out
                    postDelayed({
                        animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction { visibility = View.GONE }
                            .start()
                    }, 1000)
                }
                .start()
        }
    }

    private fun checkAnswer(userGuess: Boolean) {
        imageTimer?.cancel()
        
        currentImage?.let { image ->
            val isCorrect = userGuess == image.isReal
            
            if (isCorrect) {
                correctAnswers++
                val points = if (imageTimeLeft <= 3) 15 else 10
                score += points
                showResult(true, points)
            } else {
                wrongAnswers++
                showResult(false, 0)
            }
            
            binding.scoreTextView.text = "Puan: $score"
            loadNewImage()
        }
    }

    private fun loadImages() {
        lifecycleScope.launch {
            try {
                binding.loadingProgressBar.visibility = View.VISIBLE
                binding.startGameButton.isEnabled = false
                
                gameImageService.initialize()
                val firstImage = gameImageService.getNextImage()
                
                if (firstImage == null) {
                    Toast.makeText(
                        this@GameModeActivity,
                        "Resimler yüklenemedi. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.startGameButton.isEnabled = true
                    binding.loadingProgressBar.visibility = View.GONE
                    return@launch
                }
                
                // İlk resmi yükle
                currentImage = firstImage
                Glide.with(this@GameModeActivity)
                    .load(firstImage.url)
                    .override(512, 512)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.gameImageView)
                
                binding.loadingProgressBar.visibility = View.GONE
                binding.startGameButton.isEnabled = true
            } catch (e: Exception) {
                Log.e("GameModeActivity", "Resimler yüklenirken hata oluştu", e)
                Toast.makeText(
                    this@GameModeActivity,
                    "Resimler yüklenirken hata oluştu: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.startGameButton.isEnabled = true
                binding.loadingProgressBar.visibility = View.GONE
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
        
        binding.gameContainer.visibility = View.GONE
        binding.gameOverContainer.visibility = View.VISIBLE
        
        binding.finalScoreTextView.text = "Toplam Puan: $score"
        binding.statsTextView.text = "Doğru: $correctAnswers\nYanlış: $wrongAnswers"
        
        // Sonucu Firebase'e kaydet
        lifecycleScope.launch {
            try {
                Log.d("GameModeActivity", "Oyun sonucu kaydediliyor... Score: $score")
                
                val result = gameResultService.saveGameResult(
                    score = score,
                    correctAnswers = correctAnswers,
                    wrongAnswers = wrongAnswers,
                    totalTime = 60 - timeLeft.toInt()
                )
                
                result.fold(
                    onSuccess = { gameResult ->
                        Log.d("GameModeActivity", "Oyun sonucu başarıyla kaydedildi. DocId: ${gameResult.id}")
                        Toast.makeText(
                            this@GameModeActivity,
                            "Sonuç kaydedildi!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { error ->
                        Log.e("GameModeActivity", "Sonuç kaydedilirken hata oluştu", error)
                        Toast.makeText(
                            this@GameModeActivity,
                            "Sonuç kaydedilirken hata oluştu: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("GameModeActivity", "Beklenmeyen hata oluştu", e)
                Toast.makeText(
                    this@GameModeActivity,
                    "Beklenmeyen bir hata oluştu: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun resetGame() {
        score = 0
        correctAnswers = 0
        wrongAnswers = 0
        isGameActive = true
        timeLeft = 60L
        
        binding.scoreTextView.text = "Puan: 0"
        binding.timerTextView.text = "60"
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