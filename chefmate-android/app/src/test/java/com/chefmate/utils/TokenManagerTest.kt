package com.chefmate.utils

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class TokenManagerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var tokenManager: TokenManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(context.getSharedPreferences(any(), any())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)
        whenever(editor.apply()).then {}
        whenever(editor.commit()).thenReturn(true)
        
        tokenManager = TokenManager(context)
    }

    @Test
    fun `saveToken should save token to shared preferences`() {
        val token = "test_token"
        tokenManager.saveToken(token)
        verify(editor).putString("auth_token", token)
        verify(editor).apply()
    }

    @Test
    fun `getToken should return saved token`() {
        whenever(sharedPreferences.getString("auth_token", null)).thenReturn("test_token")
        val token = tokenManager.getToken()
        assert(token == "test_token")
    }

    @Test
    fun `clearToken should remove all auth data`() {
        tokenManager.clearToken()
        verify(editor).remove("auth_token")
        verify(editor).remove("user_id")
        verify(editor).remove("user_username")
        verify(editor).remove("user_email")
        verify(editor).apply()
    }

    @Test
    fun `isLoggedIn should return true when token exists`() {
        whenever(sharedPreferences.getString("auth_token", null)).thenReturn("test_token")
        assert(tokenManager.isLoggedIn())
    }

    @Test
    fun `isLoggedIn should return false when token is null`() {
        whenever(sharedPreferences.getString("auth_token", null)).thenReturn(null)
        assert(!tokenManager.isLoggedIn())
    }

    @Test
    fun `saveUsername should save username`() {
        val username = "testuser"
        tokenManager.saveUsername(username)
        verify(editor).putString("user_username", username)
        verify(editor).apply()
    }

    @Test
    fun `getUsername should return saved username`() {
        whenever(sharedPreferences.getString("user_username", null)).thenReturn("testuser")
        val username = tokenManager.getUsername()
        assert(username == "testuser")
    }

    @Test
    fun `isAdmin should return true when role is ADMIN`() {
        whenever(sharedPreferences.getString("user_role", null)).thenReturn("ADMIN")
        assert(tokenManager.isAdmin())
    }

    @Test
    fun `isAdmin should return false when role is not ADMIN`() {
        whenever(sharedPreferences.getString("user_role", null)).thenReturn("USER")
        assert(!tokenManager.isAdmin())
    }

    @Test
    fun `saveAutoSpeakEnabled should save auto speak setting`() {
        tokenManager.saveAutoSpeakEnabled(true)
        verify(editor).putBoolean("ai_auto_speak", true)
        verify(editor).apply()
    }
}
