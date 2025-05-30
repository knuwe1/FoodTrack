package com.foodtrack.app.data.network

import com.foodtrack.app.data.model.OpenFoodFactsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsService {
    
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): Response<OpenFoodFactsResponse>
    
    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        
        /**
         * Erstellt eine Retrofit-Instanz für OpenFoodFacts API
         */
        fun create(): OpenFoodFactsService {
            return retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
                .create(OpenFoodFactsService::class.java)
        }
    }
}
