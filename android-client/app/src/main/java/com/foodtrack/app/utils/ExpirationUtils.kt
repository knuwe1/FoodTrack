package com.foodtrack.app.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ExpirationUtils {
    
    enum class ExpirationStatus {
        EXPIRED,        // Abgelaufen
        EXPIRING_SOON,  // Läuft bald ab (1-3 Tage)
        FRESH,          // Frisch (>3 Tage)
        NO_DATE         // Kein Ablaufdatum
    }
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Bestimmt den Ablaufstatus eines Lebensmittels
     */
    fun getExpirationStatus(ablaufdatum: String?): ExpirationStatus {
        if (ablaufdatum.isNullOrBlank()) {
            return ExpirationStatus.NO_DATE
        }
        
        return try {
            val expiryDate = LocalDate.parse(ablaufdatum, dateFormatter)
            val today = LocalDate.now()
            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)
            
            when {
                daysUntilExpiry < 0 -> ExpirationStatus.EXPIRED
                daysUntilExpiry <= 3 -> ExpirationStatus.EXPIRING_SOON
                else -> ExpirationStatus.FRESH
            }
        } catch (e: Exception) {
            ExpirationStatus.NO_DATE
        }
    }
    
    /**
     * Gibt die Anzahl Tage bis zum Ablauf zurück
     */
    fun getDaysUntilExpiry(ablaufdatum: String?): Long? {
        if (ablaufdatum.isNullOrBlank()) return null
        
        return try {
            val expiryDate = LocalDate.parse(ablaufdatum, dateFormatter)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, expiryDate)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Formatiert das Ablaufdatum für die Anzeige
     */
    fun formatExpirationText(ablaufdatum: String?): String {
        val status = getExpirationStatus(ablaufdatum)
        val days = getDaysUntilExpiry(ablaufdatum)
        
        return when (status) {
            ExpirationStatus.EXPIRED -> {
                val daysOverdue = Math.abs(days ?: 0)
                "Abgelaufen vor $daysOverdue Tag${if (daysOverdue != 1L) "en" else ""}"
            }
            ExpirationStatus.EXPIRING_SOON -> {
                when (days) {
                    0L -> "Läuft heute ab"
                    1L -> "Läuft morgen ab"
                    else -> "Läuft in $days Tagen ab"
                }
            }
            ExpirationStatus.FRESH -> "Ablaufdatum: $ablaufdatum"
            ExpirationStatus.NO_DATE -> "Ablaufdatum: N/A"
        }
    }
    
    /**
     * Gibt die Farbe für den Status zurück (Android Color Resource)
     */
    fun getStatusColor(status: ExpirationStatus): Int {
        return when (status) {
            ExpirationStatus.EXPIRED -> android.graphics.Color.RED
            ExpirationStatus.EXPIRING_SOON -> android.graphics.Color.parseColor("#FF9800") // Orange
            ExpirationStatus.FRESH -> android.graphics.Color.parseColor("#4CAF50") // Green
            ExpirationStatus.NO_DATE -> android.graphics.Color.GRAY
        }
    }
    
    /**
     * Filtert Lebensmittel nach Ablaufstatus
     */
    fun filterByExpirationStatus(
        items: List<com.foodtrack.app.data.model.Lebensmittel>,
        status: ExpirationStatus
    ): List<com.foodtrack.app.data.model.Lebensmittel> {
        return items.filter { getExpirationStatus(it.ablaufdatum) == status }
    }
    
    /**
     * Gibt alle verfügbaren Filter-Optionen zurück
     */
    fun getFilterOptions(): List<String> {
        return listOf(
            "All Items",
            "Expired",
            "Expiring Soon",
            "Fresh",
            "No Date"
        )
    }
    
    /**
     * Konvertiert Filter-String zu ExpirationStatus
     */
    fun getStatusFromFilter(filter: String): ExpirationStatus? {
        return when (filter) {
            "Expired" -> ExpirationStatus.EXPIRED
            "Expiring Soon" -> ExpirationStatus.EXPIRING_SOON
            "Fresh" -> ExpirationStatus.FRESH
            "No Date" -> ExpirationStatus.NO_DATE
            else -> null // "All Items"
        }
    }
}
