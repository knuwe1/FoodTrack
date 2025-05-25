package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("is_active") val is_active: Boolean
    // Assuming these are the core fields.
    // If backend returns more, like 'items', they would be List<Lebensmittel> or similar.
)
