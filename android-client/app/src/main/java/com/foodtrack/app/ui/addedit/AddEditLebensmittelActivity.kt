package com.foodtrack.app.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.foodtrack.app.R
import com.foodtrack.app.data.local.CategoryManager
import com.foodtrack.app.data.model.Lebensmittel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditLebensmittelActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddEditLebensmittel"
        const val EXTRA_LEBENSMITTEL_ID = "lebensmittel_id"
    }

    private val viewModel: AddEditLebensmittelViewModel by viewModels()

    private lateinit var etName: EditText
    private lateinit var etMenge: EditText
    private lateinit var etEinheit: EditText
    private lateinit var spinnerKategorie: Spinner
    private lateinit var etAblaufdatum: EditText
    private lateinit var etEanCode: EditText
    private lateinit var etMindestmenge: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button // Added Delete Button
    private lateinit var progressBar: ProgressBar

    private lateinit var categoryManager: CategoryManager
    private var currentLebensmittelId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_lebensmittel)

        categoryManager = CategoryManager(this)

        etName = findViewById(R.id.etName)
        etMenge = findViewById(R.id.etMenge)
        etEinheit = findViewById(R.id.etEinheit)
        spinnerKategorie = findViewById(R.id.spinnerKategorie)
        etAblaufdatum = findViewById(R.id.etAblaufdatum)
        etEanCode = findViewById(R.id.etEanCode)
        etMindestmenge = findViewById(R.id.etMindestmenge)
        btnSave = findViewById(R.id.btnSaveLebensmittel)
        btnDelete = findViewById(R.id.btnDeleteLebensmittel) // Initialize Delete Button
        progressBar = findViewById(R.id.pbAddEditLoading)

        setupDatePickers()
        setupCategorySpinner()

        currentLebensmittelId = if (intent.hasExtra(EXTRA_LEBENSMITTEL_ID)) {
            intent.getIntExtra(EXTRA_LEBENSMITTEL_ID, -1).takeIf { it != -1 }
        } else null


        if (currentLebensmittelId != null) {
            title = "Lebensmittel bearbeiten"
            btnDelete.visibility = View.VISIBLE // Show delete button in edit mode
            viewModel.fetchLebensmittelDetails(currentLebensmittelId!!)
        } else {
            title = "Lebensmittel hinzufügen"
            btnDelete.visibility = View.GONE // Hide delete button in add mode
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
    }

    private fun setupDatePickers() {
        etAblaufdatum.setOnClickListener { showDatePickerDialog(etAblaufdatum) }
    }

    private fun setupCategorySpinner() {
        try {
            val categoryNames = categoryManager.getCategoryNames()

            // Debug logging
            Log.d(TAG, "Loaded ${categoryNames.size} category names")
            Toast.makeText(this, "Loaded ${categoryNames.size} category names", Toast.LENGTH_SHORT).show()
            categoryNames.forEach { name ->
                Log.d(TAG, "Category name: $name")
            }

            val categories = mutableListOf("-- Select Category --").apply {
                addAll(categoryNames)
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKategorie.adapter = adapter

            Log.d(TAG, "Spinner setup complete with ${categories.size} items")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading categories: ${e.message}", Toast.LENGTH_LONG).show()

            // Fallback spinner
            val fallbackCategories = listOf("-- Select Category --", "Obst", "Gemüse", "Fleisch")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fallbackCategories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKategorie.adapter = adapter
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        // Try to parse existing date in EditText to set initial date in picker
        val existingDate = editText.text.toString()
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
                editText.setText(sdf.format(selectedDate.time))
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

        // Get selected category from spinner
        val selectedCategory = spinnerKategorie.selectedItem.toString()
        val kategorie = if (selectedCategory == "-- Select Category --") "" else selectedCategory

        val ablaufdatum = etAblaufdatum.text.toString().trim()
        val eanCode = etEanCode.text.toString().trim()
        val mindestmenge = etMindestmenge.text.toString().trim()

        viewModel.saveLebensmittel(currentLebensmittelId, name, menge, einheit, kategorie, ablaufdatum, eanCode, mindestmenge)
    }

    private fun observeViewModel() {
        viewModel.lebensmittelDetails.observe(this) { lebensmittel ->
            lebensmittel?.let { populateFields(it) }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSave.isEnabled = !isLoading
            btnDelete.isEnabled = !isLoading // Also disable delete button when loading
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
        if (currentLebensmittelId == null) return // Should not happen if button is visible

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Löschen bestätigen")
            .setMessage("Möchten Sie dieses Lebensmittel wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                viewModel.deleteLebensmittel(currentLebensmittelId!!)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun populateFields(lebensmittel: Lebensmittel) {
        etName.setText(lebensmittel.name)
        etMenge.setText(lebensmittel.menge?.toString() ?: "")
        etEinheit.setText(lebensmittel.einheit ?: "")

        // Set category in spinner
        val kategorie = lebensmittel.kategorie ?: ""
        val adapter = spinnerKategorie.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(kategorie)
        if (position >= 0) {
            spinnerKategorie.setSelection(position)
        } else {
            spinnerKategorie.setSelection(0) // Select "-- Select Category --"
        }

        etAblaufdatum.setText(lebensmittel.ablaufdatum ?: "")
        etEanCode.setText(lebensmittel.eanCode ?: "")
        etMindestmenge.setText(lebensmittel.mindestmenge?.toString() ?: "")
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
            etEinheit.setText(it)
        }

        // Setze Menge falls verfügbar
        if (productQuantity > 0) {
            etMenge.setText(productQuantity.toString())
        }

        // Setze Kategorie im Spinner
        productCategory?.let { category ->
            val adapter = spinnerKategorie.adapter as ArrayAdapter<String>
            val categoryPosition = adapter.getPosition(category)
            if (categoryPosition >= 0) {
                spinnerKategorie.setSelection(categoryPosition)
            } else {
                // Kategorie nicht gefunden - füge sie hinzu
                adapter.add(category)
                adapter.notifyDataSetChanged()
                spinnerKategorie.setSelection(adapter.count - 1)
            }
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
}
