package com.foodtrack.app.data.model

data class Household(
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    val adminUserId: Int,
    val inviteCode: String? = null, // Für Einladungen
    val createdAt: String,
    val updatedAt: String? = null,
    val isActive: Boolean = true
)
