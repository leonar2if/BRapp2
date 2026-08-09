package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.admin.AdminHomeScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.client.ClientHomeScreen
import com.example.ui.viewmodels.AdminViewModel
import com.example.ui.viewmodels.AuthState
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.ClientViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    clientViewModel: ClientViewModel,
    adminViewModel: AdminViewModel
) {
    val sessionReady by authViewModel.sessionReady.collectAsState()

    // Wait for the app-start session restore attempt (real access token exchange)
    // before deciding whether to show login or a home screen. Without this, a user
    // with a persisted session would land on a "logged in" screen whose API calls
    // have no valid Authorization header yet (see P1).
    if (!sessionReady) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    val userId by authViewModel.userId.collectAsState()

    val startRoute = remember(userId, userRole) {
        when {
            userId.isNotEmpty() && userRole == "admin" -> "admin_home"
            userId.isNotEmpty() -> "client_home"
            else -> "login"
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val role = (authState as AuthState.Success).profile.role
            if (role == "admin") {
                adminViewModel.refreshData()
                navController.navigate("admin_home") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                clientViewModel.refreshData()
                navController.navigate("client_home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
    ) {
        composable("login") {
            LoginScreen(
                authState = authState,
                onLoginClick = { phone, pass ->
                    authViewModel.login(phone, pass)
                },
                onNavigateToRegister = {
                    authViewModel.resetState()
                    navController.navigate("register")
                },
                onResetError = {
                    authViewModel.resetState()
                }
            )
        }

        composable("register") {
            RegisterScreen(
                authState = authState,
                onRegisterClick = { phone, fullName, pass, confirmPass ->
                    authViewModel.register(phone, fullName, pass, confirmPass)
                },
                onBackClick = {
                    authViewModel.resetState()
                    navController.popBackStack()
                },
                onResetError = {
                    authViewModel.resetState()
                }
            )
        }

        composable("client_home") {
            ClientHomeScreen(
                clientViewModel = clientViewModel,
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    clientViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("admin_home") {
            AdminHomeScreen(
                adminViewModel = adminViewModel,
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    adminViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
