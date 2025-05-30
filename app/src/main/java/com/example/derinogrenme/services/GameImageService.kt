package com.example.derinogrenme.services

import com.example.derinogrenme.models.GameImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bumptech.glide.Glide
import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GameImageService(private val context: Context) {
    private val gitHubService: GitHubService
    private var realImages: List<GameImage> = emptyList()
    private var fakeImages: List<GameImage> = emptyList()
    private var isInitialized = false
    private val imageCache = mutableListOf<GameImage>()
    private val CACHE_SIZE = 5 // Önbellekte tutulacak resim sayısı

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        gitHubService = retrofit.create(GitHubService::class.java)
    }

    suspend fun initialize() {
        if (isInitialized) return

        withContext(Dispatchers.IO) {
            try {
                // Gerçek resimleri al
                val realContents = gitHubService.getContents("real")
                realImages = realContents
                    .filter { it.type == "file" && it.download_url != null }
                    .map { GameImage(it.download_url!!, true, it.name) }

                // Sahte resimleri al
                val fakeContents = gitHubService.getContents("fake")
                fakeImages = fakeContents
                    .filter { it.type == "file" && it.download_url != null }
                    .map { GameImage(it.download_url!!, false, it.name) }

                isInitialized = true
                
                // İlk önbelleği oluştur
                fillCache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fillCache() = coroutineScope {
        val allImages = (realImages + fakeImages).shuffled()
        imageCache.clear()
        
        // CACHE_SIZE kadar resmi önbelleğe al
        allImages.take(CACHE_SIZE).forEach { image ->
            imageCache.add(image)
            // Resmi önceden indir
            async(Dispatchers.IO) {
                try {
                    Glide.with(context)
                        .load(image.url)
                        .preload()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun getNextImage(): GameImage? {
        if (!isInitialized || imageCache.isEmpty()) return null

        // Önbellekten bir resim al
        val image = imageCache.removeAt(0)
        
        // Yeni bir resim ekle
        val allImages = (realImages + fakeImages).shuffled()
        val newImage = allImages.firstOrNull { it !in imageCache }
        newImage?.let {
            imageCache.add(it)
            // Yeni resmi önceden indir
            withContext(Dispatchers.IO) {
                try {
                    Glide.with(context)
                        .load(it.url)
                        .preload()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return image
    }

    fun getAllImages(): List<GameImage> {
        if (!isInitialized) return emptyList()
        return (realImages + fakeImages).shuffled()
    }

    fun getRandomImage(): GameImage? {
        if (!isInitialized) return null
        return getAllImages().randomOrNull()
    }

    fun getRandomImages(count: Int): List<GameImage> {
        if (!isInitialized) return emptyList()
        return getAllImages().shuffled().take(count)
    }
} 