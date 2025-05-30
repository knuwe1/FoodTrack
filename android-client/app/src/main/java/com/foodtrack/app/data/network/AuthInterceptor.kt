package com.foodtrack.app.data.network

import com.foodtrack.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessionManager.fetchAuthToken()

        // Check if the request is for login or registration
        val path = request.url.encodedPath
        val method = request.method

        val isLoginRequest = method == "POST" && (path.endsWith("/api/v1/users/login") || path.endsWith("/api/v1/users/login-json"))
        val isRegisterRequest = method == "POST" && path.endsWith("/api/v1/users/") && !path.contains("login") // Ensure it's not the login path

        if (token != null && !isLoginRequest && !isRegisterRequest) {
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(authenticatedRequest)
        }

        return chain.proceed(request)
    }
}
