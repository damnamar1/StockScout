package com.example.stockscout.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockscout.R
import com.example.stockscout.databinding.ItemInventoryBinding
import com.example.stockscout.domain.model.Item

class ItemAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, ItemAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val ctx = binding.root.context
            binding.tvItemCode.text = item.itemCode
            binding.tvItemName.text = item.name
            binding.tvUom.text = item.unitOfMeasure
            binding.tvQty.text = item.onHandQuantity.toString()
            binding.tvAliasCount.text = ctx.getString(R.string.alias_count, item.aliases.size)

            // Out-of-stock visual: red quantity. Falls back to default text color otherwise.
            val qtyColor = if (item.onHandQuantity == 0) R.color.error_red else R.color.primary_text
            binding.tvQty.setTextColor(ContextCompat.getColor(ctx, qtyColor))

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) =
            oldItem.itemCode == newItem.itemCode

        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
