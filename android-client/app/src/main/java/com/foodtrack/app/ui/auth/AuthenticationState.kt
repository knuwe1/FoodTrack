package com.foodtrack.app.ui.auth

import com.foodtrack.app.data.model.User

sealed class AuthenticationState {
    object Idle : AuthenticationState()
    object Loading : AuthenticationState()
    data class Authenticated(val user: User? = null, val token: String? = null) : AuthenticationState() // User might be null for register, token for login
    data class Error(val message: String) : AuthenticationState()
}
