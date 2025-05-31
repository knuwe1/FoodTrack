package com.foodtrack.app.ui.categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.local.CategoryManager
import com.foodtrack.app.data.model.Category
import com.foodtrack.app.data.model.CategoryWithCount
import com.foodtrack.app.ui.lebensmittel.LebensmittelViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CategoryManagementActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CategoryManagement"
    }

    private lateinit var categoryManager: CategoryManager
    private val lebensmittelViewModel: LebensmittelViewModel by viewModels()
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerViewCategories: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var tvCategoryCount: TextView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var fabAddCategory: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_category_management_modern)

            categoryManager = CategoryManager(this)

            initViews()
            setupToolbar()
            setupRecyclerView()
            setupClickListeners()
            observeViewModel()
            loadCategories()

            // Load food items to calculate category counts
            lebensmittelViewModel.fetchLebensmittel()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerViewCategories = findViewById(R.id.recyclerViewCategories)
        tvCategoryCount = findViewById(R.id.tvCategoryCount)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        fabAddCategory = findViewById(R.id.fabAddCategory)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onEditClicked = { category -> showEditCategoryDialog(category) },
            onDeleteClicked = { category -> showDeleteCategoryDialog(category) },
            onItemClicked = { category ->
                // TODO: Navigate to category details or filter food items by category
                Toast.makeText(this, "Kategorie: ${category.name}", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoryManagementActivity)
            adapter = categoryAdapter
        }
    }

    private fun setupClickListeners() {
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun observeViewModel() {
        lebensmittelViewModel.lebensmittelList.observe(this) { lebensmittelList ->
            // When food items are loaded, update categories with counts
            loadCategoriesWithCounts(lebensmittelList ?: emptyList())
        }
    }

    private fun loadCategories() {
        // This method now just triggers the initial load
        // The actual UI update happens in loadCategoriesWithCounts
        val categories = categoryManager.getCategories()

        // Update category count in header
        tvCategoryCount.text = if (categories.size == 1) {
            "1 Kategorie"
        } else {
            "${categories.size} Kategorien"
        }
    }

    private fun loadCategoriesWithCounts(lebensmittelList: List<com.foodtrack.app.data.model.Lebensmittel>) {
        try {
            val categories = categoryManager.getCategories()

            // Debug logging
            Log.d(TAG, "Loaded ${categories.size} categories")
            Log.d(TAG, "Loaded ${lebensmittelList.size} food items")

            // Calculate item count for each category
            val categoriesWithCount = categories.map { category ->
                val itemCount = lebensmittelList.count { lebensmittel ->
                    lebensmittel.kategorie?.equals(category.name, ignoreCase = true) == true
                }
                Log.d(TAG, "Category '${category.name}' has $itemCount items")
                CategoryWithCount(category, itemCount)
            }

            // Update UI based on categories
            if (categoriesWithCount.isEmpty()) {
                recyclerViewCategories.visibility = View.GONE
                emptyStateLayout.visibility = View.VISIBLE
            } else {
                recyclerViewCategories.visibility = View.VISIBLE
                emptyStateLayout.visibility = View.GONE
                categoryAdapter.submitList(categoriesWithCount)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading categories: ${e.message}", Toast.LENGTH_LONG).show()

            // Show error state
            recyclerViewCategories.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        }
    }



    private fun showAddCategoryDialog() {
        showCategoryDialog(null)
    }

    private fun showEditCategoryDialog(category: Category) {
        showCategoryDialog(category)
    }

    private fun showCategoryDialog(category: Category?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_category, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tilCategoryName = dialogView.findViewById<TextInputLayout>(R.id.tilCategoryName)
        val etCategoryName = dialogView.findViewById<TextInputEditText>(R.id.etCategoryName)
        val chipGroupPredefined = dialogView.findViewById<ChipGroup>(R.id.chipGroupPredefined)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        // Setup dialog for add or edit mode
        val isEditMode = category != null
        tvDialogTitle.text = if (isEditMode) "Kategorie bearbeiten" else "Kategorie hinzufügen"
        btnSave.text = if (isEditMode) "Aktualisieren" else "Hinzufügen"

        if (isEditMode) {
            etCategoryName.setText(category!!.name)
            chipGroupPredefined.visibility = View.GONE
        }

        // Setup predefined category chips
        setupPredefinedChips(chipGroupPredefined, etCategoryName)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etCategoryName.text.toString().trim()

            if (name.isEmpty()) {
                tilCategoryName.error = "Name ist erforderlich"
                return@setOnClickListener
            }

            tilCategoryName.error = null

            val success = if (isEditMode) {
                categoryManager.updateCategory(category!!.id, name)
            } else {
                categoryManager.addCategory(name)
            }

            if (success) {
                val message = if (isEditMode) "Kategorie aktualisiert" else "Kategorie hinzugefügt"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                loadCategories()
                // Reload food items to update counts
                lebensmittelViewModel.fetchLebensmittel()
                dialog.dismiss()
            } else {
                val errorMessage = if (isEditMode) "Fehler beim Aktualisieren" else "Kategorie existiert bereits"
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun setupPredefinedChips(chipGroup: ChipGroup, etCategoryName: TextInputEditText) {
        val predefinedCategories = listOf("Obst", "Gemüse", "Milchprodukte", "Fleisch", "Getränke", "Snacks")

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener {
                val categoryName = chip.text.toString()
                etCategoryName.setText(categoryName)
            }
        }
    }

    private fun showDeleteCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirmation, null)
        val tvDeleteMessage = dialogView.findViewById<TextView>(R.id.tvDeleteMessage)

        tvDeleteMessage.text = "Möchten Sie die Kategorie '${category.name}' wirklich löschen?"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            if (categoryManager.deleteCategory(category.id)) {
                Toast.makeText(this, "Kategorie gelöscht", Toast.LENGTH_SHORT).show()
                loadCategories()
                // Reload food items to update counts
                lebensmittelViewModel.fetchLebensmittel()
            } else {
                Toast.makeText(this, "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
