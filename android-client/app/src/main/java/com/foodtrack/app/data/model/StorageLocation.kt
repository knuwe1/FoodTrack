package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class StorageLocation(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("household_id") val householdId: Int,
    @SerializedName("location_type") val locationType: LocationType = LocationType.PANTRY,
    @SerializedName("temperature_zone") val temperatureZone: TemperatureZone = TemperatureZone.ROOM_TEMPERATURE,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null
)

enum class LocationType {
    @SerializedName("pantry") PANTRY,           // Speisekammer
    @SerializedName("refrigerator") REFRIGERATOR, // Kühlschrank
    @SerializedName("freezer") FREEZER,         // Gefrierschrank
    @SerializedName("cellar") CELLAR,           // Keller
    @SerializedName("garage") GARAGE,           // Garage
    @SerializedName("other") OTHER              // Sonstiges
}

enum class TemperatureZone {
    @SerializedName("frozen") FROZEN,                    // Tiefgefroren (-18°C)
    @SerializedName("refrigerated") REFRIGERATED,        // Gekühlt (2-8°C)
    @SerializedName("cool") COOL,                        // Kühl (8-15°C)
    @SerializedName("room_temperature") ROOM_TEMPERATURE, // Raumtemperatur (15-25°C)
    @SerializedName("warm") WARM                         // Warm (>25°C)
}
