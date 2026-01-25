package com.chefmate.ui.recipes

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.chefmate.R
import com.chefmate.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RecipeListFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testRecipeListFragmentLaunches() {
        // Wait for fragment to load
        Thread.sleep(2000)
        
        // Check if search input is visible
        onView(withId(R.id.searchInputLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSearchInputIsVisible() {
        Thread.sleep(2000)
        onView(withId(R.id.searchEditText))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testUserCanTypeInSearch() {
        Thread.sleep(2000)
        onView(withId(R.id.searchEditText))
            .perform(typeText("pasta"))
            .check(matches(withText("pasta")))
    }

    @Test
    fun testRecipesRecyclerViewIsVisible() {
        Thread.sleep(2000)
        onView(withId(R.id.recipesRecyclerView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testFiltersLayoutIsVisible() {
        Thread.sleep(2000)
        onView(withId(R.id.filtersLayout))
            .check(matches(isDisplayed()))
    }
}
