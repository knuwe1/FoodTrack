package com.foodtrack.app.ui.scanner

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodtrack.app.data.model.OpenFoodFactsProduct
import com.foodtrack.app.data.network.OpenFoodFactsService
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class BarcodeScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "BarcodeScannerViewModel"
    }

    private val openFoodFactsService = OpenFoodFactsService.create()

    private val _productData = MutableLiveData<OpenFoodFactsProduct?>()
    val productData: LiveData<OpenFoodFactsProduct?> = _productData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadProductFromBarcode(barcode: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading product data for barcode: $barcode")
                val response = openFoodFactsService.getProduct(barcode)

                if (response.isSuccessful) {
                    val openFoodFactsResponse = response.body()
                    Log.d(TAG, "API Response: status=${openFoodFactsResponse?.status}, product=${openFoodFactsResponse?.product?.getBestProductName()}")

                    when {
                        openFoodFactsResponse?.status == 1 && openFoodFactsResponse.product != null -> {
                            val product = openFoodFactsResponse.product
                            if (product.isComplete()) {
                                _productData.value = product
                                Log.d(TAG, "Product loaded successfully: ${product.getBestProductName()}")
                            } else {
                                _errorMessage.value = "Produktdaten unvollständig. Bitte manuell eingeben."
                                Log.w(TAG, "Product data incomplete")
                            }
                        }
                        openFoodFactsResponse?.status == 0 -> {
                            _errorMessage.value = "Produkt nicht in der Datenbank gefunden. Bitte manuell eingeben."
                            Log.w(TAG, "Product not found in OpenFoodFacts database")
                        }
                        else -> {
                            _errorMessage.value = "Unerwartete Antwort vom Server. Bitte manuell eingeben."
                            Log.w(TAG, "Unexpected response from OpenFoodFacts API")
                        }
                    }
                } else {
                    val errorMsg = "Fehler beim Laden der Produktdaten (${response.code()}). Bitte manuell eingeben."
                    _errorMessage.value = errorMsg
                    Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: HttpException) {
                val errorMsg = "Netzwerkfehler: ${e.message()}. Bitte manuell eingeben."
                _errorMessage.value = errorMsg
                Log.e(TAG, "HTTP Exception", e)
            } catch (e: IOException) {
                val errorMsg = "Verbindungsfehler. Bitte Internetverbindung prüfen."
                _errorMessage.value = errorMsg
                Log.e(TAG, "IO Exception", e)
            } catch (e: Exception) {
                val errorMsg = "Unerwarteter Fehler: ${e.message}. Bitte manuell eingeben."
                _errorMessage.value = errorMsg
                Log.e(TAG, "Unexpected Exception", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
