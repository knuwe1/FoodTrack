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

class LebensmittelAdapter(private val onItemClicked: (Lebensmittel) -> Unit) :
    ListAdapter<Lebensmittel, LebensmittelAdapter.LebensmittelViewHolder>(LebensmittelDiffCallback()) {

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

    class LebensmittelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvItemName)
        private val quantityUnitTextView: TextView = itemView.findViewById(R.id.tvItemQuantityUnit)
        private val mhdTextView: TextView = itemView.findViewById(R.id.tvItemMhd)
        private val categoryTextView: TextView = itemView.findViewById(R.id.tvItemCategory)
        private val lagerortTextView: TextView = itemView.findViewById(R.id.tvItemLagerort)

        fun bind(lebensmittel: Lebensmittel) {
            nameTextView.text = lebensmittel.name
            val quantityText = lebensmittel.menge?.toString() ?: ""
            val unitText = lebensmittel.einheit ?: ""
            quantityUnitTextView.text = "Menge: $quantityText $unitText".trim()

            mhdTextView.text = if (lebensmittel.mhd != null) "MHD: ${lebensmittel.mhd}" else "MHD: N/A"
            categoryTextView.text = if (lebensmittel.kategorie != null) "Kategorie: ${lebensmittel.kategorie}" else "Kategorie: N/A"
            lagerortTextView.text = if (lebensmittel.lagerort != null) "Lagerort: ${lebensmittel.lagerort}" else "Lagerort: N/A"

            // Handle visibility for optional fields
            categoryTextView.visibility = if (lebensmittel.kategorie.isNullOrEmpty()) View.GONE else View.VISIBLE
            lagerortTextView.visibility = if (lebensmittel.lagerort.isNullOrEmpty()) View.GONE else View.VISIBLE
            mhdTextView.visibility = if (lebensmittel.mhd.isNullOrEmpty()) View.GONE else View.VISIBLE
            quantityUnitTextView.visibility = if (lebensmittel.menge == null && lebensmittel.einheit.isNullOrEmpty()) View.GONE else View.VISIBLE

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
