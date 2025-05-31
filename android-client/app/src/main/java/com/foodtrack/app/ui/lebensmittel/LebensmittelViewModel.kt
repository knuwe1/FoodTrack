package com.foodtrack.app.ui.lebensmittel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.model.StorageLocation
import com.foodtrack.app.data.model.Package as FoodPackage
import com.foodtrack.app.data.model.Household
import com.foodtrack.app.data.network.RetrofitClient
import com.foodtrack.app.utils.ExpirationUtils
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LebensmittelViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application.applicationContext)

    // Original data from API
    private val _allLebensmittel = MutableLiveData<List<Lebensmittel>>()

    // Filtered data for display
    private val _lebensmittelList = MutableLiveData<List<Lebensmittel>>()
    val lebensmittelList: LiveData<List<Lebensmittel>> = _lebensmittelList

    // Available categories for filter
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    // Current filters
    private val _selectedCategory = MutableLiveData<String?>()
    val selectedCategory: LiveData<String?> = _selectedCategory

    private val _selectedExpirationFilter = MutableLiveData<String?>()
    val selectedExpirationFilter: LiveData<String?> = _selectedExpirationFilter

    // Expiration filter options
    private val _expirationFilterOptions = MutableLiveData<List<String>>()
    val expirationFilterOptions: LiveData<List<String>> = _expirationFilterOptions

    // Multi-Tenant data
    private val _households = MutableLiveData<List<Household>>()
    val households: LiveData<List<Household>> = _households

    private val _storageLocations = MutableLiveData<List<StorageLocation>>()
    val storageLocations: LiveData<List<StorageLocation>> = _storageLocations

    private val _packages = MutableLiveData<List<FoodPackage>>()
    val packages: LiveData<List<FoodPackage>> = _packages

    private val _currentHousehold = MutableLiveData<Household?>()
    val currentHousehold: LiveData<Household?> = _currentHousehold

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // Initialize expiration filter options
        _expirationFilterOptions.value = ExpirationUtils.getFilterOptions()
        // Fetch multi-tenant data first, then lebensmittel
        fetchMultiTenantData()
    }

    fun fetchLebensmittel() {
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            try {
                val response = apiService.getAllLebensmittel()
                if (response.isSuccessful) {
                    val allItems = response.body() ?: emptyList()
                    _allLebensmittel.value = allItems
                    updateCategories(allItems)
                    applyFilters() // Apply current filters
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to fetch items: Unknown error"
                    _errorMessage.value = "Error ${response.code()}: $errorBody"
                }
            } catch (e: HttpException) {
                _errorMessage.value = "Network error: ${e.message()}"
            } catch (e: IOException) {
                _errorMessage.value = "Network connection issue: ${e.message}"
            } catch (e: Exception) {
                _errorMessage.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateCategories(items: List<Lebensmittel>) {
        val uniqueCategories = items
            .mapNotNull { it.kategorie }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        // Add "All Categories" option at the beginning
        val categoriesWithAll = listOf("All Categories") + uniqueCategories
        _categories.value = categoriesWithAll
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun setSelectedExpirationFilter(filter: String?) {
        _selectedExpirationFilter.value = filter
        applyFilters()
    }

    private fun applyFilters() {
        val allItems = _allLebensmittel.value ?: emptyList()
        val selectedCat = _selectedCategory.value
        val selectedExp = _selectedExpirationFilter.value

        var filteredItems = allItems

        // Apply category filter
        if (!selectedCat.isNullOrEmpty() && selectedCat != "All Categories") {
            filteredItems = filteredItems.filter { it.kategorie == selectedCat }
        }

        // Apply expiration filter
        if (!selectedExp.isNullOrEmpty() && selectedExp != "All Items") {
            val expirationStatus = ExpirationUtils.getStatusFromFilter(selectedExp)
            if (expirationStatus != null) {
                filteredItems = ExpirationUtils.filterByExpirationStatus(filteredItems, expirationStatus)
            }
        }

        _lebensmittelList.value = filteredItems
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Multi-Tenant Functions
    private fun fetchMultiTenantData() {
        viewModelScope.launch {
            try {
                // Fetch households first
                val householdsResponse = apiService.getMyHouseholds()
                if (householdsResponse.isSuccessful) {
                    val households = householdsResponse.body() ?: emptyList()
                    _households.value = households

                    // Set current household (first one for now)
                    if (households.isNotEmpty()) {
                        _currentHousehold.value = households.first()
                    }
                }

                // Fetch storage locations
                val locationsResponse = apiService.getStorageLocations()
                if (locationsResponse.isSuccessful) {
                    _storageLocations.value = locationsResponse.body() ?: emptyList()
                }

                // Fetch packages
                val packagesResponse = apiService.getPackages()
                if (packagesResponse.isSuccessful) {
                    _packages.value = packagesResponse.body() ?: emptyList()
                }

                // Now fetch lebensmittel
                fetchLebensmittel()

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load multi-tenant data: ${e.message}"
                // Still try to fetch lebensmittel even if multi-tenant data fails
                fetchLebensmittel()
            }
        }
    }

    fun refreshMultiTenantData() {
        fetchMultiTenantData()
    }

    fun getStorageLocationById(id: Int): StorageLocation? {
        return _storageLocations.value?.find { it.id == id }
    }

    fun getPackageById(id: Int): FoodPackage? {
        return _packages.value?.find { it.id == id }
    }
}
