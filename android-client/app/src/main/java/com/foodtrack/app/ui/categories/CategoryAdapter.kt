package com.foodtrack.app.ui.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Category
import com.foodtrack.app.data.model.CategoryWithCount
import com.google.android.material.button.MaterialButton

class CategoryAdapter(
    private val onEditClicked: (Category) -> Unit,
    private val onDeleteClicked: (Category) -> Unit,
    private val onItemClicked: (Category) -> Unit
) : ListAdapter<CategoryWithCount, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_modern, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryColorIndicator: View = itemView.findViewById(R.id.categoryColorIndicator)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvItemCount: TextView = itemView.findViewById(R.id.tvItemCount)
        private val btnEditCategory: MaterialButton = itemView.findViewById(R.id.btnEditCategory)
        private val btnDeleteCategory: MaterialButton = itemView.findViewById(R.id.btnDeleteCategory)

        fun bind(categoryWithCount: CategoryWithCount) {
            val category = categoryWithCount.category
            val itemCount = categoryWithCount.itemCount

            tvCategoryName.text = category.name

            // Set item count with proper pluralization
            tvItemCount.text = when (itemCount) {
                0 -> "Keine Lebensmittel"
                1 -> "1 Lebensmittel"
                else -> "$itemCount Lebensmittel"
            }

            // Set category color based on name
            val categoryColor = getCategoryColor(category.name)
            categoryColorIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, categoryColor))

            // Set category icon based on name
            val categoryIcon = getCategoryIcon(category.name)
            ivCategoryIcon.setImageResource(categoryIcon)

            // Setup click listeners
            btnEditCategory.setOnClickListener { onEditClicked(category) }
            btnDeleteCategory.setOnClickListener { onDeleteClicked(category) }
            itemView.setOnClickListener { onItemClicked(category) }
        }

        private fun getCategoryColor(categoryName: String): Int {
            return when (categoryName.lowercase()) {
                "obst", "früchte" -> R.color.category_fruits
                "gemüse" -> R.color.category_vegetables
                "milchprodukte", "milch" -> R.color.category_dairy
                "fleisch", "wurst" -> R.color.category_meat
                "getreide", "brot" -> R.color.category_grains
                "getränke" -> R.color.category_beverages
                "süßwaren", "snacks" -> R.color.category_snacks
                "tiefkühl" -> R.color.category_frozen
                "konserven" -> R.color.category_canned
                "gewürze" -> R.color.category_spices
                else -> R.color.category_default
            }
        }

        private fun getCategoryIcon(categoryName: String): Int {
            return when (categoryName.lowercase()) {
                "obst", "früchte" -> R.drawable.ic_food_24
                "gemüse" -> R.drawable.ic_food_24
                "milchprodukte", "milch" -> R.drawable.ic_food_24
                "fleisch", "wurst" -> R.drawable.ic_food_24
                "getreide", "brot" -> R.drawable.ic_food_24
                "getränke" -> R.drawable.ic_food_24
                "süßwaren", "snacks" -> R.drawable.ic_food_24
                "tiefkühl" -> R.drawable.ic_food_24
                "konserven" -> R.drawable.ic_food_24
                "gewürze" -> R.drawable.ic_food_24
                else -> R.drawable.ic_category_24
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryWithCount>() {
        override fun areItemsTheSame(oldItem: CategoryWithCount, newItem: CategoryWithCount): Boolean {
            return oldItem.category.id == newItem.category.id
        }

        override fun areContentsTheSame(oldItem: CategoryWithCount, newItem: CategoryWithCount): Boolean {
            return oldItem == newItem
        }
    }
}
