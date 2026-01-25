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

        // Hide certain tabs for admin users
        val isAdmin = tokenManager.isAdmin()
        val menu = binding.bottomNavigationView.menu
        
        // Always hide admin fragment from bottom navigation (only accessible from profile)
        menu.findItem(com.chefmate.R.id.adminFragment)?.isVisible = false
        
        if (isAdmin) {
            menu.findItem(com.chefmate.R.id.likedRecipesFragment)?.isVisible = false
            menu.findItem(com.chefmate.R.id.shoppingListFragment)?.isVisible = false
            menu.findItem(com.chefmate.R.id.cookingAssistantFragment)?.isVisible = false
        }

        binding.bottomNavigationView.setupWithNavController(navController)
    }

    private fun handleIntentExtras() {
        try {
            val navigateTo = intent.getStringExtra("navigateTo")
            val navHostFragment = supportFragmentManager
                .findFragmentById(com.chefmate.R.id.nav_host_fragment) as? NavHostFragment
            val navController = navHostFragment?.navController ?: return
            
            when (navigateTo) {
                "userRecipes" -> {
                    val userId = intent.getLongExtra("userId", -1L)
                    val username = intent.getStringExtra("username") ?: "Recipes"
                    if (userId != -1L && userId > 0) {
                        val bundle = Bundle().apply {
                            putLong("userId", userId)
                            putString("username", username)
                        }
                        navController.navigate(com.chefmate.R.id.userRecipesFragment, bundle)
                    }
                }
                "addRecipe" -> {
                    navController.navigate(com.chefmate.R.id.addRecipeFragment)
                }
                "adminPanel" -> {
                    navController.navigate(com.chefmate.R.id.adminFragment)
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