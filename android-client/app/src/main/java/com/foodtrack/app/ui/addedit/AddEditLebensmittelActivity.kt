package com.foodtrack.app.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.foodtrack.app.R
import com.foodtrack.app.data.local.CategoryManager
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.model.StorageLocation
import com.foodtrack.app.data.model.Package as FoodPackage
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditLebensmittelActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddEditLebensmittel"
        const val EXTRA_LEBENSMITTEL_ID = "lebensmittel_id"
    }

    private val viewModel: AddEditLebensmittelViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var tilMenge: TextInputLayout
    private lateinit var etMenge: TextInputEditText
    private lateinit var tilEinheit: TextInputLayout
    private lateinit var etEinheit: AutoCompleteTextView
    private lateinit var tilKategorie: TextInputLayout
    private lateinit var spinnerKategorie: AutoCompleteTextView
    private lateinit var tilAblaufdatum: TextInputLayout
    private lateinit var etAblaufdatum: TextInputEditText
    private lateinit var tilEanCode: TextInputLayout
    private lateinit var etEanCode: TextInputEditText
    private lateinit var tilMindestmenge: TextInputLayout
    private lateinit var etMindestmenge: TextInputEditText
    private lateinit var tilStorageLocation: TextInputLayout
    private lateinit var spinnerStorageLocation: AutoCompleteTextView
    private lateinit var tilPackage: TextInputLayout
    private lateinit var spinnerPackage: AutoCompleteTextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var categoryManager: CategoryManager
    private lateinit var unitAdapter: ArrayAdapter<String>
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private lateinit var storageLocationAdapter: ArrayAdapter<String>
    private lateinit var packageAdapter: ArrayAdapter<String>
    private var currentLebensmittelId: Int? = null

    // Multi-Tenant data for mapping
    private var storageLocations: List<StorageLocation> = emptyList()
    private var packages: List<FoodPackage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_lebensmittel_modern)

        categoryManager = CategoryManager(this)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        tilName = findViewById(R.id.tilName)
        etName = findViewById(R.id.etName)
        tilMenge = findViewById(R.id.tilMenge)
        etMenge = findViewById(R.id.etMenge)
        tilEinheit = findViewById(R.id.tilEinheit)
        etEinheit = findViewById(R.id.etEinheit)
        tilKategorie = findViewById(R.id.tilKategorie)
        spinnerKategorie = findViewById(R.id.spinnerKategorie)
        tilAblaufdatum = findViewById(R.id.tilAblaufdatum)
        etAblaufdatum = findViewById(R.id.etAblaufdatum)
        tilEanCode = findViewById(R.id.tilEanCode)
        etEanCode = findViewById(R.id.etEanCode)
        tilMindestmenge = findViewById(R.id.tilMindestmenge)
        etMindestmenge = findViewById(R.id.etMindestmenge)
        tilStorageLocation = findViewById(R.id.tilStorageLocation)
        spinnerStorageLocation = findViewById(R.id.spinnerStorageLocation)
        tilPackage = findViewById(R.id.tilPackage)
        spinnerPackage = findViewById(R.id.spinnerPackage)
        btnSave = findViewById(R.id.btnSaveLebensmittel)
        btnDelete = findViewById(R.id.btnDeleteLebensmittel)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        progressBar = findViewById(R.id.pbAddEditLoading)

        setupToolbar()
        setupDatePickers()
        setupUnitDropdown()
        setupCategoryDropdown()
        setupMultiTenantDropdowns()

        currentLebensmittelId = if (intent.hasExtra(EXTRA_LEBENSMITTEL_ID)) {
            intent.getIntExtra(EXTRA_LEBENSMITTEL_ID, -1).takeIf { it != -1 }
        } else null

        if (currentLebensmittelId != null) {
            toolbar.title = "Lebensmittel bearbeiten"
            btnDelete.visibility = View.VISIBLE
            viewModel.fetchLebensmittelDetails(currentLebensmittelId!!)
        } else {
            toolbar.title = "Lebensmittel hinzufügen"
            btnDelete.visibility = View.GONE
        }

        // Check if we're coming from barcode scanner
        val fromScanner = intent.getBooleanExtra("FROM_SCANNER", false)
        if (fromScanner) {
            handleScannerData()
        }

        btnSave.setOnClickListener {
            saveLebensmittel()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        observeViewModel()
        observeMultiTenantData()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDatePickers() {
        etAblaufdatum.setOnClickListener { showDatePickerDialog() }
        tilAblaufdatum.setEndIconOnClickListener { showDatePickerDialog() }
    }

    private fun setupUnitDropdown() {
        val units = listOf(
            "Stück", "kg", "g", "l", "ml", "Packung", "Dose", "Flasche",
            "Beutel", "Karton", "Tube", "Glas", "Becher", "Tüte"
        )
        unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
        etEinheit.setAdapter(unitAdapter)
    }

    private fun setupCategoryDropdown() {
        try {
            val categoryNames = categoryManager.getCategoryNames()
            Log.d(TAG, "Loaded ${categoryNames.size} category names")

            categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
            spinnerKategorie.setAdapter(categoryAdapter)

            Log.d(TAG, "Category dropdown setup complete with ${categoryNames.size} items")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading categories: ${e.message}", Toast.LENGTH_LONG).show()

            // Fallback categories
            val fallbackCategories = listOf("Obst", "Gemüse", "Fleisch", "Milchprodukte", "Getränke")
            categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fallbackCategories)
            spinnerKategorie.setAdapter(categoryAdapter)
        }
    }



    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        // Try to parse existing date in EditText to set initial date in picker
        val existingDate = etAblaufdatum.text.toString()
        if (existingDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = sdf.parse(existingDate)!!
            } catch (e: Exception) {
                // Invalid date format, use current date
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etAblaufdatum.setText(sdf.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }


    private fun saveLebensmittel() {
        val name = etName.text.toString().trim()
        val menge = etMenge.text.toString().trim()
        val einheit = etEinheit.text.toString().trim()
        val kategorie = spinnerKategorie.text.toString().trim()
        val ablaufdatum = etAblaufdatum.text.toString().trim()
        val eanCode = etEanCode.text.toString().trim()
        val mindestmenge = etMindestmenge.text.toString().trim()

        // Multi-Tenant values
        val selectedStorageLocation = getSelectedStorageLocationId()
        val selectedPackage = getSelectedPackageId()
        val packageCount = 1 // Default for now, could be made configurable

        // Basic validation
        if (name.isEmpty()) {
            tilName.error = "Name ist erforderlich"
            return
        } else {
            tilName.error = null
        }

        viewModel.saveLebensmittel(
            currentLebensmittelId,
            name,
            menge,
            einheit,
            kategorie,
            ablaufdatum,
            eanCode,
            mindestmenge,
            selectedStorageLocation,
            selectedPackage,
            packageCount
        )
    }

    private fun observeViewModel() {
        viewModel.lebensmittelDetails.observe(this) { lebensmittel ->
            lebensmittel?.let { populateFields(it) }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSave.isEnabled = !isLoading
            btnDelete.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage() // Clear error after showing
            }
        }

        viewModel.saveResult.observe(this) { success ->
            success?.let {
                if (it) {
                    val message = if (currentLebensmittelId == null) {
                        "Item created successfully!"
                    } else {
                        "Item updated successfully!"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish() // Go back to the list
                }
                // If !success and there's no specific error message shown by errorMessage LiveData,
                // you might want to show a generic "Save failed" Toast here.
                // However, the ViewModel's saveLebensmittel already sets errorMessage.
                viewModel.clearSaveResult() // Reset for next operation
            }
        }

        viewModel.deleteResult.observe(this) { success ->
            success?.let {
                if (it) {
                    Toast.makeText(this, "Lebensmittel gelöscht!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to the list
                }
                // If !success, an error message should have been shown via errorMessage LiveData
                viewModel.clearDeleteResult() // Reset for next operation
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        if (currentLebensmittelId == null) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirmation, null)
        val tvDeleteMessage = dialogView.findViewById<TextView>(R.id.tvDeleteMessage)

        // Customize message with product name
        val productName = etName.text.toString().takeIf { it.isNotEmpty() } ?: "dieses Lebensmittel"
        tvDeleteMessage.text = "Möchten Sie '$productName' wirklich löschen?"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            viewModel.deleteLebensmittel(currentLebensmittelId!!)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun populateFields(lebensmittel: Lebensmittel) {
        etName.setText(lebensmittel.name)
        etMenge.setText(lebensmittel.menge?.toString() ?: "")
        etEinheit.setText(lebensmittel.einheit ?: "", false)
        spinnerKategorie.setText(lebensmittel.kategorie ?: "", false)
        etAblaufdatum.setText(lebensmittel.ablaufdatum ?: "")
        etEanCode.setText(lebensmittel.eanCode ?: "")
        etMindestmenge.setText(lebensmittel.mindestmenge?.toString() ?: "")

        // Populate Multi-Tenant fields
        populateStorageLocationField(lebensmittel.storageLocationId)
        populatePackageField(lebensmittel.packageId)
    }

    private fun handleScannerData() {
        // Hole Daten aus dem Intent
        val productName = intent.getStringExtra("PRODUCT_NAME")
        val productCategory = intent.getStringExtra("PRODUCT_CATEGORY")
        val productUnit = intent.getStringExtra("PRODUCT_UNIT")
        val productQuantity = intent.getIntExtra("PRODUCT_QUANTITY", 0)
        val productBrand = intent.getStringExtra("PRODUCT_BRAND")
        val productPackaging = intent.getStringExtra("PRODUCT_PACKAGING")
        val barcode = intent.getStringExtra("BARCODE")
        val scanError = intent.getStringExtra("SCAN_ERROR")

        // Zeige Scan-Ergebnis
        if (scanError != null) {
            Toast.makeText(this, "Scanner: $scanError", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Produktdaten aus Barcode geladen", Toast.LENGTH_SHORT).show()
        }

        // Fülle Felder mit gescannten Daten
        productName?.let { name ->
            // Kombiniere Name mit Marke falls verfügbar
            val fullName = if (productBrand != null && !name.contains(productBrand, ignoreCase = true)) {
                "$productBrand $name"
            } else {
                name
            }
            etName.setText(fullName)
        }

        productUnit?.let {
            etEinheit.setText(it, false)
        }

        // Setze Menge falls verfügbar
        if (productQuantity > 0) {
            etMenge.setText(productQuantity.toString())
        }

        // Setze Kategorie
        productCategory?.let { category ->
            spinnerKategorie.setText(category, false)
        }

        // Setze EAN-Code
        barcode?.let {
            etEanCode.setText(it)
        }

        // Zeige Zusammenfassung der gescannten Daten
        val scannedInfo = mutableListOf<String>()
        productName?.let { scannedInfo.add("Name: $it") }
        productBrand?.let { scannedInfo.add("Marke: $it") }
        productQuantity.takeIf { it > 0 }?.let { scannedInfo.add("Menge: $it") }
        productUnit?.let { scannedInfo.add("Einheit: $it") }
        productCategory?.let { scannedInfo.add("Kategorie: $it") }
        productPackaging?.let { scannedInfo.add("Verpackung: $it") }
        barcode?.let { scannedInfo.add("EAN: $it") }

        if (scannedInfo.isNotEmpty()) {
            Toast.makeText(this, "Gescannt: ${scannedInfo.joinToString(", ")}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMultiTenantDropdowns() {
        // Initialize empty adapters - will be populated when data is loaded
        storageLocationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        spinnerStorageLocation.setAdapter(storageLocationAdapter)

        packageAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        spinnerPackage.setAdapter(packageAdapter)
    }

    private fun observeMultiTenantData() {
        // Observe storage locations
        viewModel.storageLocations.observe(this) { locations ->
            this.storageLocations = locations
            val locationNames = locations.map { "${it.name} (${it.locationType})" }
            storageLocationAdapter.clear()
            storageLocationAdapter.addAll(locationNames)
            storageLocationAdapter.notifyDataSetChanged()
        }

        // Observe packages
        viewModel.packages.observe(this) { packages ->
            this.packages = packages
            val packageNames = packages.map { "${it.name} - ${it.fillAmount} ${it.fillUnit}" }
            packageAdapter.clear()
            packageAdapter.addAll(packageNames)
            packageAdapter.notifyDataSetChanged()
        }
    }

    private fun getSelectedStorageLocationId(): Int? {
        val selectedText = spinnerStorageLocation.text.toString()
        if (selectedText.isBlank()) return null

        return storageLocations.find {
            "${it.name} (${it.locationType})" == selectedText
        }?.id
    }

    private fun getSelectedPackageId(): Int? {
        val selectedText = spinnerPackage.text.toString()
        if (selectedText.isBlank()) return null

        return packages.find {
            "${it.name} - ${it.fillAmount} ${it.fillUnit}" == selectedText
        }?.id
    }

    private fun populateStorageLocationField(storageLocationId: Int?) {
        if (storageLocationId == null) return

        val storageLocation = storageLocations.find { it.id == storageLocationId }
        storageLocation?.let {
            val displayText = "${it.name} (${it.locationType})"
            spinnerStorageLocation.setText(displayText, false)
        }
    }

    private fun populatePackageField(packageId: Int?) {
        if (packageId == null) return

        val packageInfo = packages.find { it.id == packageId }
        packageInfo?.let {
            val displayText = "${it.name} - ${it.fillAmount} ${it.fillUnit}"
            spinnerPackage.setText(displayText, false)
        }
    }
}
