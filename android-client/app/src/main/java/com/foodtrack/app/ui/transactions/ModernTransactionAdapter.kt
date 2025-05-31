package com.foodtrack.app.ui.transactions

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
import com.foodtrack.app.data.model.Transaction
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class ModernTransactionAdapter(
    private val onItemClicked: (Transaction) -> Unit = {}
) : ListAdapter<Transaction, ModernTransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_modern, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val transactionTypeIndicator: View = itemView.findViewById(R.id.transactionTypeIndicator)
        private val ivTransactionIcon: ImageView = itemView.findViewById(R.id.ivTransactionIcon)
        private val tvFoodItemName: TextView = itemView.findViewById(R.id.tvFoodItemName)
        private val chipTransactionType: Chip = itemView.findViewById(R.id.chipTransactionType)
        private val tvQuantityChange: TextView = itemView.findViewById(R.id.tvQuantityChange)
        private val tvTransactionDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)

        fun bind(transaction: Transaction) {
            // Set food item name (placeholder for now - would need to resolve from lebensmittelId)
            tvFoodItemName.text = "Lebensmittel #${transaction.lebensmittelId}"

            // Set transaction type and styling
            val isPurchase = transaction.transactionType.toString().equals("PURCHASE", ignoreCase = true)

            if (isPurchase) {
                chipTransactionType.text = "Einkauf"
                chipTransactionType.setChipBackgroundColorResource(R.color.chip_purchase_background)
                chipTransactionType.setTextColor(ContextCompat.getColor(itemView.context, R.color.chip_purchase_text))

                ivTransactionIcon.setImageResource(R.drawable.ic_add_24)
                transactionTypeIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.transaction_purchase))

                tvQuantityChange.text = "+${transaction.quantityChange}"
                tvQuantityChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.transaction_purchase))
            } else {
                chipTransactionType.text = "Verbrauch"
                chipTransactionType.setChipBackgroundColorResource(R.color.chip_consumption_background)
                chipTransactionType.setTextColor(ContextCompat.getColor(itemView.context, R.color.chip_consumption_text))

                ivTransactionIcon.setImageResource(R.drawable.ic_remove_24)
                transactionTypeIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.transaction_consumption))

                tvQuantityChange.text = "-${Math.abs(transaction.quantityChange)}"
                tvQuantityChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.transaction_consumption))
            }

            // Set transaction date
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            try {
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(transaction.createdAt)
                tvTransactionDate.text = dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                tvTransactionDate.text = transaction.createdAt
            }

            // Set reason if available
            if (!transaction.reason.isNullOrBlank()) {
                tvReason.text = transaction.reason
                tvReason.visibility = View.VISIBLE
            } else {
                tvReason.visibility = View.GONE
            }

            // Set click listener
            itemView.setOnClickListener { onItemClicked(transaction) }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
