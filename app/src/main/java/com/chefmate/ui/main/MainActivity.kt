package com.chefmate.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.chefmate.R
import com.chefmate.databinding.ActivityMainBinding
import com.chefmate.ui.auth.LoginActivity
import com.chefmate.utils.TokenManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        tokenManager = TokenManager(this)
        
        if (!tokenManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        
        // Handle navigation extras after view is created
        binding.root.post {
            handleIntentExtras()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(com.chefmate.R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)
    }

    private fun handleIntentExtras() {
        try {
            val navigateTo = intent.getStringExtra("navigateTo")
            if (navigateTo == "userRecipes") {
                val userId = intent.getLongExtra("userId", -1L)
                val username = intent.getStringExtra("username") ?: "Рецепти"
                if (userId != -1L && userId > 0) {
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(com.chefmate.R.id.nav_host_fragment) as? NavHostFragment
                    if (navHostFragment != null) {
                        val navController = navHostFragment.navController
                        val bundle = Bundle().apply {
                            putLong("userId", userId)
                            putString("username", username)
                        }
                        navController.navigate(com.chefmate.R.id.userRecipesFragment, bundle)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error handling intent extras", e)
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}