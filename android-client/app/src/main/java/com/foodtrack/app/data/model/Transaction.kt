package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionType {
    @SerializedName("PURCHASE")
    PURCHASE,
    
    @SerializedName("CONSUMPTION")
    CONSUMPTION,
    
    @SerializedName("ADJUSTMENT")
    ADJUSTMENT,
    
    @SerializedName("EXPIRED")
    EXPIRED
}

data class Transaction(
    val id: Int,
    @SerializedName("lebensmittel_id") val lebensmittelId: Int,
    @SerializedName("transaction_type") val transactionType: TransactionType,
    @SerializedName("quantity_change") val quantityChange: Int,
    @SerializedName("quantity_before") val quantityBefore: Int?,
    @SerializedName("quantity_after") val quantityAfter: Int?,
    val reason: String?,
    @SerializedName("created_at") val createdAt: String
)

data class TransactionCreate(
    @SerializedName("lebensmittel_id") val lebensmittelId: Int,
    @SerializedName("transaction_type") val transactionType: TransactionType,
    @SerializedName("quantity_change") val quantityChange: Int,
    val reason: String?
)

// Statistics models
data class CategoryStatistics(
    val kategorie: String,
    @SerializedName("total_purchases") val totalPurchases: Int,
    @SerializedName("total_consumption") val totalConsumption: Int,
    @SerializedName("net_change") val netChange: Int,
    @SerializedName("transaction_count") val transactionCount: Int
)

data class MonthlyStatistics(
    val year: Int,
    val month: Int,
    @SerializedName("total_purchases") val totalPurchases: Int,
    @SerializedName("total_consumption") val totalConsumption: Int,
    @SerializedName("net_change") val netChange: Int,
    @SerializedName("transaction_count") val transactionCount: Int
)

data class LebensmittelStatistics(
    @SerializedName("lebensmittel_id") val lebensmittelId: Int,
    @SerializedName("lebensmittel_name") val lebensmittelName: String,
    val kategorie: String?,
    @SerializedName("total_purchases") val totalPurchases: Int,
    @SerializedName("total_consumption") val totalConsumption: Int,
    @SerializedName("net_change") val netChange: Int,
    @SerializedName("transaction_count") val transactionCount: Int,
    @SerializedName("last_transaction") val lastTransaction: String?
)

data class StatisticsOverview(
    @SerializedName("total_transactions") val totalTransactions: Int,
    @SerializedName("total_purchases") val totalPurchases: Int,
    @SerializedName("total_consumption") val totalConsumption: Int,
    @SerializedName("most_purchased_category") val mostPurchasedCategory: String?,
    @SerializedName("most_consumed_category") val mostConsumedCategory: String?,
    val categories: List<CategoryStatistics>,
    val monthly: List<MonthlyStatistics>,
    @SerializedName("top_items") val topItems: List<LebensmittelStatistics>
)
