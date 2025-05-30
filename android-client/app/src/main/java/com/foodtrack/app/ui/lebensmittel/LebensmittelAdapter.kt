package com.foodtrack.app.ui.lebensmittel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Lebensmittel
import com.foodtrack.app.utils.ExpirationUtils
import com.google.android.material.button.MaterialButton

class LebensmittelAdapter(
    private val onItemClicked: (Lebensmittel) -> Unit,
    private val onPurchaseClicked: (Lebensmittel) -> Unit,
    private val onConsumeClicked: (Lebensmittel) -> Unit
) : ListAdapter<Lebensmittel, LebensmittelAdapter.LebensmittelViewHolder>(LebensmittelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LebensmittelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lebensmittel, parent, false)
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
        private val nameTextView: TextView = itemView.findViewById(R.id.tvItemName)
        private val quantityUnitTextView: TextView = itemView.findViewById(R.id.tvItemQuantityUnit)
        private val ablaufdatumTextView: TextView = itemView.findViewById(R.id.tvItemAblaufdatum)
        private val categoryTextView: TextView = itemView.findViewById(R.id.tvItemCategory)
        private val btnPurchase: MaterialButton = itemView.findViewById(R.id.btnPurchase)
        private val btnConsume: MaterialButton = itemView.findViewById(R.id.btnConsume)

        fun bind(lebensmittel: Lebensmittel) {
            nameTextView.text = lebensmittel.name

            // Debug logging
            android.util.Log.d("LebensmittelAdapter", "Binding ${lebensmittel.name}: menge=${lebensmittel.menge}, einheit=${lebensmittel.einheit}")

            // Better quantity and unit handling
            val quantityText = when {
                lebensmittel.menge != null && lebensmittel.menge > 0 -> lebensmittel.menge.toString()
                else -> ""
            }
            val unitText = lebensmittel.einheit?.takeIf { it.isNotBlank() } ?: ""

            val baseQuantityText = when {
                quantityText.isNotEmpty() && unitText.isNotEmpty() -> "Menge: $quantityText $unitText"
                quantityText.isNotEmpty() -> "Menge: $quantityText"
                unitText.isNotEmpty() -> "Einheit: $unitText"
                else -> "Menge: N/A"
            }

            // Prüfe Mindestmenge-Warnung
            val finalQuantityText = if (lebensmittel.isBelowMinimum()) {
                val shortage = lebensmittel.getMinimumShortage()
                "$baseQuantityText ⚠️ (${shortage} fehlen)"
            } else {
                baseQuantityText
            }

            quantityUnitTextView.text = finalQuantityText

            // Setze Textfarbe basierend auf Mindestmenge
            val textColor = if (lebensmittel.isBelowMinimum()) {
                android.graphics.Color.parseColor("#FF5722") // Orange-rot für Warnung
            } else {
                android.graphics.Color.parseColor("#757575") // Standard grau
            }
            quantityUnitTextView.setTextColor(textColor)

            // Enhanced expiration date display with status
            val expirationStatus = ExpirationUtils.getExpirationStatus(lebensmittel.ablaufdatum)
            ablaufdatumTextView.text = ExpirationUtils.formatExpirationText(lebensmittel.ablaufdatum)
            ablaufdatumTextView.setTextColor(ExpirationUtils.getStatusColor(expirationStatus))

            categoryTextView.text = if (lebensmittel.kategorie != null) "Kategorie: ${lebensmittel.kategorie}" else "Kategorie: N/A"

            // Handle visibility for optional fields
            categoryTextView.visibility = if (lebensmittel.kategorie.isNullOrEmpty()) View.GONE else View.VISIBLE
            ablaufdatumTextView.visibility = if (lebensmittel.ablaufdatum.isNullOrEmpty()) View.GONE else View.VISIBLE
            quantityUnitTextView.visibility = View.VISIBLE // Always show, even if N/A

            // Setup button click listeners
            btnPurchase.setOnClickListener { onPurchaseClicked(lebensmittel) }
            btnConsume.setOnClickListener { onConsumeClicked(lebensmittel) }
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
