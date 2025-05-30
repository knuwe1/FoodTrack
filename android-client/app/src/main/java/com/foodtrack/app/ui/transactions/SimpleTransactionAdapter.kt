package com.foodtrack.app.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodtrack.app.R
import com.foodtrack.app.data.model.Transaction

class SimpleTransactionAdapter : ListAdapter<Transaction, SimpleTransactionAdapter.ViewHolder>(SimpleTransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInfo: TextView = itemView.findViewById(R.id.tvTransactionInfo)

        fun bind(transaction: Transaction) {
            val info = "ID: ${transaction.id} | " +
                    "Lebensmittel: ${transaction.lebensmittelId} | " +
                    "Typ: ${transaction.transactionType} | " +
                    "Menge: ${transaction.quantityChange} | " +
                    "Grund: ${transaction.reason ?: "N/A"}"

            tvInfo.text = info
        }
    }
}

class SimpleTransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}
