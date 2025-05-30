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
import com.foodtrack.app.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val lebensmittelNames: Map<Int, String> = emptyMap()
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction, lebensmittelNames)
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivTransactionType: ImageView = itemView.findViewById(R.id.ivTransactionType)
        private val tvLebensmittelName: TextView = itemView.findViewById(R.id.tvLebensmittelName)
        private val tvQuantityChange: TextView = itemView.findViewById(R.id.tvQuantityChange)
        private val tvTransactionType: TextView = itemView.findViewById(R.id.tvTransactionType)
        private val tvQuantityBeforeAfter: TextView = itemView.findViewById(R.id.tvQuantityBeforeAfter)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)

        fun bind(transaction: Transaction, lebensmittelNames: Map<Int, String>) {
            try {
                val context = itemView.context

                // Set lebensmittel name
                tvLebensmittelName.text = lebensmittelNames[transaction.lebensmittelId]
                    ?: "Lebensmittel #${transaction.lebensmittelId}"

            // Set quantity change with sign and color
            val quantityChangeText = when {
                transaction.quantityChange > 0 -> "+${transaction.quantityChange}"
                else -> transaction.quantityChange.toString()
            }
            tvQuantityChange.text = quantityChangeText

            // Set icon and colors based on transaction type
            when (transaction.transactionType) {
                TransactionType.PURCHASE -> {
                    ivTransactionType.setImageResource(R.drawable.ic_shopping_cart)
                    ivTransactionType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    tvQuantityChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                }
                TransactionType.CONSUMPTION -> {
                    ivTransactionType.setImageResource(R.drawable.ic_restaurant)
                    ivTransactionType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                    tvQuantityChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                }
                TransactionType.ADJUSTMENT -> {
                    ivTransactionType.setImageResource(R.drawable.ic_edit)
                    ivTransactionType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                    tvQuantityChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                }
                TransactionType.EXPIRED -> {
                    ivTransactionType.setImageResource(R.drawable.ic_warning)
                    ivTransactionType.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    tvQuantityChange.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
            }

            // Set transaction type and reason
            val typeText = when (transaction.transactionType) {
                TransactionType.PURCHASE -> "Einkauf"
                TransactionType.CONSUMPTION -> "Verbrauch"
                TransactionType.ADJUSTMENT -> "Korrektur"
                TransactionType.EXPIRED -> "Ablauf"
            }

            tvTransactionType.text = if (transaction.reason.isNullOrBlank()) {
                typeText
            } else {
                "$typeText • ${transaction.reason}"
            }

            // Set quantity before/after
            val beforeText = transaction.quantityBefore?.toString() ?: "?"
            val afterText = transaction.quantityAfter?.toString() ?: "?"
            tvQuantityBeforeAfter.text = "$beforeText → $afterText"

            // Format and set date/time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
                val date = inputFormat.parse(transaction.createdAt)
                tvDateTime.text = date?.let { outputFormat.format(it) } ?: transaction.createdAt
            } catch (e: Exception) {
                // Fallback to raw string if parsing fails
                tvDateTime.text = transaction.createdAt
            }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback values
                tvLebensmittelName.text = "Lebensmittel #${transaction.lebensmittelId}"
                tvQuantityChange.text = transaction.quantityChange.toString()
                tvTransactionType.text = transaction.transactionType.name
                tvQuantityBeforeAfter.text = "${transaction.quantityBefore ?: "?"} → ${transaction.quantityAfter ?: "?"}"
                tvDateTime.text = transaction.createdAt
            }
        }
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
