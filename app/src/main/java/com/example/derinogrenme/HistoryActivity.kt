package com.example.derinogrenme

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.derinogrenme.fragments.HistoryFragment
import com.google.android.material.appbar.MaterialToolbar

class HistoryActivity : AppCompatActivity() {
    private lateinit var historyFragment: HistoryFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Toolbar'ı ayarla
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Fragment'ı ekle
        if (savedInstanceState == null) {
            historyFragment = HistoryFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, historyFragment)
                .commit()
        } else {
            historyFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as HistoryFragment
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_filter -> {
                historyFragment.showFilterBottomSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 