package com.chefmate

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.chefmate.utils.TokenManager

class ChefMateApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Apply saved theme mode
        val tokenManager = TokenManager(this)
        val themeMode = tokenManager.getThemeMode()
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}