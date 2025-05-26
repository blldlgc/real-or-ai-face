package com.example.derinogrenme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.derinogrenme.databinding.ActivityMainBinding
import com.example.derinogrenme.fragments.HomeFragment
import com.example.derinogrenme.fragments.HistoryFragment
import com.example.derinogrenme.fragments.StatsFragment
import com.example.derinogrenme.fragments.ProfileFragment
import android.view.WindowManager
import android.graphics.Color
import android.view.WindowInsetsController
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar ayarlarını yap
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        // Tema moduna göre status bar rengini ayarla
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        if (isDarkMode) {
            window.decorView.setBackgroundColor(Color.parseColor("#1E1E1E"))
            windowInsetsController.isAppearanceLightStatusBars = false
        } else {
            window.decorView.setBackgroundColor(Color.parseColor("#FFFFFF"))
            windowInsetsController.isAppearanceLightStatusBars = true
        }

        setSupportActionBar(binding.toolbar)
        setupBottomNavigation()

        // Varsayılan olarak HomeFragment'ı göster
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment(), false)
                    true
                }
                R.id.navigation_history -> {
                    loadFragment(HistoryFragment(), false)
                    true
                }
                R.id.navigation_stats -> {
                    loadFragment(StatsFragment(), false)
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment(), false)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        // Önce tüm fragment'ları temizle
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.slide_out
            )
            .replace(R.id.fragmentContainer, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }
}
