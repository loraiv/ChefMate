package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService

package com.chefmate.data.repository

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object TestHelper {
    fun mockApiService(apiService: ApiService) {
        val fields = ApiClient::class.java.declaredFields
        // Try different possible field names for lazy properties
        val possibleNames = listOf(
            "\$apiService\$delegate",
            "apiService\$delegate",
            "\$INSTANCE\$apiService\$delegate",
            "INSTANCE\$apiService\$delegate"
        )
        
        var fieldFound = false
        for (fieldName in possibleNames) {
            try {
                val field = ApiClient::class.java.getDeclaredField(fieldName)
                makeAccessible(field)
                val mockLazy = kotlin.lazy { apiService }
                field.set(null, mockLazy)
                fieldFound = true
                break
            } catch (e: NoSuchFieldException) {
                // Try next name
                continue
            } catch (e: IllegalAccessException) {
                // Try to make it accessible using different approach
                try {
                    val field = ApiClient::class.java.getDeclaredField(fieldName)
                    makeAccessible(field)
                    val mockLazy = kotlin.lazy { apiService }
                    field.set(null, mockLazy)
                    fieldFound = true
                    break
                } catch (e2: Exception) {
                    // Continue to next name
                    continue
                }
            }
        }
        
        if (!fieldFound) {
            // Last resort: try to find any field containing "apiService"
            val matchingField = fields.find { 
                it.name.contains("apiService", ignoreCase = true) 
            }
            if (matchingField != null) {
                makeAccessible(matchingField)
                try {
                    val mockLazy = kotlin.lazy { apiService }
                    matchingField.set(null, mockLazy)
                    fieldFound = true
                } catch (e: Exception) {
                    // If setting fails, try to set the value directly
                    try {
                        matchingField.set(null, apiService)
                        fieldFound = true
                    } catch (e2: Exception) {
                        // Try to remove final modifier if present
                        try {
                            val modifiersField = Field::class.java.getDeclaredField("modifiers")
                            makeAccessible(modifiersField)
                            modifiersField.setInt(matchingField, matchingField.modifiers and Modifier.FINAL.inv())
                            matchingField.set(null, apiService)
                            fieldFound = true
                        } catch (e3: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        }
        
        if (!fieldFound) {
            throw IllegalStateException(
                "Could not set apiService field in ApiClient. " +
                "Available fields: ${fields.map { it.name }.joinToString(", ")}"
            )
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
