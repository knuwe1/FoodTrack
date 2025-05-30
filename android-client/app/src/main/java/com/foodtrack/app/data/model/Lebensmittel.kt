package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class Lebensmittel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val menge: Int?, // Backend sendet "quantity"
    @SerializedName("einheit") val einheit: String?,
    @SerializedName("kategorie") val kategorie: String?,
    @SerializedName("ablaufdatum") val ablaufdatum: String?, // Format "YYYY-MM-DD"
    @SerializedName("ean_code") val eanCode: String?,
    @SerializedName("mindestmenge") val mindestmenge: Int?
) {
    /**
     * Prüft ob der Bestand unter der Mindestmenge liegt
     */
    fun isBelowMinimum(): Boolean {
        return mindestmenge != null && mindestmenge > 0 && (menge ?: 0) < mindestmenge
    }

    /**
     * Berechnet wie viel fehlt bis zur Mindestmenge
     */
    fun getMinimumShortage(): Int {
        return if (isBelowMinimum()) {
            mindestmenge!! - (menge ?: 0)
        } else {
            0
        }
    }
}
