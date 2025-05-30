package com.foodtrack.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.foodtrack.app.data.model.Category
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CategoryManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("categories", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "CategoryManager"
        private const val KEY_CATEGORIES = "categories_list"
        private const val KEY_NEXT_ID = "next_category_id"

        // Default categories
        private val DEFAULT_CATEGORIES = listOf(
            Category(1, "Obst"),
            Category(2, "Gemüse"),
            Category(3, "Fleisch"),
            Category(4, "Milchprodukte"),
            Category(5, "Getränke"),
            Category(6, "Süßwaren"),
            Category(7, "Tiefkühlkost"),
            Category(8, "Konserven")
        )
    }

    init {
        // Initialize with default categories if none exist
        val existingCategories = getCategories()
        Log.d(TAG, "Init - Found ${existingCategories.size} existing categories")

        if (existingCategories.isEmpty()) {
            Log.d(TAG, "No categories found, initializing with defaults")
            saveCategories(DEFAULT_CATEGORIES)
            prefs.edit().putInt(KEY_NEXT_ID, 9).apply()
            Log.d(TAG, "Initialized with ${DEFAULT_CATEGORIES.size} default categories")
        } else {
            Log.d(TAG, "Using existing categories")
            existingCategories.forEach { category ->
                Log.d(TAG, "Existing category - ID: ${category.id}, Name: ${category.name}")
            }
        }
    }

    fun getCategories(): List<Category> {
        val json = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getCategoryNames(): List<String> {
        return getCategories().map { it.name }
    }

    fun addCategory(name: String): Boolean {
        Log.d(TAG, "Adding category '$name'")
        if (name.isBlank()) {
            Log.d(TAG, "Category name is blank, returning false")
            return false
        }

        val categories = getCategories().toMutableList()
        Log.d(TAG, "Current categories count: ${categories.size}")

        // Check if category already exists
        if (categories.any { it.name.equals(name, ignoreCase = true) }) {
            Log.d(TAG, "Category '$name' already exists")
            return false
        }

        val nextId = prefs.getInt(KEY_NEXT_ID, 9)
        val newCategory = Category(nextId, name.trim())
        categories.add(newCategory)

        saveCategories(categories)
        prefs.edit().putInt(KEY_NEXT_ID, nextId + 1).apply()

        Log.d(TAG, "Successfully added category '${newCategory.name}' with ID ${newCategory.id}")
        return true
    }

    fun updateCategory(id: Int, newName: String): Boolean {
        if (newName.isBlank()) return false

        val categories = getCategories().toMutableList()
        val index = categories.indexOfFirst { it.id == id }

        if (index == -1) return false

        // Check if new name already exists (excluding current category)
        if (categories.any { it.id != id && it.name.equals(newName, ignoreCase = true) }) {
            return false
        }

        categories[index] = categories[index].copy(name = newName.trim())
        saveCategories(categories)

        return true
    }

    fun deleteCategory(id: Int): Boolean {
        val categories = getCategories().toMutableList()
        val removed = categories.removeAll { it.id == id }

        if (removed) {
            saveCategories(categories)
        }

        return removed
    }

    private fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_CATEGORIES, json).apply()
    }
}
