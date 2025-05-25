package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class UserCreate(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)
