package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("status")
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String,
    @SerializedName("product")
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    @SerializedName("_id")
    val id: String?,
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("product_name_de")
    val productNameDe: String?,
    @SerializedName("product_name_en")
    val productNameEn: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("categories")
    val categories: String?,
    @SerializedName("categories_tags")
    val categoriesTags: List<String>?,
    @SerializedName("quantity")
    val quantity: String?,
    @SerializedName("serving_size")
    val servingSize: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("image_front_url")
    val imageFrontUrl: String?,
    @SerializedName("image_front_small_url")
    val imageFrontSmallUrl: String?,
    @SerializedName("nutriscore_grade")
    val nutriscoreGrade: String?,
    @SerializedName("nova_group")
    val novaGroup: Int?,
    @SerializedName("ecoscore_grade")
    val ecoscoreGrade: String?,
    @SerializedName("ingredients_text")
    val ingredientsText: String?,
    @SerializedName("ingredients_text_de")
    val ingredientsTextDe: String?,
    @SerializedName("allergens")
    val allergens: String?,
    @SerializedName("traces")
    val traces: String?,
    @SerializedName("stores")
    val stores: String?,
    @SerializedName("countries")
    val countries: String?,
    @SerializedName("manufacturing_places")
    val manufacturingPlaces: String?,
    @SerializedName("labels")
    val labels: String?,
    @SerializedName("packaging")
    val packaging: String?,
    @SerializedName("code")
    val code: String?
) {
    /**
     * Gibt den besten verfügbaren Produktnamen zurück
     */
    fun getBestProductName(): String? {
        return when {
            !productNameDe.isNullOrBlank() -> productNameDe
            !productName.isNullOrBlank() -> productName
            !productNameEn.isNullOrBlank() -> productNameEn
            else -> null
        }
    }

    /**
     * Gibt die beste verfügbare Kategorie zurück
     */
    fun getBestCategory(): String? {
        return when {
            !categories.isNullOrBlank() -> {
                // Nimm die erste Kategorie und bereinige sie
                categories.split(",").firstOrNull()?.trim()?.let { category ->
                    // Entferne "en:" Präfix falls vorhanden
                    if (category.startsWith("en:")) {
                        category.substring(3).replace("-", " ").replaceFirstChar { it.uppercase() }
                    } else {
                        category.replace("-", " ").replaceFirstChar { it.uppercase() }
                    }
                }
            }
            categoriesTags?.isNotEmpty() == true -> {
                // Fallback auf Tags
                categoriesTags.firstOrNull()?.let { tag ->
                    if (tag.startsWith("en:")) {
                        tag.substring(3).replace("-", " ").replaceFirstChar { it.uppercase() }
                    } else {
                        tag.replace("-", " ").replaceFirstChar { it.uppercase() }
                    }
                }
            }
            else -> null
        }
    }

    /**
     * Extrahiert eine sinnvolle Einheit aus der Quantity
     */
    fun getUnit(): String? {
        return quantity?.let { qty ->
            when {
                qty.contains("kg", ignoreCase = true) -> "kg"
                qty.contains("g", ignoreCase = true) && !qty.contains("kg", ignoreCase = true) -> "g"
                qty.contains("l", ignoreCase = true) && !qty.contains("ml", ignoreCase = true) -> "l"
                qty.contains("ml", ignoreCase = true) -> "ml"
                qty.contains("stück", ignoreCase = true) || qty.contains("piece", ignoreCase = true) -> "Stück"
                else -> "Stück" // Default
            }
        } ?: "Stück"
    }

    /**
     * Extrahiert die Menge/Gewicht aus der Quantity
     */
    fun getQuantityValue(): Int? {
        return quantity?.let { qty ->
            // Extrahiere Zahlen aus dem String
            val numberRegex = Regex("(\\d+(?:[.,]\\d+)?)")
            val match = numberRegex.find(qty)
            match?.value?.replace(",", ".")?.toDoubleOrNull()?.toInt()
        }
    }

    /**
     * Gibt den besten verfügbaren Markennamen zurück
     */
    fun getBestBrand(): String? {
        return brands?.split(",")?.firstOrNull()?.trim()
    }

    /**
     * Extrahiert Verpackungsart aus packaging
     */
    fun getPackagingType(): String? {
        return packaging?.let { pack ->
            when {
                pack.contains("glas", ignoreCase = true) || pack.contains("glass", ignoreCase = true) -> "Glas"
                pack.contains("dose", ignoreCase = true) || pack.contains("can", ignoreCase = true) -> "Dose"
                pack.contains("flasche", ignoreCase = true) || pack.contains("bottle", ignoreCase = true) -> "Flasche"
                pack.contains("karton", ignoreCase = true) || pack.contains("carton", ignoreCase = true) -> "Karton"
                pack.contains("beutel", ignoreCase = true) || pack.contains("bag", ignoreCase = true) -> "Beutel"
                else -> null
            }
        }
    }

    /**
     * Prüft ob das Produkt vollständige Informationen hat
     */
    fun isComplete(): Boolean {
        return !getBestProductName().isNullOrBlank()
    }
}
