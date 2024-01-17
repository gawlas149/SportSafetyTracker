@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.sportsafetytracker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.sportsafetytracker.ui.SettingsScreen
import com.example.sportsafetytracker.ui.TrackerScreen

enum class SafetyTrackerScreen(@StringRes val title: Int) {
    Tracker(title = R.string.tracker),
    Settings(title = R.string.settings)
}

/**
 * Composable that displays the topBar and displays back button if back navigation is possible.
 */
@Composable
fun SafetyTrackerAppBar(
    currentScreen : SafetyTrackerScreen,
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
fun SafetyTrackerApp(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = SafetyTrackerScreen.valueOf(
        backStackEntry?.destination?.route ?: SafetyTrackerScreen.Tracker.name
    )

    Scaffold(
        topBar = {
            SafetyTrackerAppBar(currentScreen = currentScreen)
        },
        bottomBar = {},
    ) {innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SafetyTrackerScreen.Tracker.name,
            modifier = Modifier.padding(innerPadding)
        ){
            composable(route = SafetyTrackerScreen.Tracker.name){
                TrackerScreen(onSettingsButtonClicked = {navController.navigate(SafetyTrackerScreen.Settings.name)})
            }
            composable(route = SafetyTrackerScreen.Settings.name){
                SettingsScreen(onCancelButtonClicked = {navController.popBackStack(SafetyTrackerScreen.Tracker.name, false)})
            }
        }
    }
}


