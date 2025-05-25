package com.foodtrack.app.ui.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.model.LebensmittelCreate
import com.foodtrack.app.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AddEditLebensmittelViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application.applicationContext)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _lebensmittelDetails = MutableLiveData<Lebensmittel?>()
    val lebensmittelDetails: LiveData<Lebensmittel?> = _lebensmittelDetails

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    fun fetchLebensmittelDetails(id: Int) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = apiService.getLebensmittelById(id)
                if (response.isSuccessful) {
                    _lebensmittelDetails.value = response.body()
                } else {
                    _errorMessage.value = "Error fetching details: ${response.code()} - ${response.errorBody()?.string()}"
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message()}"
            } catch (e: IOException) {
                _errorMessage.value = "Network connection issue: ${e.message}"
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveLebensmittel(
        id: Int?,
        name: String,
        mengeStr: String,
        einheit: String,
        kategorie: String,
        kaufdatum: String,
        mhd: String,
        lagerort: String
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Name cannot be empty."
            _saveResult.value = false // Indicate failure
            return
        }

        val menge: Double? = mengeStr.toDoubleOrNull()
        if (mengeStr.isNotEmpty() && menge == null) {
            _errorMessage.value = "Menge must be a valid number or empty."
            _saveResult.value = false
            return
        }

        // Basic date validation (YYYY-MM-DD)
        val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (kaufdatum.isNotEmpty() && !datePattern.matches(kaufdatum)) {
            _errorMessage.value = "Kaufdatum must be in YYYY-MM-DD format or empty."
            _saveResult.value = false
            return
        }
        if (mhd.isNotEmpty() && !datePattern.matches(mhd)) {
            _errorMessage.value = "MHD must be in YYYY-MM-DD format or empty."
            _saveResult.value = false
            return
        }


        val lebensmittelCreate = LebensmittelCreate(
            name = name,
            menge = menge,
            einheit = einheit.ifBlank { null },
            kategorie = kategorie.ifBlank { null },
            kaufdatum = kaufdatum.ifBlank { null },
            mhd = mhd.ifBlank { null },
            lagerort = lagerort.ifBlank { null }
        )

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = if (id == null) {
                    apiService.createLebensmittel(lebensmittelCreate)
                } else {
                    apiService.updateLebensmittel(id, lebensmittelCreate)
                }

                if (response.isSuccessful) {
                    _saveResult.value = true
                } else {
                    _errorMessage.value = "Save failed: ${response.code()} - ${response.errorBody()?.string()}"
                    _saveResult.value = false
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message()}"
                _saveResult.value = false
            } catch (e: IOException) {
                _errorMessage.value = "Network connection issue: ${e.message}"
                _saveResult.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _saveResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = false // Reset to a non-triggering state if needed
    }

    fun deleteLebensmittel(id: Int) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = apiService.deleteLebensmittel(id)
                if (response.isSuccessful) {
                    _deleteResult.value = true
                } else {
                    _errorMessage.value = "Delete failed: ${response.code()} - ${response.errorBody()?.string()}"
                    _deleteResult.value = false
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message()}"
                _deleteResult.value = false
            } catch (e: IOException) {
                _errorMessage.value = "Network connection issue: ${e.message}"
                _deleteResult.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _deleteResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = false // Reset
    }
}
