package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class Lebensmittel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("menge") val menge: Double?,
    @SerializedName("einheit") val einheit: String?,
    @SerializedName("kategorie") val kategorie: String?,
    @SerializedName("kaufdatum") val kaufdatum: String?, // Format "YYYY-MM-DD"
    @SerializedName("mhd") val mhd: String?, // Format "YYYY-MM-DD"
    @SerializedName("lagerort") val lagerort: String?,
    @SerializedName("user_id") val user_id: Int?
)
