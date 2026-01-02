package com.chefmate.utils

object Constants {
    // За Android Emulator използвай 10.0.2.2, за реален телефон използвай IP адреса на компютъра
    const val BASE_URL = "http://10.0.2.2:8090/"
    const val SHARED_PREFS_NAME = "ChefMatePrefs"
    const val KEY_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    
    // AI Assistant
    const val AI_VOICE_SPEED = 1.0f
    const val AI_VOICE_PITCH = 1.0f
}