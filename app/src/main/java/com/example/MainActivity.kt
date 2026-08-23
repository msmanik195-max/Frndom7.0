package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.login.LoginScreen
import com.example.ui.main.MainFeedContainer
import com.example.ui.register.RegisterScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        setContent {
            MyApplicationTheme {
                FrndomApp()
            }
        }
    }
}

@Composable
fun FrndomApp(
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle Back Press on Register screen to return to Login
    BackHandler(enabled = uiState.currentScreen == AuthScreen.REGISTER) {
        viewModel.navigateTo(AuthScreen.LOGIN)
    }

    AnimatedContent(
        targetState = uiState.currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AuthScreen.SPLASH -> {
                SplashScreen()
            }

            AuthScreen.LOGIN -> {
                LoginScreen(
                    state = uiState,
                    onIdentifierChange = viewModel::onLoginIdentifierChange,
                    onPasswordChange = viewModel::onLoginPasswordChange,
                    onTogglePasswordVisibility = viewModel::toggleLoginPasswordVisibility,
                    onLoginClick = viewModel::login,
                    onCreateAccountClick = { viewModel.navigateTo(AuthScreen.REGISTER) }
                )
            }

            AuthScreen.REGISTER -> {
                RegisterScreen(
                    state = uiState,
                    onFirstNameChange = viewModel::onRegFirstNameChange,
                    onLastNameChange = viewModel::onRegLastNameChange,
                    onIdentifierTypeChange = viewModel::onRegIdentifierTypeChange,
                    onEmailChange = viewModel::onRegEmailChange,
                    onPhoneChange = viewModel::onRegPhoneChange,
                    onGenderChange = viewModel::onRegGenderChange,
                    onBirthDateChange = viewModel::onRegBirthDateChange,
                    onPasswordChange = viewModel::onRegPasswordChange,
                    onConfirmPasswordChange = viewModel::onRegConfirmPasswordChange,
                    onTogglePasswordVisibility = viewModel::toggleRegPasswordVisibility,
                    onToggleConfirmPasswordVisibility = viewModel::toggleRegConfirmPasswordVisibility,
                    onRegisterClick = viewModel::register,
                    onLoginClick = { viewModel.navigateTo(AuthScreen.LOGIN) }
                )
            }

            AuthScreen.WELCOME -> {
                MainFeedContainer(
                    userProfile = uiState.currentUserProfile,
                    onLogoutClick = viewModel::logout
                )
            }
        }
    }
}
