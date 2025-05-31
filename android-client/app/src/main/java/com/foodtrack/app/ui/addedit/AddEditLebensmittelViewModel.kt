package com.foodtrack.app.ui.addedit

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.model.LebensmittelCreate
import com.foodtrack.app.data.model.StorageLocation
import com.foodtrack.app.data.model.Package as FoodPackage
import com.foodtrack.app.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AddEditLebensmittelViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AddEditViewModel"
    }

    private val apiService = RetrofitClient.getApiService(application.applicationContext)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _saveResult = MutableLiveData<Boolean?>()
    val saveResult: LiveData<Boolean?> = _saveResult

    private val _lebensmittelDetails = MutableLiveData<Lebensmittel?>()
    val lebensmittelDetails: LiveData<Lebensmittel?> = _lebensmittelDetails

    private val _deleteResult = MutableLiveData<Boolean?>()
    val deleteResult: LiveData<Boolean?> = _deleteResult

    // Multi-Tenant data
    private val _storageLocations = MutableLiveData<List<StorageLocation>>()
    val storageLocations: LiveData<List<StorageLocation>> = _storageLocations

    private val _packages = MutableLiveData<List<FoodPackage>>()
    val packages: LiveData<List<FoodPackage>> = _packages

    init {
        // Load multi-tenant data when ViewModel is created
        loadMultiTenantData()
    }

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
        ablaufdatum: String,
        eanCode: String,
        mindestmengeStr: String,
        storageLocationId: Int? = null,
        packageId: Int? = null,
        packageCount: Int = 1
    ) {
        Log.d(TAG, "Starting saveLebensmittel")
        Log.d(TAG, "ID=$id, name=$name, menge=$mengeStr, kategorie=$kategorie")
        if (name.isBlank()) {
            _errorMessage.value = "Name cannot be empty."
            _saveResult.value = false // Indicate failure
            return
        }

        val menge: Int? = mengeStr.toIntOrNull()
        if (mengeStr.isNotEmpty() && menge == null) {
            _errorMessage.value = "Menge must be a valid number or empty."
            _saveResult.value = false
            return
        }

        val mindestmenge: Int? = mindestmengeStr.toIntOrNull()
        if (mindestmengeStr.isNotEmpty() && mindestmenge == null) {
            _errorMessage.value = "Mindestmenge must be a valid number or empty."
            _saveResult.value = false
            return
        }

        // Basic date validation (YYYY-MM-DD)
        val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        if (ablaufdatum.isNotEmpty() && !datePattern.matches(ablaufdatum)) {
            _errorMessage.value = "Ablaufdatum must be in YYYY-MM-DD format or empty."
            _saveResult.value = false
            return
        }


        val lebensmittelCreate = LebensmittelCreate(
            name = name,
            quantity = menge,
            einheit = einheit.ifBlank { null },
            kategorie = kategorie.ifBlank { null },
            ablaufdatum = ablaufdatum.ifBlank { null },
            eanCode = eanCode.ifBlank { null },
            mindestmenge = mindestmenge,
            storageLocationId = storageLocationId,
            packageId = packageId,
            packageCount = packageCount
        )

        Log.d(TAG, "LebensmittelCreate: name=$name, quantity=$menge, einheit=${einheit.ifBlank { null }}, kategorie=${kategorie.ifBlank { null }}, ablaufdatum=${ablaufdatum.ifBlank { null }}")

        Log.d(TAG, "Setting loading state and starting coroutine")
        _isLoading.value = true
        _errorMessage.value = null
        _saveResult.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Making API call...")
                val response = if (id == null) {
                    Log.d(TAG, "Creating new lebensmittel")
                    apiService.createLebensmittel(lebensmittelCreate)
                } else {
                    Log.d(TAG, "Updating lebensmittel with ID $id")
                    apiService.updateLebensmittel(id, lebensmittelCreate)
                }

                Log.d(TAG, "API call completed. Success: ${response.isSuccessful}, Code: ${response.code()}")
                if (response.isSuccessful) {
                    Log.d(TAG, "Save successful")
                    _saveResult.value = true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Save failed: ${response.code()} - $errorBody")
                    _errorMessage.value = "Save failed: ${response.code()} - $errorBody"
                    _saveResult.value = false
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException: ${e.message()}")
                _errorMessage.value = "Network error: ${e.message()}"
                _saveResult.value = false
            } catch (e: IOException) {
                Log.e(TAG, "IOException: ${e.message}")
                _errorMessage.value = "Network connection issue: ${e.message}"
                _saveResult.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "Unexpected error: ${e.message}"
                _saveResult.value = false
            } finally {
                Log.d(TAG, "Setting loading to false")
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = null // Reset to a non-triggering state if needed
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
        _deleteResult.value = null // Reset
    }

    // Multi-Tenant Functions
    private fun loadMultiTenantData() {
        viewModelScope.launch {
            try {
                // Load storage locations
                val locationsResponse = apiService.getStorageLocations()
                if (locationsResponse.isSuccessful) {
                    _storageLocations.value = locationsResponse.body() ?: emptyList()
                }

                // Load packages
                val packagesResponse = apiService.getPackages()
                if (packagesResponse.isSuccessful) {
                    _packages.value = packagesResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load multi-tenant data: ${e.message}")
                // Don't show error to user for this, just log it
            }
        }
    }

    fun getStorageLocationById(id: Int): StorageLocation? {
        return _storageLocations.value?.find { it.id == id }
    }

    fun getPackageById(id: Int): FoodPackage? {
        return _packages.value?.find { it.id == id }
    }
}
