package com.chefmate.ui.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chefmate.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun testLoginActivityLaunches() {
        onView(withId(R.id.emailEditText))
            .check(matches(isDisplayed()))
        onView(withId(R.id.passwordEditText))
            .check(matches(isDisplayed()))
        onView(withId(R.id.loginButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testUserCanEnterEmail() {
        onView(withId(R.id.emailEditText))
            .perform(typeText("test@example.com"))
            .check(matches(withText("test@example.com")))
    }

    @Test
    fun testUserCanEnterPassword() {
        onView(withId(R.id.passwordEditText))
            .perform(typeText("password123"))
            .check(matches(withText("password123")))
    }

    @Test
    fun testLoginButtonIsClickable() {
        onView(withId(R.id.loginButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testRegisterButtonIsDisplayed() {
        onView(withId(R.id.registerButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testForgotPasswordLinkIsDisplayed() {
        onView(withId(R.id.forgotPasswordTextView))
            .check(matches(isDisplayed()))
    }
}
