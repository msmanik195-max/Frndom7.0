package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthScreen {
    SPLASH,
    LOGIN,
    REGISTER,
    WELCOME
}

data class AuthUiState(
    val currentScreen: AuthScreen = AuthScreen.SPLASH,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUserProfile: UserProfile? = null,

    // Login Form State
    val loginIdentifier: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,

    // Register Form State
    val regFirstName: String = "",
    val regLastName: String = "",
    val regIdentifierType: String = "email", // "email" or "phone"
    val regEmail: String = "",
    val regPhone: String = "",
    val regGender: String = "Male", // "Male", "Female", "Custom"
    val regBirthDay: Int = 1,
    val regBirthMonth: Int = 1,
    val regBirthYear: Int = 2000,
    val regPassword: String = "",
    val regConfirmPassword: String = "",
    val isRegPasswordVisible: Boolean = false,
    val isRegConfirmPasswordVisible: Boolean = false
)

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: AuthRepository = AuthRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Initialize and check persistent session
        checkExistingAuth()
    }

    private fun checkExistingAuth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulated splash delay for facebook-style branding feel
            delay(2200L)

            val currentFirebaseUser = repository.currentUser
            if (currentFirebaseUser != null) {
                val profile = repository.fetchCurrentUserProfile()
                val userRepo = com.example.data.repository.UserRepository(getApplication())
                val resolvedProfile = if (profile != null) {
                    userRepo.enrichProfileWithVerification(profile)
                } else {
                    val fallback = userRepo.getLocalUserProfile(currentFirebaseUser.uid) ?: UserProfile(
                        uid = currentFirebaseUser.uid,
                        email = currentFirebaseUser.email ?: "",
                        fullName = currentFirebaseUser.displayName ?: "User"
                    )
                    userRepo.enrichProfileWithVerification(fallback)
                }
                userRepo.saveLocalUserProfile(resolvedProfile)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = resolvedProfile,
                        currentScreen = AuthScreen.WELCOME
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentScreen = AuthScreen.LOGIN
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AuthScreen) {
        _uiState.update {
            it.copy(
                currentScreen = screen,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // --- Login Form Updaters ---
    fun onLoginIdentifierChange(value: String) {
        _uiState.update { it.copy(loginIdentifier = value, errorMessage = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.update { it.copy(loginPassword = value, errorMessage = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.update { it.copy(isLoginPasswordVisible = !it.isLoginPasswordVisible) }
    }

    // --- Register Form Updaters ---
    fun onRegFirstNameChange(value: String) {
        _uiState.update { it.copy(regFirstName = value, errorMessage = null) }
    }

    fun onRegLastNameChange(value: String) {
        _uiState.update { it.copy(regLastName = value, errorMessage = null) }
    }

    fun onRegIdentifierTypeChange(type: String) {
        _uiState.update { it.copy(regIdentifierType = type, errorMessage = null) }
    }

    fun onRegEmailChange(value: String) {
        _uiState.update { it.copy(regEmail = value, errorMessage = null) }
    }

    fun onRegPhoneChange(value: String) {
        _uiState.update { it.copy(regPhone = value, errorMessage = null) }
    }

    fun onRegGenderChange(gender: String) {
        _uiState.update { it.copy(regGender = gender) }
    }

    fun onRegBirthDateChange(day: Int, month: Int, year: Int) {
        _uiState.update {
            it.copy(
                regBirthDay = day,
                regBirthMonth = month,
                regBirthYear = year
            )
        }
    }

    fun onRegPasswordChange(value: String) {
        _uiState.update { it.copy(regPassword = value, errorMessage = null) }
    }

    fun onRegConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(regConfirmPassword = value, errorMessage = null) }
    }

    fun toggleRegPasswordVisibility() {
        _uiState.update { it.copy(isRegPasswordVisible = !it.isRegPasswordVisible) }
    }

    fun toggleRegConfirmPasswordVisibility() {
        _uiState.update { it.copy(isRegConfirmPasswordVisible = !it.isRegConfirmPasswordVisible) }
    }

    // --- Actions ---

    fun login() {
        val state = _uiState.value
        val identifier = state.loginIdentifier.trim()
        val password = state.loginPassword

        if (identifier.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email or phone number") }
            return
        }

        if (password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.loginUser(identifier, password)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = profile,
                        currentScreen = AuthScreen.WELCOME,
                        loginPassword = ""
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed. Please verify your credentials."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        val firstName = state.regFirstName.trim()
        val lastName = state.regLastName.trim()
        val identifierType = state.regIdentifierType
        val email = state.regEmail.trim()
        val phone = state.regPhone.trim()
        val gender = state.regGender
        val day = state.regBirthDay
        val month = state.regBirthMonth
        val year = state.regBirthYear
        val password = state.regPassword
        val confirmPassword = state.regConfirmPassword

        // Validations
        if (firstName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your First Name") }
            return
        }

        if (lastName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your Last Name") }
            return
        }

        val identifierValue = if (identifierType == "email") email else phone

        if (identifierType == "email") {
            if (email.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please enter your email address") }
                return
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
                return
            }
        } else {
            if (phone.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please enter your phone number") }
                return
            }
            if (phone.length < 7) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid phone number") }
                return
            }
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.registerUser(
                firstName = firstName,
                lastName = lastName,
                identifierType = identifierType,
                identifierValue = identifierValue,
                gender = gender,
                birthDay = day,
                birthMonth = month,
                birthYear = year,
                password = password
            )

            if (result.isSuccess) {
                val profile = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserProfile = profile,
                        currentScreen = AuthScreen.WELCOME,
                        regPassword = "",
                        regConfirmPassword = ""
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed. Please try again."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.update {
            it.copy(
                currentUserProfile = null,
                currentScreen = AuthScreen.LOGIN,
                errorMessage = null,
                successMessage = null
            )
        }
    }
}
