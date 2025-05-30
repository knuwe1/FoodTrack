package com.foodtrack.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.foodtrack.app.R
import com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class BarcodeScannerActivity : AppCompatActivity() {

    private val viewModel: BarcodeScannerViewModel by viewModels()

    // Kamera-Berechtigung Request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startBarcodeScanner()
        } else {
            Toast.makeText(this, "Kamera-Berechtigung erforderlich für Barcode-Scan", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Barcode Scanner Launcher
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan abgebrochen", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            val barcode = result.contents
            Toast.makeText(this, "Barcode gescannt: $barcode", Toast.LENGTH_SHORT).show()

            // Lade Produktdaten von OpenFoodFacts
            viewModel.loadProductFromBarcode(barcode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        setupObservers()
        checkCameraPermissionAndScan()
    }

    private fun setupObservers() {
        viewModel.productData.observe(this, Observer { product ->
            if (product != null) {
                // Produktdaten gefunden - öffne AddEdit Activity mit vorausgefüllten Daten
                val intent = Intent(this, AddEditLebensmittelActivity::class.java).apply {
                    putExtra("PRODUCT_NAME", product.getBestProductName())
                    putExtra("PRODUCT_CATEGORY", product.getBestCategory())
                    putExtra("PRODUCT_UNIT", product.getUnit())
                    putExtra("PRODUCT_QUANTITY", product.getQuantityValue())
                    putExtra("PRODUCT_BRAND", product.getBestBrand())
                    putExtra("PRODUCT_PACKAGING", product.getPackagingType())
                    putExtra("BARCODE", product.code)
                    putExtra("FROM_SCANNER", true)
                }
                startActivity(intent)
                finish()
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                // Bei Fehler trotzdem AddEdit Activity öffnen, aber ohne vorausgefüllte Daten
                val intent = Intent(this, AddEditLebensmittelActivity::class.java).apply {
                    putExtra("FROM_SCANNER", true)
                    putExtra("SCAN_ERROR", error)
                }
                startActivity(intent)
                finish()
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            // Hier könnte ein Loading-Indikator angezeigt werden
            if (isLoading) {
                Toast.makeText(this, "Lade Produktdaten...", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startBarcodeScanner()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Barcode scannen")
            setCameraId(0) // Rückkamera
            setBeepEnabled(true)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        barcodeLauncher.launch(options)
    }
}
