package com.foodtrack.app.ui.categories

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.foodtrack.app.R
import com.foodtrack.app.data.local.CategoryManager
import com.foodtrack.app.data.model.Category

class CategoryManagementActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CategoryManagement"
    }

    private lateinit var categoryManager: CategoryManager
    private lateinit var categoriesContainer: LinearLayout
    private lateinit var btnAddCategory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_category_management_full)

            categoryManager = CategoryManager(this)

            initViews()
            setupClickListeners()
            loadCategories()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        categoriesContainer = findViewById(R.id.categoriesContainer)
        btnAddCategory = findViewById(R.id.btnAddCategory)
    }

    private fun setupClickListeners() {
        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadCategories() {
        try {
            categoriesContainer.removeAllViews()

            val categories = categoryManager.getCategories()

            // Debug logging
            Log.d(TAG, "Loaded ${categories.size} categories")
            Toast.makeText(this, "Loaded ${categories.size} categories", Toast.LENGTH_SHORT).show()
            categories.forEach { category ->
                Log.d(TAG, "Category - ID: ${category.id}, Name: ${category.name}")
            }

            if (categories.isEmpty()) {
                val emptyText = TextView(this).apply {
                    text = "No categories found. Add some categories!"
                    setPadding(32)
                    textSize = 16f
                }
                categoriesContainer.addView(emptyText)
            } else {
                categories.forEach { category ->
                    addCategoryView(category)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorText = TextView(this).apply {
                text = "Error loading categories: ${e.message}"
                setPadding(32)
                textSize = 16f
            }
            categoriesContainer.addView(errorText)
        }
    }

    private fun addCategoryView(category: Category) {
        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 16, 32, 16)
        }

        val nameText = TextView(this).apply {
            text = category.name
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val editButton = Button(this).apply {
            text = "Edit"
            setOnClickListener { showEditCategoryDialog(category) }
        }

        val deleteButton = Button(this).apply {
            text = "Delete"
            setOnClickListener { showDeleteCategoryDialog(category) }
        }

        categoryLayout.addView(nameText)
        categoryLayout.addView(editButton)
        categoryLayout.addView(deleteButton)

        categoriesContainer.addView(categoryLayout)
    }

    private fun showAddCategoryDialog() {
        val editText = EditText(this).apply {
            hint = "Category name"
        }

        AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val name = editText.text.toString().trim()
                if (categoryManager.addCategory(name)) {
                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Failed to add category (may already exist)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val editText = EditText(this).apply {
            setText(category.name)
            hint = "Category name"
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Category")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (categoryManager.updateCategory(category.id, newName)) {
                    Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Failed to update category", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                if (categoryManager.deleteCategory(category.id)) {
                    Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Failed to delete category", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
