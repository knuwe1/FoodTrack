package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class LebensmittelCreate(
    @SerializedName("name") val name: String,
    @SerializedName("menge") val menge: Double?,
    @SerializedName("einheit") val einheit: String?,
    @SerializedName("kategorie") val kategorie: String?,
    @SerializedName("kaufdatum") val kaufdatum: String?, // "YYYY-MM-DD"
    @SerializedName("mhd") val mhd: String?, // "YYYY-MM-DD"
    @SerializedName("lagerort") val lagerort: String?
)
