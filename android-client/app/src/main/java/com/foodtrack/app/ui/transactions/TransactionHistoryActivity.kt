package com.foodtrack.app.ui.transactions

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.model.TransactionType
import com.foodtrack.app.ui.lebensmittel.LebensmittelViewModel
import androidx.appcompat.widget.Toolbar

class TransactionHistoryActivity : AppCompatActivity() {

    private val transactionViewModel: TransactionViewModel by viewModels()
    private val lebensmittelViewModel: LebensmittelViewModel by viewModels()

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var spinnerLebensmittelFilter: Spinner
    private lateinit var spinnerTypeFilter: Spinner
    private lateinit var tvTotalTransactions: TextView
    private lateinit var tvTotalPurchases: TextView
    private lateinit var tvTotalConsumption: TextView

    private lateinit var lebensmittelAdapter: ArrayAdapter<String>
    private lateinit var typeAdapter: ArrayAdapter<String>

    private var lebensmittelList: List<Lebensmittel> = emptyList()
    private var lebensmittelNames: Map<Int, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupFilters()
        observeViewModels()

        // Load initial data
        try {
            lebensmittelViewModel.fetchLebensmittel()
            transactionViewModel.fetchTransactions()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Fehler beim Laden der Daten: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.rvTransactionHistory)
        progressBar = findViewById(R.id.pbHistoryLoading)
        emptyMessage = findViewById(R.id.tvEmptyHistoryMessage)
        spinnerLebensmittelFilter = findViewById(R.id.spinnerLebensmittelFilter)
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter)
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions)
        tvTotalPurchases = findViewById(R.id.tvTotalPurchases)
        tvTotalConsumption = findViewById(R.id.tvTotalConsumption)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(lebensmittelNames)
        recyclerView.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
        }
    }

    private fun setupFilters() {
        // Setup Lebensmittel filter
        lebensmittelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>())
        lebensmittelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLebensmittelFilter.adapter = lebensmittelAdapter

        spinnerLebensmittelFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = lebensmittelAdapter.getItem(position)
                val lebensmittelId = if (selectedItem == "Alle Lebensmittel") {
                    null
                } else {
                    lebensmittelList.find { it.name == selectedItem }?.id
                }
                transactionViewModel.fetchTransactions(lebensmittelId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Type filter
        val typeOptions = listOf("Alle Typen", "Einkauf", "Verbrauch", "Korrektur", "Ablauf")
        typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeFilter.adapter = typeAdapter

        spinnerTypeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // For now, we'll implement type filtering in a future update
                // The backend API supports it, but we need to modify the ViewModel
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModels() {
        // Observe Lebensmittel data
        lebensmittelViewModel.lebensmittelList.observe(this) { lebensmittelList ->
            try {
                this.lebensmittelList = lebensmittelList ?: emptyList()
                this.lebensmittelNames = this.lebensmittelList.associate { it.id to it.name }

                // Update adapter with new names
                transactionAdapter = TransactionAdapter(lebensmittelNames)
                recyclerView.adapter = transactionAdapter

                // Update filter options
                lebensmittelAdapter.clear()
                lebensmittelAdapter.add("Alle Lebensmittel")
                lebensmittelAdapter.addAll(this.lebensmittelList.map { it.name })
                lebensmittelAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Observe Transaction data
        transactionViewModel.transactions.observe(this) { transactions ->
            transactionAdapter.submitList(transactions ?: emptyList())
            updateStatistics(transactions ?: emptyList())
            updateUiVisibility()
        }

        transactionViewModel.isLoading.observe(this) { isLoading ->
            updateUiVisibility()
        }

        transactionViewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, "Fehler: $error", Toast.LENGTH_LONG).show()
                transactionViewModel.clearError()
            }
            updateUiVisibility()
        }
    }

    private fun updateStatistics(transactions: List<com.foodtrack.app.data.model.Transaction>) {
        val totalTransactions = transactions.size
        val totalPurchases = transactions.count { it.transactionType == TransactionType.PURCHASE }
        val totalConsumption = transactions.count { it.transactionType == TransactionType.CONSUMPTION }

        tvTotalTransactions.text = totalTransactions.toString()
        tvTotalPurchases.text = totalPurchases.toString()
        tvTotalConsumption.text = totalConsumption.toString()
    }

    private fun updateUiVisibility() {
        val isLoading = transactionViewModel.isLoading.value ?: false
        val transactions = transactionViewModel.transactions.value
        val hasError = transactionViewModel.errorMessage.value != null

        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        when {
            isLoading -> {
                recyclerView.visibility = View.GONE
                emptyMessage.visibility = View.GONE
            }
            hasError -> {
                recyclerView.visibility = View.GONE
                emptyMessage.visibility = View.VISIBLE
                emptyMessage.text = "Fehler beim Laden der Transaktionen"
            }
            transactions.isNullOrEmpty() -> {
                recyclerView.visibility = View.GONE
                emptyMessage.visibility = View.VISIBLE
                emptyMessage.text = "Keine Transaktionen gefunden"
            }
            else -> {
                recyclerView.visibility = View.VISIBLE
                emptyMessage.visibility = View.GONE
            }
        }
    }
}
