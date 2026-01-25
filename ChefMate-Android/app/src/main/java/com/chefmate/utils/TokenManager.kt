package com.chefmate.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("ChefMatePrefs", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString("user_id", userId).apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString("user_id", null)
    }

    fun clearToken() {
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("user_id")
            .remove("user_username")
            .remove("user_email")
            .remove("user_first_name")
            .remove("user_last_name")
            .remove("user_role")
            .remove("profile_image_path")
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    fun saveRememberMeCredentials(email: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putString("remembered_email", email)
        editor.putString("remembered_password", password)
        editor.putBoolean("remember_me", true)
        val result = editor.commit()
        android.util.Log.d("TokenManager", "Saved remember me credentials - commit result: $result")
        android.util.Log.d("TokenManager", "Verification - Email: ${getRememberedEmail()}, Flag: ${isRememberMeEnabled()}")
    }

    fun getRememberedEmail(): String? {
        return sharedPreferences.getString("remembered_email", null)
    }

    fun getRememberedPassword(): String? {
        return sharedPreferences.getString("remembered_password", null) ?: ""
    }

    fun isRememberMeEnabled(): Boolean {
        return sharedPreferences.getBoolean("remember_me", false)
    }

    fun clearRememberMeCredentials() {
        sharedPreferences.edit()
            .remove("remembered_email")
            .remove("remembered_password")
            .remove("remember_me")
            .apply()
    }

    fun saveUserInfo(username: String, email: String, firstName: String? = null, lastName: String? = null, role: String? = null) {
        sharedPreferences.edit()
            .putString("user_username", username)
            .putString("user_email", email)
            .putString("user_first_name", firstName)
            .putString("user_last_name", lastName)
            .putString("user_role", role)
            .apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString("user_username", null)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString("user_email", null)
    }

    fun getFirstName(): String? {
        return sharedPreferences.getString("user_first_name", null)
    }

    fun getLastName(): String? {
        return sharedPreferences.getString("user_last_name", null)
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit()
            .putString("user_username", username)
            .apply()
    }

    fun clearUserInfo() {
        sharedPreferences.edit()
            .remove("user_username")
            .remove("user_email")
            .remove("user_first_name")
            .remove("user_last_name")
            .remove("user_role")
            .remove("profile_image_path")
            .apply()
    }

    fun saveProfileImagePath(path: String?) {
        if (path != null) {
            sharedPreferences.edit().putString("profile_image_path", path).apply()
        } else {
            sharedPreferences.edit().remove("profile_image_path").apply()
        }
    }

    fun getProfileImagePath(): String? {
        return sharedPreferences.getString("profile_image_path", null)
    }

    // Theme methods
    fun saveThemeMode(mode: Int) {
        sharedPreferences.edit().putInt("theme_mode", mode).apply()
    }

    fun getThemeMode(): Int {
        return sharedPreferences.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun saveAutoSpeakEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("ai_auto_speak", enabled).apply()
    }

    fun isAutoSpeakEnabled(): Boolean {
        return sharedPreferences.getBoolean("ai_auto_speak", false)
    }

    fun saveUserRole(role: String) {
        sharedPreferences.edit()
            .putString("user_role", role)
            .apply()
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString("user_role", null)
    }

    fun isAdmin(): Boolean {
        return getUserRole() == "ADMIN"
    }
}