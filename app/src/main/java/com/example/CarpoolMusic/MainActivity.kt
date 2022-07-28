package com.example.CarpoolMusic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.CarpoolMusic.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

//        findViewById<Button>(R.id.settingButton).visibility = Button.INVISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit().putString(SpotifyTokens.ACCESS_TOKEN, "").apply()
    }
}

//ToDo: When closing app, remove user from room