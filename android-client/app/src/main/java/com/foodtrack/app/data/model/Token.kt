package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class Token(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("token_type") val token_type: String
)
