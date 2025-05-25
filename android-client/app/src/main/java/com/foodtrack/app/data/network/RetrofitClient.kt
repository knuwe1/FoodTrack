package com.foodtrack.app.data.network

import android.content.Context
import com.foodtrack.app.data.local.SessionManager
import com.foodtrack.app.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Volatile to ensure atomic access to the variable
    @Volatile
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        // Use the application context to avoid memory leaks
        val appContext = context.applicationContext
        if (apiService == null) {
            synchronized(this) {
                if (apiService == null) {
                    val sessionManager = SessionManager(appContext)

                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY // Or Level.BASIC for less verbose logs
                    }

                    val authInterceptor = AuthInterceptor(sessionManager)

                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(authInterceptor)
                        .addInterceptor(loggingInterceptor) // Add logging interceptor
                        .build()

                    apiService = Retrofit.Builder()
                        .baseUrl(Constants.BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(ApiService::class.java)
                }
            }
        }
        return apiService!!
    }
}
