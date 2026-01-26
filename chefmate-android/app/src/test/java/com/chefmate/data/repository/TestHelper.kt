package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object TestHelper {
    fun mockApiService(apiService: ApiService) {
        try {
            // Get all fields from ApiClient
            val declaredFields = ApiClient::class.java.declaredFields.toList()
            
            // Find the lazy delegate field for apiService
            // Kotlin lazy properties create a field named like: $apiService$delegate
            val delegateField = declaredFields.find { field ->
                field.name.contains("apiService") && field.name.contains("delegate")
            }
            
            if (delegateField != null) {
                makeAccessible(delegateField)
                // Create a new lazy that returns our mock
                val mockLazy = kotlin.lazy(LazyThreadSafetyMode.NONE) { apiService }
                delegateField.set(null, mockLazy)
                return
            }
            
            // Alternative: Try to find and replace the lazy delegate's value
            // Get the actual lazy delegate instance
            val apiServiceField = ApiClient::class.java.getDeclaredField("apiService")
            makeAccessible(apiServiceField)
            
            // Try to access the lazy delegate
            val lazyDelegate = declaredFields.find { 
                it.name.contains("apiService") && it.name.contains("delegate")
            }
            
            if (lazyDelegate != null) {
                makeAccessible(lazyDelegate)
                val currentLazy = lazyDelegate.get(null) as? kotlin.Lazy<*>
                
                if (currentLazy != null) {
                    // Try to replace the lazy delegate
                    val newLazy = kotlin.lazy(LazyThreadSafetyMode.NONE) { apiService }
                    lazyDelegate.set(null, newLazy)
                    return
                }
            }
            
            // Last resort: Use reflection to access the lazy's value directly
            // This is a hack but should work for testing
            val allFields = ApiClient::class.java.declaredFields
            for (field in allFields) {
                if (field.name.contains("apiService") || field.name.contains("delegate")) {
                    try {
                        makeAccessible(field)
                        val fieldType = field.type
                        
                        // If it's a Lazy type, replace it
                        if (kotlin.Lazy::class.java.isAssignableFrom(fieldType)) {
                            val newLazy = kotlin.lazy(LazyThreadSafetyMode.NONE) { apiService }
                            field.set(null, newLazy)
                            return
                        }
                    } catch (e: Exception) {
                        // Continue trying other fields
                    }
                }
            }
            
            // If we still haven't found it, try to set it directly (might work if not initialized)
            try {
                val directField = ApiClient::class.java.getDeclaredField("apiService")
                makeAccessible(directField)
                // Remove final modifier if present
                try {
                    val modifiersField = Field::class.java.getDeclaredField("modifiers")
                    makeAccessible(modifiersField)
                    modifiersField.setInt(directField, directField.modifiers and Modifier.FINAL.inv())
                } catch (e: Exception) {
                    // Ignore
                }
                directField.set(null, apiService)
                return
            } catch (e: Exception) {
                // Fall through to error
            }
            
            // If all else fails, throw with debug info
            val allFieldNames = declaredFields.map { "${it.name} (${it.type.simpleName})" }.joinToString(", ")
            throw IllegalStateException(
                "Could not mock apiService in ApiClient. " +
                "Available fields: $allFieldNames. " +
                "Note: This might fail if ApiClient.apiService was already initialized."
            )
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Failed to mock ApiClient.apiService: ${e.message}", e)
        }
    }
    
    private fun makeAccessible(field: Field) {
        try {
            // Try standard approach first
            field.isAccessible = true
        } catch (e: Exception) {
            // In Java 9+, we might need to use different approach
            try {
                // Try to remove final modifier using reflection
                val modifiersField = Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                val currentModifiers = modifiersField.getInt(field)
                modifiersField.setInt(field, currentModifiers and Modifier.FINAL.inv())
                field.isAccessible = true
            } catch (e2: Exception) {
                // If all else fails, try using MethodHandles (Java 9+)
                try {
                    val lookup = java.lang.invoke.MethodHandles.lookup()
                    val privateLookup = java.lang.invoke.MethodHandles.privateLookupIn(
                        field.declaringClass,
                        lookup
                    )
                    // This is just to make it accessible, we'll still use field.set()
                    field.isAccessible = true
                } catch (e3: Exception) {
                    // Last resort: just try to use it anyway
                }
            }
        }
    }
}
