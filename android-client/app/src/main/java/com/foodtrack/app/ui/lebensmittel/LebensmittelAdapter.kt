package com.foodtrack.app.ui.lebensmittel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.data.model.StorageLocation
import com.foodtrack.app.data.model.Package as FoodPackage
import com.foodtrack.app.utils.ExpirationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class LebensmittelAdapter(
    private val onItemClicked: (Lebensmittel) -> Unit,
    private val onPurchaseClicked: (Lebensmittel) -> Unit,
    private val onConsumeClicked: (Lebensmittel) -> Unit,
    private val getStorageLocationById: (Int) -> StorageLocation? = { null },
    private val getPackageById: (Int) -> FoodPackage? = { null }
) : ListAdapter<Lebensmittel, LebensmittelAdapter.LebensmittelViewHolder>(LebensmittelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LebensmittelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lebensmittel_modern, parent, false)
        return LebensmittelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LebensmittelViewHolder, position: Int) {
        val lebensmittel = getItem(position)
        holder.bind(lebensmittel)
        holder.itemView.setOnClickListener {
            onItemClicked(lebensmittel)
        }
    }

    inner class LebensmittelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryIndicator: View = itemView.findViewById(R.id.categoryIndicator)
        private val nameTextView: TextView = itemView.findViewById(R.id.tvItemName)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val quantityUnitTextView: TextView = itemView.findViewById(R.id.tvItemQuantityUnit)
        private val ablaufdatumTextView: TextView = itemView.findViewById(R.id.tvItemAblaufdatum)
        private val categoryTextView: TextView = itemView.findViewById(R.id.tvItemCategory)
        private val lowStockWarning: LinearLayout = itemView.findViewById(R.id.lowStockWarning)
        private val lowStockWarningText: TextView = itemView.findViewById(R.id.tvLowStockWarning)
        private val btnPurchase: MaterialButton = itemView.findViewById(R.id.btnPurchase)
        private val btnConsume: MaterialButton = itemView.findViewById(R.id.btnConsume)

        // Multi-Tenant UI elements
        private val layoutStorageLocation: LinearLayout = itemView.findViewById(R.id.layoutStorageLocation)
        private val tvStorageLocation: TextView = itemView.findViewById(R.id.tvStorageLocation)
        private val layoutPackageInfo: LinearLayout = itemView.findViewById(R.id.layoutPackageInfo)
        private val tvPackageInfo: TextView = itemView.findViewById(R.id.tvPackageInfo)

        fun bind(lebensmittel: Lebensmittel) {
            // Set product name
            nameTextView.text = lebensmittel.name

            // Set category color indicator
            val categoryColor = getCategoryColor(lebensmittel.kategorie)
            categoryIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, categoryColor))

            // Set expiration status chip
            val expirationStatus = ExpirationUtils.getExpirationStatus(lebensmittel.ablaufdatum)
            setupStatusChip(expirationStatus)

            // Set quantity and unit
            val quantityText = when {
                lebensmittel.menge != null && lebensmittel.menge > 0 -> "${lebensmittel.menge}"
                else -> "0"
            }
            val unitText = lebensmittel.einheit?.takeIf { it.isNotBlank() } ?: ""
            quantityUnitTextView.text = if (unitText.isNotEmpty()) "$quantityText $unitText" else quantityText

            // Set expiration date
            ablaufdatumTextView.text = ExpirationUtils.formatExpirationText(lebensmittel.ablaufdatum)

            // Set category
            categoryTextView.text = lebensmittel.kategorie ?: "Keine Kategorie"

            // Handle low stock warning
            setupLowStockWarning(lebensmittel)

            // Setup Multi-Tenant information
            setupStorageLocationInfo(lebensmittel)
            setupPackageInfo(lebensmittel)

            // Setup button click listeners
            btnPurchase.setOnClickListener { onPurchaseClicked(lebensmittel) }
            btnConsume.setOnClickListener { onConsumeClicked(lebensmittel) }

            // Setup card click listener
            itemView.setOnClickListener { onItemClicked(lebensmittel) }
        }

        private fun setupStatusChip(status: ExpirationUtils.ExpirationStatus) {
            when (status) {
                ExpirationUtils.ExpirationStatus.FRESH -> {
                    statusChip.text = "Frisch"
                    statusChip.setChipBackgroundColorResource(R.color.food_status_fresh_container)
                    statusChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.food_status_fresh))
                }
                ExpirationUtils.ExpirationStatus.EXPIRING_SOON -> {
                    statusChip.text = "Läuft bald ab"
                    statusChip.setChipBackgroundColorResource(R.color.food_status_expiring_soon_container)
                    statusChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.food_status_expiring_soon))
                }
                ExpirationUtils.ExpirationStatus.EXPIRED -> {
                    statusChip.text = "Abgelaufen"
                    statusChip.setChipBackgroundColorResource(R.color.food_status_expired_container)
                    statusChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.food_status_expired))
                }
                ExpirationUtils.ExpirationStatus.NO_DATE -> {
                    statusChip.text = "Kein Datum"
                    statusChip.setChipBackgroundColorResource(R.color.md_theme_light_surfaceVariant)
                    statusChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_onSurfaceVariant))
                }
            }
        }

        private fun setupLowStockWarning(lebensmittel: Lebensmittel) {
            if (lebensmittel.isBelowMinimum()) {
                val shortage = lebensmittel.getMinimumShortage()
                lowStockWarningText.text = "Niedriger Bestand (${shortage} fehlen)"
                lowStockWarning.visibility = View.VISIBLE
            } else {
                lowStockWarning.visibility = View.GONE
            }
        }

        private fun getCategoryColor(category: String?): Int {
            return when (category?.lowercase()) {
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

        private fun setupStorageLocationInfo(lebensmittel: Lebensmittel) {
            val storageLocationId = lebensmittel.storageLocationId
            if (storageLocationId != null) {
                val storageLocation = getStorageLocationById(storageLocationId)
                if (storageLocation != null) {
                    tvStorageLocation.text = storageLocation.name
                    layoutStorageLocation.visibility = View.VISIBLE
                } else {
                    layoutStorageLocation.visibility = View.GONE
                }
            } else {
                layoutStorageLocation.visibility = View.GONE
            }
        }

        private fun setupPackageInfo(lebensmittel: Lebensmittel) {
            val packageId = lebensmittel.packageId
            if (packageId != null) {
                val packageInfo = getPackageById(packageId)
                if (packageInfo != null) {
                    val packageText = "${lebensmittel.packageCount}x ${packageInfo.name}"
                    tvPackageInfo.text = packageText
                    layoutPackageInfo.visibility = View.VISIBLE
                } else {
                    layoutPackageInfo.visibility = View.GONE
                }
            } else {
                layoutPackageInfo.visibility = View.GONE
            }
        }

    }
}

class LebensmittelDiffCallback : DiffUtil.ItemCallback<Lebensmittel>() {
    override fun areItemsTheSame(oldItem: Lebensmittel, newItem: Lebensmittel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Lebensmittel, newItem: Lebensmittel): Boolean {
        return oldItem == newItem
    }
}
