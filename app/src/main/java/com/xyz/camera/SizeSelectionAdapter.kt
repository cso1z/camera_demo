package com.xyz.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SizeItem(
    val id: String,
    val width: Int,
    val height: Int
) {
    override fun toString(): String {
        val ratio = String.format("%.2f", width.toFloat() / height.toFloat())
        return "$width:$height($ratio)"
    }

    fun toString1():String{
        val ratio = String.format("%.2f", width.toFloat() / height.toFloat())
        return "$width:$height\n($ratio)"
    }
}

class SizeSelectionAdapter(
    private var sizeList: List<SizeItem>,
    private val onItemClick: (SizeItem) -> Unit
) : RecyclerView.Adapter<SizeSelectionAdapter.SizeViewHolder>() {

    class SizeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSize: TextView = itemView.findViewById(R.id.tvSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_size_selection, parent, false)
        return SizeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SizeViewHolder, position: Int) {
        val sizeItem = sizeList[position]
        holder.tvSize.text = sizeItem.toString1()
        holder.itemView.setOnClickListener {
            onItemClick(sizeItem)
        }
    }

    override fun getItemCount(): Int = sizeList.size

    fun updateData(newList: List<SizeItem>) {
        sizeList = newList
        notifyDataSetChanged()
    }
}
