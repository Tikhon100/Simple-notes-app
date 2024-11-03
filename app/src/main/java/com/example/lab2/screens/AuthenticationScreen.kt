package com.example.lab2.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val database = FirebaseDatabase.getInstance().reference

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        _currentUser.value = auth.currentUser

        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun setAuthenticated() {
        _isAuthenticated.value = true
    }

    fun resetAuthentication() {
        _isAuthenticated.value = false
    }
}

@Composable
fun RegistrationScreen(
    onRegistrationClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onRegistrationClicked()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.length > 4 && password.length > 4 && username.length > 4) {
                        SharedPrefsHelper.saveLoginAndPassword(email, password)
                        isRegistering = true
                        registerUserInFirebase(
                            email,
                            username,
                            password,
                            viewModel = viewModel,
                            onRegistrationFailure = { error ->
                                errorMessage = error
                                isRegistering = false
                            }
                        )
                    }
                }
            ) {
                Text("Register")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLoginClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account?")
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun registerUserInFirebase(
    email: String,
    username: String,
    password: String,
    viewModel: AuthViewModel,
    onRegistrationFailure: (String?) -> Unit
) {
    val auth = Firebase.auth
    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { authResult ->
            val user = authResult.user
            val userId = user?.uid

            if (userId != null) {
                val userMap = mapOf(
                    "email" to email,
                    "username" to username
                )

                // Устанавливаем флаг аутентификации сразу после создания пользователя
                viewModel.setAuthenticated()

                // Запись в базу данных происходит параллельно
                database.child("users").child(userId).setValue(userMap)
                    .addOnFailureListener { e ->
                        user.delete().addOnCompleteListener {
                            Log.e("Firebase", "Database write failed", e)
                            onRegistrationFailure("Database error: ${e.message}")
                            // Сбрасываем флаг аутентификации в случае ошибки
                            viewModel.resetAuthentication()
                        }
                    }
            } else {
                onRegistrationFailure("User ID is null")
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Registration failed", e)
            onRegistrationFailure("Registration error: ${e.message}")
        }
}

@Composable
fun LoginScreen(
    onBackClicked: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        SharedPrefsHelper.saveLoginAndPassword(email, password)
                        isLoggingIn = true
                        loginUser(
                            email,
                            password,
                            viewModel,
                            onLoginFailure = { error ->
                                errorMessage = error
                                isLoggingIn = false
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBackClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun loginUser(
    email: String,
    password: String,
    viewModel: AuthViewModel,
    onLoginFailure: (String) -> Unit
) {
    val auth = Firebase.auth
    auth.signInWithEmailAndPassword(email, password)
        .addOnSuccessListener {
            viewModel.setAuthenticated()
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Login failed", e)
            when (e) {
                is FirebaseAuthInvalidUserException -> {
                    onLoginFailure("User not found")
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    onLoginFailure("Invalid email or password")
                }
                else -> {
                    onLoginFailure("Login failed: ${e.message}")
                }
            }
        }
}