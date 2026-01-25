package com.chefmate.ui.main

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chefmate.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunches() {
        // Wait for activity to load
        Thread.sleep(1000)
        
        // Check if bottom navigation is visible
        onView(withId(R.id.bottomNavigationView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationIsVisible() {
        Thread.sleep(1000)
        onView(withId(R.id.bottomNavigationView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCanNavigateToRecipesTab() {
        Thread.sleep(1000)
        onView(withId(R.id.recipeListFragment))
            .perform(click())
    }

    @Test
    fun testCanNavigateToLikedRecipesTab() {
        Thread.sleep(1000)
        onView(withId(R.id.likedRecipesFragment))
            .perform(click())
    }

    @Test
    fun testCanNavigateToShoppingListTab() {
        Thread.sleep(1000)
        onView(withId(R.id.shoppingListFragment))
            .perform(click())
    }
}
