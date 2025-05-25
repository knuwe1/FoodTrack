package com.foodtrack.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.local.SessionManager
import com.foodtrack.app.data.model.UserCreate
import com.foodtrack.app.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application.applicationContext)
    private val sessionManager = SessionManager(application.applicationContext)

    private val _authenticationState = MutableLiveData<AuthenticationState>(AuthenticationState.Idle)
    val authenticationState: LiveData<AuthenticationState> = _authenticationState

    // For simplicity, using a single error message LiveData. Could be part of AuthenticationState.
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading


    fun login(email: String, password: String) {
        _isLoading.value = true
        _authenticationState.value = AuthenticationState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.loginUser(username = email, password = password)
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.access_token
                    sessionManager.saveAuthToken(token)
                    // Optionally fetch user details here if needed immediately
                    _authenticationState.value = AuthenticationState.Authenticated(token = token)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Login failed: Unknown error"
                    _errorMessage.value = "Login failed: ${response.code()} - $errorBody"
                    _authenticationState.value = AuthenticationState.Error("Login failed: ${response.code()} - $errorBody")
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Login failed: Network error - ${e.message()}"
                _authenticationState.value = AuthenticationState.Error("Login failed: Network error - ${e.message()}")
            } catch (e: IOException) {
                _errorMessage.value = "Login failed: Network connection issue - ${e.message}"
                _authenticationState.value = AuthenticationState.Error("Login failed: Network connection issue - ${e.message}")
            } catch (e: Exception) {
                _errorMessage.value = "Login failed: An unexpected error occurred - ${e.message}"
                _authenticationState.value = AuthenticationState.Error("Login failed: An unexpected error occurred - ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, password: String) {
        _isLoading.value = true
        _authenticationState.value = AuthenticationState.Loading
        viewModelScope.launch {
            try {
                val userCreate = UserCreate(email = email, password = password)
                val response = apiService.registerUser(userCreate)
                if (response.isSuccessful && response.body() != null) {
                    // User registered, typically doesn't auto-login or return token
                    // Navigate to login or show success message.
                    _authenticationState.value = AuthenticationState.Authenticated(user = response.body())
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Registration failed: Unknown error"
                    _errorMessage.value = "Registration failed: ${response.code()} - $errorBody"
                    _authenticationState.value = AuthenticationState.Error("Registration failed: ${response.code()} - $errorBody")
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Registration failed: Network error - ${e.message()}"
                _authenticationState.value = AuthenticationState.Error("Registration failed: Network error - ${e.message()}")
            } catch (e: IOException) {
                _errorMessage.value = "Registration failed: Network connection issue - ${e.message}"
                _authenticationState.value = AuthenticationState.Error("Registration failed: Network connection issue - ${e.message}")
            } catch (e: Exception) {
                _errorMessage.value = "Registration failed: An unexpected error occurred - ${e.message}"
                _authenticationState.value = AuthenticationState.Error("Registration failed: An unexpected error occurred - ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetErrorMesssage() {
        _errorMessage.value = null
    }
}
