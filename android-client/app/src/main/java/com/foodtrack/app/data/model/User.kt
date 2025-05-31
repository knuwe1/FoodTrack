package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("household_id") val householdId: Int,
    @SerializedName("role") val role: UserRole = UserRole.MEMBER,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("last_login_at") val lastLoginAt: String? = null
)

enum class UserRole {
    @SerializedName("admin") ADMIN,    // Haushalts-Administrator
    @SerializedName("member") MEMBER   // Haushalts-Mitglied
}
