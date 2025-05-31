package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class Package(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String,                    // z.B. "6er Pack", "500g Dose", "1L Flasche"
    @SerializedName("description") val description: String? = null,
    @SerializedName("package_type") val packageType: PackageType = PackageType.PIECE,
    @SerializedName("fill_amount") val fillAmount: Double,       // Füllmenge pro Gebinde
    @SerializedName("fill_unit") val fillUnit: String,          // Einheit der Füllmenge (g, ml, Stück)
    @SerializedName("package_material") val packageMaterial: PackageMaterial? = null,
    @SerializedName("is_reusable") val isReusable: Int = 0,
    @SerializedName("is_active") val isActive: Int = 1,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null
)

enum class PackageType {
    @SerializedName("piece") PIECE,           // Einzelstück
    @SerializedName("pack") PACK,             // Packung/Multipack
    @SerializedName("bottle") BOTTLE,         // Flasche
    @SerializedName("can") CAN,               // Dose
    @SerializedName("jar") JAR,               // Glas
    @SerializedName("bag") BAG,               // Beutel/Tüte
    @SerializedName("box") BOX,               // Karton/Schachtel
    @SerializedName("tube") TUBE,             // Tube
    @SerializedName("container") CONTAINER,   // Behälter
    @SerializedName("bulk") BULK              // Lose/Bulk
}

enum class PackageMaterial {
    @SerializedName("plastic") PLASTIC,       // Kunststoff
    @SerializedName("glass") GLASS,           // Glas
    @SerializedName("metal") METAL,           // Metall
    @SerializedName("paper") PAPER,           // Papier/Karton
    @SerializedName("composite") COMPOSITE,   // Verbundmaterial
    @SerializedName("other") OTHER            // Sonstiges
}
