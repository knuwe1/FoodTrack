package com.foodtrack.app.data.network

import com.foodtrack.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/users/")
    suspend fun registerUser(@Body userCreate: UserCreate): Response<User>

    @FormUrlEncoded
    @POST("api/v1/users/login")
    suspend fun loginUser(
        @Field("username") username: String, // This is the email
        @Field("password") password: String
    ): Response<Token>

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<User> // Requires Auth token

    @GET("api/v1/lebensmittel/")
    suspend fun getAllLebensmittel(): Response<List<Lebensmittel>> // Requires Auth token

    @POST("api/v1/lebensmittel/")
    suspend fun createLebensmittel(@Body lebensmittelCreate: LebensmittelCreate): Response<Lebensmittel> // Requires Auth token

    @GET("api/v1/lebensmittel/{id}")
    suspend fun getLebensmittelById(@Path("id") id: Int): Response<Lebensmittel> // Requires Auth token

    @PUT("api/v1/lebensmittel/{id}")
    suspend fun updateLebensmittel(@Path("id") id: Int, @Body lebensmittelCreate: LebensmittelCreate): Response<Lebensmittel> // Requires Auth token

    @DELETE("api/v1/lebensmittel/{id}")
    suspend fun deleteLebensmittel(@Path("id") id: Int): Response<MessageResponse> // Requires Auth token
}
