package com.foodtrack.app.ui.lebensmittel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LebensmittelViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application.applicationContext)

    private val _lebensmittelList = MutableLiveData<List<Lebensmittel>>()
    val lebensmittelList: LiveData<List<Lebensmittel>> = _lebensmittelList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchLebensmittel()
    }

    fun fetchLebensmittel() {
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            try {
                val response = apiService.getAllLebensmittel()
                if (response.isSuccessful) {
                    _lebensmittelList.value = response.body() ?: emptyList()
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

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
