package com.foodtrack.app.ui.transactions

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.model.Transaction
import com.foodtrack.app.data.model.TransactionCreate
import com.foodtrack.app.data.model.TransactionType
import com.foodtrack.app.data.model.StatisticsOverview
import com.foodtrack.app.data.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getApiService(application.applicationContext)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _transactionSuccess = MutableLiveData<Transaction?>()
    val transactionSuccess: LiveData<Transaction?> = _transactionSuccess

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _statistics = MutableLiveData<StatisticsOverview?>()
    val statistics: LiveData<StatisticsOverview?> = _statistics

    companion object {
        private const val TAG = "TransactionViewModel"
    }

    fun recordPurchase(lebensmittelId: Int, quantity: Int, reason: String?, mhd: String? = null) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Recording purchase: lebensmittelId=$lebensmittelId, quantity=$quantity, reason=$reason, mhd=$mhd")

                // Use general transaction API instead of convenience endpoint
                val transactionCreate = TransactionCreate(
                    lebensmittelId = lebensmittelId,
                    transactionType = TransactionType.PURCHASE,
                    quantityChange = quantity,
                    reason = reason
                )
                val response = apiService.createTransaction(transactionCreate)

                if (response.isSuccessful) {
                    val transaction = response.body()
                    _transactionSuccess.value = transaction
                    Log.d(TAG, "Purchase recorded successfully: ${transaction?.id}")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to record purchase"
                    _errorMessage.value = "Error ${response.code()}: $errorBody"
                    Log.e(TAG, "Purchase failed: ${response.code()} - $errorBody")
                }
            } catch (e: HttpException) {
                val errorMsg = "Network error: ${e.message()}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, e)
            } catch (e: IOException) {
                val errorMsg = "Network connection issue: ${e.message}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "An unexpected error occurred: ${e.message}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun recordConsumption(lebensmittelId: Int, quantity: Int, reason: String?) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Recording consumption: lebensmittelId=$lebensmittelId, quantity=$quantity, reason=$reason")

                // Use general transaction API instead of convenience endpoint
                val transactionCreate = TransactionCreate(
                    lebensmittelId = lebensmittelId,
                    transactionType = TransactionType.CONSUMPTION,
                    quantityChange = quantity,
                    reason = reason
                )
                val response = apiService.createTransaction(transactionCreate)

                if (response.isSuccessful) {
                    val transaction = response.body()
                    _transactionSuccess.value = transaction
                    Log.d(TAG, "Consumption recorded successfully: ${transaction?.id}")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to record consumption"
                    _errorMessage.value = "Error ${response.code()}: $errorBody"
                    Log.e(TAG, "Consumption failed: ${response.code()} - $errorBody")
                }
            } catch (e: HttpException) {
                val errorMsg = "Network error: ${e.message()}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, e)
            } catch (e: IOException) {
                val errorMsg = "Network connection issue: ${e.message}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "An unexpected error occurred: ${e.message}"
                _errorMessage.value = errorMsg
                Log.e(TAG, errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTransactions(lebensmittelId: Int? = null) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = apiService.getAllTransactions(lebensmittelId = lebensmittelId)

                if (response.isSuccessful) {
                    _transactions.value = response.body() ?: emptyList()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to fetch transactions"
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

    fun fetchStatistics() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = apiService.getStatisticsOverview()

                if (response.isSuccessful) {
                    _statistics.value = response.body()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Failed to fetch statistics"
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

    fun clearTransactionSuccess() {
        _transactionSuccess.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
