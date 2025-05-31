package com.foodtrack.app.data.session

import android.content.Context
import android.content.SharedPreferences
import com.foodtrack.app.data.model.User
import com.foodtrack.app.data.model.Household
import com.google.gson.Gson

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "foodtrack_session"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_CURRENT_HOUSEHOLD = "current_household"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SELECTED_HOUSEHOLD_ID = "selected_household_id"
    }
    
    /**
     * Speichert die Anmeldedaten
     */
    fun saveLoginSession(token: String, user: User) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_DATA, gson.toJson(user))
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_SELECTED_HOUSEHOLD_ID, user.householdId)
            apply()
        }
    }
    
    /**
     * Speichert den aktuell ausgewählten Haushalt
     */
    fun setCurrentHousehold(household: Household) {
        prefs.edit().apply {
            putString(KEY_CURRENT_HOUSEHOLD, gson.toJson(household))
            putInt(KEY_SELECTED_HOUSEHOLD_ID, household.id)
            apply()
        }
    }
    
    /**
     * Gibt den Auth-Token zurück
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    /**
     * Gibt die aktuellen Benutzerdaten zurück
     */
    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Gibt den aktuell ausgewählten Haushalt zurück
     */
    fun getCurrentHousehold(): Household? {
        val householdJson = prefs.getString(KEY_CURRENT_HOUSEHOLD, null)
        return if (householdJson != null) {
            try {
                gson.fromJson(householdJson, Household::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Gibt die ID des aktuell ausgewählten Haushalts zurück
     */
    fun getSelectedHouseholdId(): Int {
        return prefs.getInt(KEY_SELECTED_HOUSEHOLD_ID, -1)
    }
    
    /**
     * Prüft ob der Benutzer angemeldet ist
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAuthToken() != null
    }
    
    /**
     * Prüft ob der aktuelle Benutzer Admin des aktuellen Haushalts ist
     */
    fun isCurrentUserHouseholdAdmin(): Boolean {
        val user = getCurrentUser()
        val household = getCurrentHousehold()
        return user != null && household != null && user.id == household.adminUserId
    }
    
    /**
     * Meldet den Benutzer ab und löscht alle Session-Daten
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Aktualisiert die Benutzerdaten
     */
    fun updateUserData(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_DATA, gson.toJson(user))
            apply()
        }
    }
}
