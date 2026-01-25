# Testing Documentation

This document provides an overview of all unit tests and UI tests in the ChefMate Android application.

## Overview

The project includes comprehensive test coverage for:
- **Unit Tests**: ViewModels, Repositories, and Utility classes
- **UI Tests**: Activities and Fragments using Espresso

## Test Structure

```
app/src/
├── test/                    # Unit tests (run on JVM)
│   └── java/com/chefmate/
│       ├── data/repository/ # Repository tests
│       ├── ui/              # ViewModel tests
│       └── utils/           # Utility class tests
└── androidTest/             # UI tests (run on Android device/emulator)
    └── java/com/chefmate/
        └── ui/              # Activity and Fragment tests
```

## Unit Tests

### ViewModel Tests

#### 1. AuthViewModelTest (6 tests)
Tests authentication ViewModel functionality:
- Initial state is Idle
- Login with valid credentials sets Success state
- Login with invalid credentials sets Error state
- Register with valid data sets Success state
- Register with invalid data sets Error state
- Login sets Loading state before Success

**Location**: `app/src/test/java/com/chefmate/ui/auth/viewmodel/AuthViewModelTest.kt`

#### 2. RecipeViewModelTest (9 tests)
Tests recipe listing and filtering:
- Load recipes updates recipes on success
- Load recipes sets error on failure
- Search recipes updates recipes on success
- Filter by difficulty updates selected difficulty
- Filter by time updates selected max time
- Toggle like updates recipe like status
- Toggle like unlikes recipe when already liked
- Clear error clears error state
- IsLoading is true during load

**Location**: `app/src/test/java/com/chefmate/ui/recipes/viewmodel/RecipeViewModelTest.kt`

#### 3. ShoppingListViewModelTest (7 tests)
Tests shopping list management:
- Load shopping list updates list on success
- Load shopping list sets error on failure
- Update item updates item checked status
- Add item adds new item to list
- Delete item removes item from list
- Clear error clears error state
- Clear message clears message state

**Location**: `app/src/test/java/com/chefmate/ui/shopping/viewmodel/ShoppingListViewModelTest.kt`

#### 4. AiViewModelTest (7 tests)
Tests AI chat functionality:
- Initial state has empty messages
- Send message adds user message immediately
- Send message with blank message does not add message
- Send message sets loading state
- Clear error clears error state
- Clear chat clears all messages
- IsLoading is false initially

**Location**: `app/src/test/java/com/chefmate/ui/ai/viewmodel/AiViewModelTest.kt`

#### 5. AddRecipeViewModelTest (6 tests)
Tests recipe creation and editing:
- Create recipe sets recipeCreated on success
- Create recipe sets error on failure
- Update recipe sets recipeCreated on success
- Update recipe sets error on failure
- Clear error clears error state
- Clear recipe created clears recipe created state

**Location**: `app/src/test/java/com/chefmate/ui/recipes/viewmodel/AddRecipeViewModelTest.kt`

#### 6. RecipeDetailViewModelTest (8 tests)
Tests recipe detail functionality:
- Load recipe updates recipe on success
- Load recipe sets error on failure
- Toggle like updates recipe like status
- Add to shopping list sets success message
- Delete recipe sets success message for admin
- Delete recipe sets success message for regular user
- Clear error clears error state
- Clear shopping list message clears message

**Location**: `app/src/test/java/com/chefmate/ui/recipes/viewmodel/RecipeDetailViewModelTest.kt`

### Repository Tests

#### 1. AuthRepositoryTest (4 tests)
Tests authentication repository:
- Login with valid credentials returns success
- Login with empty credentials returns failure
- Register with valid data returns success
- Register with empty data returns failure

**Location**: `app/src/test/java/com/chefmate/data/repository/AuthRepositoryTest.kt`

#### 2. RecipeRepositoryTest (8 tests)
Tests recipe repository:
- Get recipes returns list of recipes on success
- Get recipes returns failure when not authenticated
- Get recipe by ID returns recipe on success
- Like recipe returns success
- Unlike recipe returns success
- Search recipes returns filtered recipes
- Delete recipe returns success
- Delete comment returns success

**Location**: `app/src/test/java/com/chefmate/data/repository/RecipeRepositoryTest.kt`

#### 3. AiRepositoryTest (5 tests)
Tests AI repository:
- Chat with AI returns response on success
- Chat with AI returns failure when not authenticated
- Chat with AI handles HTTP errors
- Chat with AI handles network errors
- Chat with AI handles HttpException

**Location**: `app/src/test/java/com/chefmate/data/repository/AiRepositoryTest.kt`

#### 4. ShoppingRepositoryTest (6 tests)
Tests shopping repository:
- Get my shopping list returns shopping list on success
- Get my shopping list returns failure when not authenticated
- Create shopping list from recipe returns shopping list on success
- Update shopping list item returns success
- Add shopping list item returns success
- Delete shopping list item returns success

**Location**: `app/src/test/java/com/chefmate/data/repository/ShoppingRepositoryTest.kt`

#### 5. AdminRepositoryTest (3 tests)
Tests admin repository:
- Get all users returns list of users
- Block user returns success message
- Delete user returns success message

**Location**: `app/src/test/java/com/chefmate/data/repository/AdminRepositoryTest.kt`

### Utility Tests

