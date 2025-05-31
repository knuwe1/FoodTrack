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

    // Household Management
    @POST("api/v1/households/")
    suspend fun createHousehold(@Body householdCreate: HouseholdCreate): Response<Household>

    @GET("api/v1/households/")
    suspend fun getMyHouseholds(): Response<List<Household>>

    @GET("api/v1/households/{id}")
    suspend fun getHouseholdById(@Path("id") id: Int): Response<Household>

    @PATCH("api/v1/households/{id}")
    suspend fun updateHousehold(@Path("id") id: Int, @Body householdUpdate: HouseholdUpdate): Response<Household>

    @DELETE("api/v1/households/{id}")
    suspend fun deleteHousehold(@Path("id") id: Int): Response<Unit>

    @POST("api/v1/households/{id}/invite")
    suspend fun generateInviteCode(@Path("id") id: Int): Response<InviteCodeResponse>

    @POST("api/v1/households/join")
    suspend fun joinHousehold(@Body joinRequest: JoinHouseholdRequest): Response<Household>

    @GET("api/v1/households/{id}/members")
    suspend fun getHouseholdMembers(@Path("id") id: Int): Response<List<User>>

    @DELETE("api/v1/households/{household_id}/members/{user_id}")
    suspend fun removeHouseholdMember(@Path("household_id") householdId: Int, @Path("user_id") userId: Int): Response<Unit>

    // Storage Location Management
    @POST("api/v1/storage-locations/")
    suspend fun createStorageLocation(@Body storageLocationCreate: StorageLocationCreate): Response<StorageLocation>

    @GET("api/v1/storage-locations/")
    suspend fun getStorageLocations(): Response<List<StorageLocation>>

    @GET("api/v1/storage-locations/{id}")
    suspend fun getStorageLocationById(@Path("id") id: Int): Response<StorageLocation>

    @PATCH("api/v1/storage-locations/{id}")
    suspend fun updateStorageLocation(@Path("id") id: Int, @Body storageLocationUpdate: StorageLocationUpdate): Response<StorageLocation>

    @DELETE("api/v1/storage-locations/{id}")
    suspend fun deleteStorageLocation(@Path("id") id: Int): Response<Unit>

    // Package Management
    @POST("api/v1/packages/")
    suspend fun createPackage(@Body packageCreate: PackageCreate): Response<Package>

    @GET("api/v1/packages/")
    suspend fun getPackages(): Response<List<Package>>

    @GET("api/v1/packages/{id}")
    suspend fun getPackageById(@Path("id") id: Int): Response<Package>

    @PATCH("api/v1/packages/{id}")
    suspend fun updatePackage(@Path("id") id: Int, @Body packageUpdate: PackageUpdate): Response<Package>

    @DELETE("api/v1/packages/{id}")
    suspend fun deletePackage(@Path("id") id: Int): Response<Unit>
}
