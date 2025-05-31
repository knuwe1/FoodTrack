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
    @SerializedName("mindestmenge") val mindestmenge: Int?,

    // Neue Multi-Tenant Felder
    @SerializedName("household_id") val householdId: Int,
    @SerializedName("storage_location_id") val storageLocationId: Int? = null,
    @SerializedName("package_id") val packageId: Int? = null,
    @SerializedName("package_count") val packageCount: Int = 1, // Anzahl der Gebinde
    @SerializedName("created_by") val createdBy: Int,
    @SerializedName("updated_by") val updatedBy: Int? = null
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

    /**
     * Berechnet die Gesamtmenge basierend auf Gebinden
     * Wenn packageId gesetzt ist, wird packageCount * fillAmount verwendet
     */
    fun getTotalQuantityFromPackages(packageInfo: Package?): Double? {
        return if (packageInfo != null && packageCount > 0) {
            packageCount * packageInfo.fillAmount
        } else {
            menge?.toDouble()
        }
    }

    /**
     * Formatiert die Mengenangabe mit Gebinde-Information
     */
    fun getFormattedQuantityWithPackages(packageInfo: Package?): String {
        return if (packageInfo != null && packageCount > 0) {
            val totalAmount = getTotalQuantityFromPackages(packageInfo)
            "$packageCount x ${packageInfo.name} (${totalAmount?.let { "%.1f".format(it) }} ${packageInfo.fillUnit})"
        } else {
            "${menge ?: 0} ${einheit ?: ""}"
        }
    }
}