#### 1. TokenManagerTest (11 tests)
Tests token and user data management:
- Save token saves token to shared preferences
- Get token returns saved token
- Clear token removes all auth data
- Is logged in returns true when token exists
- Is logged in returns false when token is null
- Save username saves username
- Get username returns saved username
- Is admin returns true when role is ADMIN
- Is admin returns false when role is not ADMIN
- Save auto speak enabled saves auto speak setting

**Location**: `app/src/test/java/com/chefmate/utils/TokenManagerTest.kt`

## UI Tests

### Activity Tests

#### 1. LoginActivityTest (6 tests)
Tests login activity UI:
- Login activity launches
- User can enter email
- User can enter password
- Login button is clickable
- Register button is displayed
- Forgot password link is displayed

**Location**: `app/src/androidTest/java/com/chefmate/ui/auth/LoginActivityTest.kt`

#### 2. MainActivityTest (5 tests)
Tests main activity navigation:
- Main activity launches
- Bottom navigation is visible
- Can navigate to recipes tab
- Can navigate to liked recipes tab
- Can navigate to shopping list tab

**Location**: `app/src/androidTest/java/com/chefmate/ui/main/MainActivityTest.kt`

### Fragment Tests

#### 1. RecipeListFragmentTest (5 tests)
Tests recipe list fragment:
- Recipe list fragment launches
- Search input is visible
- User can type in search
- Recipes RecyclerView is visible
- Filters layout is visible

**Location**: `app/src/androidTest/java/com/chefmate/ui/recipes/RecipeListFragmentTest.kt`

## Test Statistics

### Total Tests
- **Unit Tests**: 80 tests (79 real + 1 example)
- **UI Tests**: 17 tests (16 real + 1 example)
- **Total**: 97 tests

### Test Coverage by Category
- ViewModels: 43 tests
  - AuthViewModel: 6 tests
  - RecipeViewModel: 9 tests
  - ShoppingListViewModel: 7 tests
  - AiViewModel: 7 tests
  - AddRecipeViewModel: 6 tests
  - RecipeDetailViewModel: 8 tests
- Repositories: 26 tests
  - AuthRepository: 4 tests
  - RecipeRepository: 8 tests
  - AiRepository: 5 tests
  - ShoppingRepository: 6 tests
  - AdminRepository: 3 tests
- Utilities: 11 tests
  - TokenManager: 11 tests
- UI (Activities/Fragments): 16 tests
  - LoginActivity: 6 tests
  - MainActivity: 5 tests
  - RecipeListFragment: 5 tests
- Example tests: 2 tests

## Running Tests

### Prerequisites
1. Отвори **PowerShell** или **Command Prompt**
2. Навигирай до директорията на Android проекта:
   ```powershell
   cd C:\Users\tonyi\Desktop\ChefMate\chefmate-android
   ```

### Run All Unit Tests
Изпълни всички unit тестове (тестовете, които не се нуждаят от Android устройство):
```powershell
.\gradlew.bat test
```
или на Windows:
```powershell
gradlew.bat test
```

### Run All UI Tests
Изпълни всички UI тестове (изискват Android устройство/емулатор):
```powershell
.\gradlew.bat connectedAndroidTest
```
**Важно**: Преди това трябва да имаш стартиран Android емулатор или свързано устройство.

### Run Specific Test Class
Изпълни конкретен тест клас:
```powershell
.\gradlew.bat test --tests "com.chefmate.ui.auth.viewmodel.AuthViewModelTest"
```

### Run Tests with Coverage
Изпълни тестове с покритие (code coverage):
```powershell
.\gradlew.bat testDebugUnitTest jacocoTestReport
```

### Run Tests from Android Studio
1. Отвори проекта в Android Studio
2. Отиди до `app/src/test` или `app/src/androidTest`
3. Кликни десен бутон на тест файл или клас
4. Избери "Run 'TestName'"

### View Test Results
След изпълнение, резултатите са в:
- Unit тестове: `app/build/reports/tests/test/index.html`
- UI тестове: `app/build/reports/androidTests/connected/index.html`

## Dependencies

### Testing Dependencies
- `junit:junit:4.13.2` - JUnit testing framework
- `org.mockito:mockito-core:5.3.1` - Mocking framework
- `org.mockito:mockito-inline:5.2.0` - Inline mocking for final classes
- `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3` - Coroutines testing
- `androidx.arch.core:core-testing:2.2.0` - Architecture components testing
- `androidx.test.ext:junit:1.1.5` - Android JUnit extensions
- `androidx.test.espresso:espresso-core:3.5.1` - Espresso UI testing
- `androidx.test.espresso:espresso-intents:3.5.1` - Espresso intents
- `androidx.test.espresso:espresso-contrib:3.5.1` - Espresso contributions
- `androidx.test:runner:1.5.2` - Test runner
- `androidx.test:rules:1.5.0` - Test rules
- `androidx.fragment:fragment-testing:1.6.2` - Fragment testing
- `androidx.navigation:navigation-testing:2.7.4` - Navigation testing

## Test Best Practices

1. **Isolation**: Each test is independent and does not rely on other tests
2. **Mocking**: External dependencies are mocked using Mockito
3. **Coroutines**: Tests use `runTest` and `StandardTestDispatcher` for coroutine testing
4. **StateFlow**: ViewModel state is tested using Flow collection
5. **UI Testing**: Espresso is used for UI interaction testing

## Notes

- All ViewModel tests use `StandardTestDispatcher` for predictable coroutine execution
- Repository tests mock `ApiService` and `TokenManager` to isolate network and storage concerns
- UI tests include delays to account for fragment loading and navigation
- Tests follow the Arrange-Act-Assert pattern
