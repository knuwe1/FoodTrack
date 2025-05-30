package com.foodtrack.app.ui.lebensmittel

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.ui.categories.CategoryManagementActivity
import com.foodtrack.app.ui.transactions.TransactionViewModel
import com.foodtrack.app.ui.transactions.SimpleTransactionHistoryActivity
import com.foodtrack.app.ui.scanner.BarcodeScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LebensmittelListActivity : AppCompatActivity() {

    private val viewModel: LebensmittelViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var lebensmittelAdapter: LebensmittelAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyListMessage: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabScan: FloatingActionButton
    private lateinit var btnCategories: MaterialButton
    private lateinit var btnTransactionHistory: MaterialButton
    private lateinit var spinnerCategoryFilter: Spinner
    private lateinit var spinnerExpirationFilter: Spinner
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private lateinit var expirationAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lebensmittel_list)

        // Initialize views
        recyclerView = findViewById(R.id.rvLebensmittelList)
        progressBar = findViewById(R.id.pbListLoading)
        emptyListMessage = findViewById(R.id.tvEmptyListMessage)
        fabAdd = findViewById(R.id.fabAddLebensmittel)
        fabScan = findViewById(R.id.fabScan)
        btnCategories = findViewById(R.id.btnCategories)
        btnTransactionHistory = findViewById(R.id.btnTransactionHistory)
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter)
        spinnerExpirationFilter = findViewById(R.id.spinnerExpirationFilter)

        setupRecyclerView()
        setupFilters()
        observeViewModel()
        observeTransactionViewModel()

        // Initial fetch is done in ViewModel's init block,
        // but you might want to add a swipe-to-refresh or a button to call this explicitly later.
        // viewModel.fetchLebensmittel() // Uncomment if not fetching in init or need refresh logic here

        fabAdd.setOnClickListener {
            val intent = Intent(this, com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity::class.java)
            startActivity(intent)
        }

        fabScan.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            startActivity(intent)
        }

        btnCategories.setOnClickListener {
            try {
                val intent = Intent(this, CategoryManagementActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Categories feature coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        btnTransactionHistory.setOnClickListener {
            val intent = Intent(this, SimpleTransactionHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when the activity resumes
        viewModel.fetchLebensmittel()
    }

    private fun setupRecyclerView() {
        lebensmittelAdapter = LebensmittelAdapter(
            onItemClicked = { lebensmittel ->
                // Handle item click - navigate to detail/edit screen
                val intent = Intent(this, com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity::class.java).apply {
                    putExtra(com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity.EXTRA_LEBENSMITTEL_ID, lebensmittel.id)
                }
                startActivity(intent)
            },
            onPurchaseClicked = { lebensmittel ->
                showTransactionDialog(lebensmittel, isConsumption = false)
            },
            onConsumeClicked = { lebensmittel ->
                showTransactionDialog(lebensmittel, isConsumption = true)
            }
        )
        recyclerView.apply {
            adapter = lebensmittelAdapter
            layoutManager = LinearLayoutManager(this@LebensmittelListActivity)
        }
    }

    private fun updateUiVisibility() {
        val isLoading = viewModel.isLoading.value ?: false
        val currentList = viewModel.lebensmittelList.value
        val currentError = viewModel.errorMessage.value

        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            recyclerView.visibility = View.GONE
            emptyListMessage.visibility = View.GONE
        } else {
            if (currentError != null) {
                recyclerView.visibility = View.GONE
                emptyListMessage.text = currentError
                emptyListMessage.visibility = View.VISIBLE
                Toast.makeText(this, currentError, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage() // Clear after showing
            } else if (currentList.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                emptyListMessage.text = "No food items found." // Default empty message
                emptyListMessage.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyListMessage.visibility = View.GONE
                lebensmittelAdapter.submitList(currentList) // submitList should be here
            }
        }
    }

    private fun setupFilters() {
        // Setup category filter
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoryFilter.adapter = categoryAdapter

        spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryAdapter.getItem(position)
                viewModel.setSelectedCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Setup expiration filter
        expirationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        expirationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExpirationFilter.adapter = expirationAdapter

        spinnerExpirationFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedFilter = expirationAdapter.getItem(position)
                viewModel.setSelectedExpirationFilter(selectedFilter)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun observeViewModel() {
        viewModel.lebensmittelList.observe(this) { lebensmittelList ->
            // Only submit list if not loading and no error
            if (viewModel.isLoading.value == false && viewModel.errorMessage.value == null) {
                lebensmittelAdapter.submitList(lebensmittelList ?: emptyList())
            }
            updateUiVisibility()
        }

        viewModel.categories.observe(this) { categories ->
            categoryAdapter.clear()
            categoryAdapter.addAll(categories ?: emptyList())
            categoryAdapter.notifyDataSetChanged()
        }

        viewModel.expirationFilterOptions.observe(this) { options ->
            expirationAdapter.clear()
            expirationAdapter.addAll(options ?: emptyList())
            expirationAdapter.notifyDataSetChanged()
        }

        viewModel.isLoading.observe(this) { _ -> updateUiVisibility() }
        viewModel.errorMessage.observe(this) { _ -> updateUiVisibility() }
    }

    private fun observeTransactionViewModel() {
        transactionViewModel.transactionSuccess.observe(this) { transaction ->
            if (transaction != null) {
                Toast.makeText(this, "Transaktion erfolgreich!", Toast.LENGTH_SHORT).show()
                // Refresh the list to show updated quantities
                viewModel.fetchLebensmittel()
                transactionViewModel.clearTransactionSuccess()
            }
        }

        transactionViewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, "Fehler: $error", Toast.LENGTH_LONG).show()
                transactionViewModel.clearError()
            }
        }
    }

    private fun showTransactionDialog(lebensmittel: Lebensmittel, isConsumption: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvLebensmittelName = dialogView.findViewById<TextView>(R.id.tvLebensmittelName)
        val tvCurrentQuantity = dialogView.findViewById<TextView>(R.id.tvCurrentQuantity)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etReason)
        val etMhd = dialogView.findViewById<TextInputEditText>(R.id.etMhd)
        val layoutMhd = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layoutMhd)

        // Setup dialog content
        tvDialogTitle.text = if (isConsumption) "Verbrauch erfassen" else "Einkauf erfassen"
        tvLebensmittelName.text = "Lebensmittel: ${lebensmittel.name}"
        tvCurrentQuantity.text = "Aktuelle Menge: ${lebensmittel.menge ?: 0} ${lebensmittel.einheit ?: ""}"

        // MHD-Feld nur bei Einkauf anzeigen
        layoutMhd.visibility = if (isConsumption) android.view.View.GONE else android.view.View.VISIBLE

        // DatePicker für MHD-Feld einrichten
        if (!isConsumption) {
            setupDatePicker(etMhd, layoutMhd)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            val quantityText = etQuantity.text?.toString()
            val reason = etReason.text?.toString()?.takeIf { it.isNotBlank() }
            val mhd = etMhd.text?.toString()?.takeIf { it.isNotBlank() }

            if (quantityText.isNullOrBlank()) {
                Toast.makeText(this, "Bitte Menge eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityText.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(this, "Bitte gültige Menge eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validiere MHD-Format (optional)
            if (!isConsumption && mhd != null && !isValidDateFormat(mhd)) {
                Toast.makeText(this, "Bitte gültiges MHD-Format verwenden (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isConsumption) {
                transactionViewModel.recordConsumption(lebensmittel.id, quantity, reason)
            } else {
                transactionViewModel.recordPurchase(lebensmittel.id, quantity, reason, mhd)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDatePicker(etMhd: TextInputEditText, layoutMhd: com.google.android.material.textfield.TextInputLayout) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Click-Listener für das Textfeld
        etMhd.setOnClickListener {
            showDatePickerDialog(etMhd, dateFormat, calendar)
        }

        // Click-Listener für das End-Icon (Kalender-Symbol)
        layoutMhd.setEndIconOnClickListener {
            showDatePickerDialog(etMhd, dateFormat, calendar)
        }
    }

    private fun showDatePickerDialog(etMhd: TextInputEditText, dateFormat: SimpleDateFormat, calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val formattedDate = dateFormat.format(calendar.time)
                etMhd.setText(formattedDate)
            },
            year, month, day
        )

        // Setze Mindestdatum auf heute
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            regex.matches(date)
        } catch (e: Exception) {
            false
        }
    }
}
