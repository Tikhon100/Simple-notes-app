package com.example.lab2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lab2.screens.RegistrationScreen
import com.example.lab2.screens.HomeScreen
import com.example.lab2.screens.LoginScreen


@Composable
fun AppNavGraph(
    navController: NavHostController,
    onRegistrationClicked: () -> Unit, // функция для регистрации в базе данных
    onLoginClicked: () -> Unit // функция для входа в приложение
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Authentication.route
    ) {
        composable(Screen.Authentication.route) {
            RegistrationScreen(
                onRegistrationClicked = onRegistrationClicked,
                onLoginClicked = onLoginClicked // Pass this parameter
            )
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onBackClicked = { navController.popBackStack() },
                onLoginSuccess = onRegistrationClicked
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Authentication : Screen("authentication")
    object Login : Screen("login")
    object Home : Screen("home")
}