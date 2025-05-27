package com.example.derinogrenme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.derinogrenme.databinding.ActivityMainBinding
import com.example.derinogrenme.fragments.HomeFragment
import com.example.derinogrenme.fragments.HistoryFragment
import com.example.derinogrenme.fragments.StatsFragment
import com.example.derinogrenme.fragments.ProfileFragment
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowManager
import com.example.derinogrenme.utils.NotificationScheduler
import android.content.Context

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationScheduler: NotificationScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema ayarını yükle
        loadThemePreference()
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bildirim zamanlayıcısını başlat
        notificationScheduler = NotificationScheduler(this)
        notificationScheduler.scheduleNotifications()

        // Tam ekran ve cutout alanını kullan
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        // Tema moduna göre status bar rengini ayarla
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        if (isDarkMode) {
            window.decorView.setBackgroundColor(Color.parseColor("#1E1E1E"))
            windowInsetsController?.isAppearanceLightStatusBars = false
        } else {
            window.decorView.setBackgroundColor(Color.parseColor("#FFFFFF"))
            windowInsetsController?.isAppearanceLightStatusBars = true
        }

        setSupportActionBar(binding.toolbar)
        setupBottomNavigation()

        // BottomNavigationView alt padding'ini kaldır
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            insets
        }

        // Varsayılan olarak HomeFragment'ı göster
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
    }

    private fun loadThemePreference() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
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
