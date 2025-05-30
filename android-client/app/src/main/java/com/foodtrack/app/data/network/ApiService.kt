package com.foodtrack.app.data.network

import com.foodtrack.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/users/")
    suspend fun registerUser(@Body userCreate: UserCreate): Response<User>

    @POST("api/v1/users/login-json")
    suspend fun loginUser(@Body loginRequest: UserLogin): Response<Token>

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): Response<User> // Requires Auth token

    @GET("api/v1/lebensmittel/")
    suspend fun getAllLebensmittel(): Response<List<Lebensmittel>> // Requires Auth token

    @POST("api/v1/lebensmittel/")
    suspend fun createLebensmittel(@Body lebensmittelCreate: LebensmittelCreate): Response<Lebensmittel> // Requires Auth token

    @GET("api/v1/lebensmittel/{id}")
    suspend fun getLebensmittelById(@Path("id") id: Int): Response<Lebensmittel> // Requires Auth token

    @PATCH("api/v1/lebensmittel/{id}")
    suspend fun updateLebensmittel(@Path("id") id: Int, @Body lebensmittelCreate: LebensmittelCreate): Response<Lebensmittel> // Requires Auth token

    @DELETE("api/v1/lebensmittel/{id}")
    suspend fun deleteLebensmittel(@Path("id") id: Int): Response<Unit> // HTTP 204 No Content

    // Transaction endpoints
    @POST("api/v1/transactions/")
    suspend fun createTransaction(@Body transactionCreate: TransactionCreate): Response<Transaction>

    @GET("api/v1/transactions/")
    suspend fun getAllTransactions(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("lebensmittel_id") lebensmittelId: Int? = null,
        @Query("transaction_type") transactionType: String? = null
    ): Response<List<Transaction>>

    @GET("api/v1/transactions/{id}")
    suspend fun getTransactionById(@Path("id") id: Int): Response<Transaction>

    // Convenience endpoints
    @POST("api/v1/transactions/purchase/{lebensmittel_id}")
    suspend fun recordPurchase(
        @Path("lebensmittel_id") lebensmittelId: Int,
        @Query("quantity") quantity: Int,
        @Query("reason") reason: String? = null,
        @Query("mhd") mhd: String? = null
    ): Response<Transaction>

    @POST("api/v1/transactions/consume/{lebensmittel_id}")
    suspend fun recordConsumption(
        @Path("lebensmittel_id") lebensmittelId: Int,
        @Query("quantity") quantity: Int,
        @Query("reason") reason: String? = null
    ): Response<Transaction>

    // Statistics endpoints
    @GET("api/v1/transactions/statistics/overview")
    suspend fun getStatisticsOverview(): Response<StatisticsOverview>

    @GET("api/v1/transactions/statistics/categories")
    suspend fun getCategoryStatistics(): Response<List<CategoryStatistics>>

    @GET("api/v1/transactions/statistics/monthly")
    suspend fun getMonthlyStatistics(@Query("months") months: Int = 12): Response<List<MonthlyStatistics>>

    @GET("api/v1/transactions/statistics/items")
    suspend fun getLebensmittelStatistics(@Query("limit") limit: Int = 10): Response<List<LebensmittelStatistics>>
}
