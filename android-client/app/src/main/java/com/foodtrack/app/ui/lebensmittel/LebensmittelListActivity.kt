package com.foodtrack.app.ui.lebensmittel

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LebensmittelListActivity : AppCompatActivity() {

    private val viewModel: LebensmittelViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var lebensmittelAdapter: LebensmittelAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyListMessage: TextView
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lebensmittel_list)

        recyclerView = findViewById(R.id.rvLebensmittelList)
        progressBar = findViewById(R.id.pbListLoading)
        emptyListMessage = findViewById(R.id.tvEmptyListMessage)
        fabAdd = findViewById(R.id.fabAddLebensmittel)

        setupRecyclerView()
        observeViewModel()

        // Initial fetch is done in ViewModel's init block,
        // but you might want to add a swipe-to-refresh or a button to call this explicitly later.
        // viewModel.fetchLebensmittel() // Uncomment if not fetching in init or need refresh logic here

        fabAdd.setOnClickListener {
            val intent = Intent(this, com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when the activity resumes
        viewModel.fetchLebensmittel()
    }

    private fun setupRecyclerView() {
        lebensmittelAdapter = LebensmittelAdapter { lebensmittel ->
            // Handle item click - navigate to detail/edit screen
            val intent = Intent(this, com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity::class.java).apply {
                putExtra(com.foodtrack.app.ui.addedit.AddEditLebensmittelActivity.EXTRA_LEBENSMIITEL_ID, lebensmittel.id)
            }
            startActivity(intent)
        }
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

    private fun observeViewModel() {
        viewModel.lebensmittelList.observe(this) { _ ->
            // Only submit list if not loading and no error
            if (viewModel.isLoading.value == false && viewModel.errorMessage.value == null) {
                lebensmittelAdapter.submitList(it ?: emptyList())
            }
            updateUiVisibility()
        }
        viewModel.isLoading.observe(this) { _ -> updateUiVisibility() }
        viewModel.errorMessage.observe(this) { _ -> updateUiVisibility() }
    }
}
