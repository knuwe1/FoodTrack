package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class LebensmittelCreate(
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: Int?, // Backend erwartet "quantity" als Alias
    @SerializedName("einheit") val einheit: String?,
    @SerializedName("kategorie") val kategorie: String?,
    @SerializedName("ablaufdatum") val ablaufdatum: String?, // "YYYY-MM-DD"
    @SerializedName("ean_code") val eanCode: String?,
    @SerializedName("mindestmenge") val mindestmenge: Int?,

    // Multi-Tenant Felder
    @SerializedName("storage_location_id") val storageLocationId: Int? = null,
    @SerializedName("package_id") val packageId: Int? = null,
    @SerializedName("package_count") val packageCount: Int = 1
)
