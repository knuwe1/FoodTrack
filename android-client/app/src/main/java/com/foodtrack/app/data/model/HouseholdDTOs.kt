package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

// Household DTOs
data class HouseholdCreate(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class HouseholdUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

data class InviteCodeResponse(
    @SerializedName("invite_code") val inviteCode: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class JoinHouseholdRequest(
    @SerializedName("invite_code") val inviteCode: String
)

// Storage Location DTOs
data class StorageLocationCreate(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("location_type") val locationType: LocationType = LocationType.PANTRY,
    @SerializedName("temperature_zone") val temperatureZone: TemperatureZone = TemperatureZone.ROOM_TEMPERATURE
)

data class StorageLocationUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("location_type") val locationType: LocationType? = null,
    @SerializedName("temperature_zone") val temperatureZone: TemperatureZone? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)

// Package DTOs
data class PackageCreate(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("package_type") val packageType: PackageType = PackageType.PIECE,
    @SerializedName("fill_amount") val fillAmount: Double,
    @SerializedName("fill_unit") val fillUnit: String,
    @SerializedName("package_material") val packageMaterial: PackageMaterial? = null,
    @SerializedName("is_reusable") val isReusable: Boolean = false
)

data class PackageUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("package_type") val packageType: PackageType? = null,
    @SerializedName("fill_amount") val fillAmount: Double? = null,
    @SerializedName("fill_unit") val fillUnit: String? = null,
    @SerializedName("package_material") val packageMaterial: PackageMaterial? = null,
    @SerializedName("is_reusable") val isReusable: Boolean? = null,
    @SerializedName("is_active") val isActive: Boolean? = null
)
