package com.foodtrack.app.ui.transactions

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R

class SimpleTransactionHistoryActivity : AppCompatActivity() {

    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: SimpleTransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_transaction_history)

        setupViews()
        observeViewModel()
        loadData()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.rvTransactions)
        transactionAdapter = SimpleTransactionAdapter()

        recyclerView.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@SimpleTransactionHistoryActivity)
        }
    }

    private fun observeViewModel() {
        transactionViewModel.transactions.observe(this) { transactions ->
            transactionAdapter.submitList(transactions ?: emptyList())
        }

        transactionViewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, "Fehler: $error", Toast.LENGTH_LONG).show()
                transactionViewModel.clearError()
            }
        }
    }

    private fun loadData() {
        transactionViewModel.fetchTransactions()
    }
}
