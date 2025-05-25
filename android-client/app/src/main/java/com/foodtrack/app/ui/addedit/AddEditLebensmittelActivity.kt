package com.foodtrack.app.ui.addedit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditLebensmittelActivity : AppCompatActivity() {

    private val viewModel: AddEditLebensmittelViewModel by viewModels()

    private lateinit var etName: EditText
    private lateinit var etMenge: EditText
    private lateinit var etEinheit: EditText
    private lateinit var etKategorie: EditText
    private lateinit var etKaufdatum: EditText
    private lateinit var etMhd: EditText
    private lateinit var etLagerort: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button // Added Delete Button
    private lateinit var progressBar: ProgressBar

    private var currentLebensmittelId: Int? = null

    companion object {
        const val EXTRA_LEBENSMIITEL_ID = "lebensmittel_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_lebensmittel)

        etName = findViewById(R.id.etName)
        etMenge = findViewById(R.id.etMenge)
        etEinheit = findViewById(R.id.etEinheit)
        etKategorie = findViewById(R.id.etKategorie)
        etKaufdatum = findViewById(R.id.etKaufdatum)
        etMhd = findViewById(R.id.etMhd)
        etLagerort = findViewById(R.id.etLagerort)
        btnSave = findViewById(R.id.btnSaveLebensmittel)
        btnDelete = findViewById(R.id.btnDeleteLebensmittel) // Initialize Delete Button
        progressBar = findViewById(R.id.pbAddEditLoading)

        setupDatePickers()

        currentLebensmittelId = if (intent.hasExtra(EXTRA_LEBENSMIITEL_ID)) {
            intent.getIntExtra(EXTRA_LEBENSMIITEL_ID, -1).takeIf { it != -1 }
        } else null


        if (currentLebensmittelId != null) {
            title = "Lebensmittel bearbeiten"
            btnDelete.visibility = View.VISIBLE // Show delete button in edit mode
            viewModel.fetchLebensmittelDetails(currentLebensmittelId!!)
        } else {
            title = "Lebensmittel hinzufügen"
            btnDelete.visibility = View.GONE // Hide delete button in add mode
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
        etKaufdatum.setOnClickListener { showDatePickerDialog(etKaufdatum) }
        etMhd.setOnClickListener { showDatePickerDialog(etMhd) }
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
        val kategorie = etKategorie.text.toString().trim()
        val kaufdatum = etKaufdatum.text.toString().trim()
        val mhd = etMhd.text.toString().trim()
        val lagerort = etLagerort.text.toString().trim()

        viewModel.saveLebensmittel(currentLebensmittelId, name, menge, einheit, kategorie, kaufdatum, mhd, lagerort)
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
            if (success) {
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

        viewModel.deleteResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Lebensmittel gelöscht!", Toast.LENGTH_SHORT).show()
                finish() // Go back to the list
            }
            // If !success, an error message should have been shown via errorMessage LiveData
            viewModel.clearDeleteResult() // Reset for next operation
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
        etKategorie.setText(lebensmittel.kategorie ?: "")
        etKaufdatum.setText(lebensmittel.kaufdatum ?: "")
        etMhd.setText(lebensmittel.mhd ?: "")
        etLagerort.setText(lebensmittel.lagerort ?: "")
    }
}
