package com.foodtrack.app.data.model

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("message") val message: String
)
