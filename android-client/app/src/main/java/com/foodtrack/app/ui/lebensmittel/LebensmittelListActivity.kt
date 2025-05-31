package com.foodtrack.app.ui.lebensmittel

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.ui.transactions.TransactionViewModel
import com.foodtrack.app.ui.scanner.BarcodeScannerActivity
import com.foodtrack.app.ui.categories.CategoryManagementActivity
import com.foodtrack.app.ui.transactions.SimpleTransactionHistoryActivity
import com.foodtrack.app.ui.multitenant.MultiTenantTestActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LebensmittelListActivity : AppCompatActivity() {

    private val viewModel: LebensmittelViewModel by viewModels()
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var lebensmittelAdapter: LebensmittelAdapter
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabScan: FloatingActionButton
    private lateinit var spinnerKategorie: AutoCompleteTextView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipFresh: Chip
    private lateinit var chipExpiringSoon: Chip
    private lateinit var chipExpired: Chip
    private lateinit var chipLowStock: Chip
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lebensmittel_list_new)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerViewLebensmittel)
        fabAdd = findViewById(R.id.fabAdd)
        fabScan = findViewById(R.id.fabScan)
        spinnerKategorie = findViewById(R.id.spinnerKategorie)
        chipGroupStatus = findViewById(R.id.chipGroupStatus)
        chipAll = findViewById(R.id.chipAll)
        chipFresh = findViewById(R.id.chipFresh)
        chipExpiringSoon = findViewById(R.id.chipExpiringSoon)
        chipExpired = findViewById(R.id.chipExpired)
        chipLowStock = findViewById(R.id.chipLowStock)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupClickListeners()
        observeViewModel()
        observeTransactionViewModel()
        observeMultiTenantData()

        // Initial fetch is done in ViewModel's init block,
        // but you might want to add a swipe-to-refresh or a button to call this explicitly later.
        // viewModel.fetchLebensmittel() // Uncomment if not fetching in init or need refresh logic here
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            val intent = Intent(this, com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity::class.java)
            startActivity(intent)
        }

        fabScan.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.my_food_items)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_categories -> {
                try {
                    val intent = Intent(this, CategoryManagementActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Categories feature coming soon!", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_transaction_history -> {
                val intent = Intent(this, SimpleTransactionHistoryActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_multi_tenant_test -> {
                val intent = Intent(this, MultiTenantTestActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupFilters() {
        // Setup category filter
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        spinnerKategorie.setAdapter(categoryAdapter)

        spinnerKategorie.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categoryAdapter.getItem(position)
            viewModel.setSelectedCategory(selectedCategory)
        }

        // Setup status filter chips
        chipGroupStatus.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = findViewById<Chip>(checkedIds[0])
                val filterType = when (checkedChip.id) {
                    R.id.chipAll -> "all"
                    R.id.chipFresh -> "fresh"
                    R.id.chipExpiringSoon -> "expiring_soon"
                    R.id.chipExpired -> "expired"
                    R.id.chipLowStock -> "low_stock"
                    else -> "all"
                }
                viewModel.setSelectedExpirationFilter(filterType)
            }
        }

        // Set default selection
        chipAll.isChecked = true
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
            },
            getStorageLocationById = { id -> viewModel.getStorageLocationById(id) },
            getPackageById = { id -> viewModel.getPackageById(id) }
        )
        recyclerView.apply {
            adapter = lebensmittelAdapter
            layoutManager = LinearLayoutManager(this@LebensmittelListActivity)
        }
    }





    private fun observeViewModel() {
        viewModel.lebensmittelList.observe(this) { lebensmittelList ->
            lebensmittelAdapter.submitList(lebensmittelList ?: emptyList())
        }

        viewModel.categories.observe(this) { categories ->
            categoryAdapter.clear()
            categoryAdapter.add("Alle Kategorien") // Add "All Categories" option
            categoryAdapter.addAll(categories ?: emptyList())
            categoryAdapter.notifyDataSetChanged()
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_modern, null)

        val ivTransactionIcon = dialogView.findViewById<android.widget.ImageView>(R.id.ivTransactionIcon)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvLebensmittelName = dialogView.findViewById<TextView>(R.id.tvLebensmittelName)
        val tvCurrentQuantity = dialogView.findViewById<TextView>(R.id.tvCurrentQuantity)
        val tvCurrentExpiration = dialogView.findViewById<TextView>(R.id.tvCurrentExpiration)
        val layoutCurrentExpiration = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutCurrentExpiration)
        val tilQuantity = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilQuantity)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val tilReason = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilReason)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etReason)
        val tilMhd = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilMhd)
        val etMhd = dialogView.findViewById<TextInputEditText>(R.id.etMhd)

        // Setup dialog content
        tvDialogTitle.text = if (isConsumption) "Verbrauch erfassen" else "Einkauf erfassen"
        tvLebensmittelName.text = lebensmittel.name
        tvCurrentQuantity.text = "${lebensmittel.menge ?: 0} ${lebensmittel.einheit ?: ""}"

        // Setup transaction icon
        if (isConsumption) {
            ivTransactionIcon.setImageResource(R.drawable.ic_remove_24)
            ivTransactionIcon.background = ContextCompat.getDrawable(this, R.drawable.warning_icon_background)
        } else {
            ivTransactionIcon.setImageResource(R.drawable.ic_add_24)
            ivTransactionIcon.background = ContextCompat.getDrawable(this, R.drawable.transaction_icon_background)
        }

        // Setup current expiration info
        if (!lebensmittel.ablaufdatum.isNullOrEmpty()) {
            layoutCurrentExpiration.visibility = android.view.View.VISIBLE
            tvCurrentExpiration.text = lebensmittel.ablaufdatum
        } else {
            layoutCurrentExpiration.visibility = android.view.View.GONE
        }

        // Setup input field hints based on transaction type
        tilQuantity.hint = if (isConsumption) "Verbrauchte Menge" else "Eingekaufte Menge"
        tilReason.hint = if (isConsumption) "Grund für Verbrauch (optional)" else "Einkaufsnotiz (optional)"

        // MHD-Feld nur bei Einkauf anzeigen
        tilMhd.visibility = if (isConsumption) android.view.View.GONE else android.view.View.VISIBLE

        // DatePicker für MHD-Feld einrichten
        if (!isConsumption) {
            setupDatePicker(etMhd, tilMhd)
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

    private fun observeMultiTenantData() {
        // Observe current household and update toolbar subtitle
        viewModel.currentHousehold.observe(this) { household ->
            household?.let {
                toolbar.subtitle = "📍 ${it.name}"
            }
        }

        // Observe storage locations for future use
        viewModel.storageLocations.observe(this) { locations ->
            // Storage locations loaded - could be used for filtering later
        }

        // Observe packages for future use
        viewModel.packages.observe(this) { packages ->
            // Packages loaded - could be used in add/edit dialogs
        }
    }
}
