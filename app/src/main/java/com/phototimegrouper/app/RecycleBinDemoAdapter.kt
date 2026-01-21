package com.phototimegrouper.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 回收站预览版适配器：简单的分组标题 + 列表项
 */
class RecycleBinDemoAdapter(
    private var items: List<RecycleBinDemoItem>,
    private val onItemClick: (RecycleBinDemoItem.Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RecycleBinDemoItem.Header -> TYPE_HEADER
            is RecycleBinDemoItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_recycle_bin_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_recycle_bin_photo, parent, false)
                ItemViewHolder(view, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when {
            holder is HeaderViewHolder && item is RecycleBinDemoItem.Header -> holder.bind(item)
            holder is ItemViewHolder && item is RecycleBinDemoItem.Item -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<RecycleBinDemoItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleText: TextView = view.findViewById(R.id.headerTitleTextView)

        fun bind(header: RecycleBinDemoItem.Header) {
            titleText.text = header.title
        }
    }

    class ItemViewHolder(
        view: View,
        private val onItemClick: (RecycleBinDemoItem.Item) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.nameTextView)
        private val infoText: TextView = view.findViewById(R.id.infoTextView)
        private val deletedText: TextView = view.findViewById(R.id.deletedInfoTextView)

        fun bind(item: RecycleBinDemoItem.Item) {
            nameText.text = item.displayName
            infoText.text = "${item.sizeText} · ${item.resolutionText} · ${item.formatText}"
            deletedText.text = "将于 ${item.autoDeleteDate} 自动删除"

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

sealed class RecycleBinDemoItem {
    data class Header(val title: String) : RecycleBinDemoItem()
    data class Item(
        val id: Long,
        val uri: String,
        val displayName: String,
        val sizeText: String,
        val resolutionText: String,
        val formatText: String,
        val deletedAtText: String,
        val autoDeleteDate: String
    ) : RecycleBinDemoItem()
}
