package com.example.lab2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.lab2.navigation.AppNavGraph
import com.example.lab2.navigation.Screen
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SharedPrefsHelper.init(this)
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("Firebase", "Firebase initialization failed", e)
        }
        setContent {
            navController = rememberNavController()

            AppNavGraph(
                navController = navController,
                onRegistrationClicked = { navigateToHome() },
                onLoginClicked = { navigateToLogin() }
            )
        }
    }

    private fun navigateToHome() {
        // Выполнить любые необходимые действия после успешной авторизации
        // и затем перейти к главному экрану приложения
        navController.navigate(Screen.Home.route)
    }
    private fun navigateToLogin() {
        navController.navigate(Screen.Login.route)
    }
}