package com.phototimegrouper.app.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.phototimegrouper.app.PhotoItem
import com.phototimegrouper.app.SizeFilter
import com.phototimegrouper.app.SortOrder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

data class FilterChipItem(
    val id: String,
    val label: String,
    val type: FilterChipType
)

enum class FilterChipType {
    /** 用于“筛选与排序”入口，点击打开筛选面板，不显示关闭图标 */
    OPEN_FILTER,
    MEDIA_TYPE,
    DATE_RANGE,
    SIZE,
    SORT_ORDER
}

class FilterChipAdapter(
    private val chips: MutableList<FilterChipItem>,
    private val onRemoveChip: (FilterChipItem) -> Unit,
    private val onOpenFilterClick: (() -> Unit)? = null
) : RecyclerView.Adapter<FilterChipAdapter.ChipViewHolder>() {

    class ChipViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val chip = Chip(parent.context)
        chip.isCloseIconVisible = true
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = 1f
        return ChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val item = chips[position]
        holder.chip.text = item.label
        val isOpenFilter = item.type == FilterChipType.OPEN_FILTER
        holder.chip.isCloseIconVisible = !isOpenFilter
        if (isOpenFilter) {
            holder.chip.setOnClickListener { onOpenFilterClick?.invoke() }
            holder.chip.setOnCloseIconClickListener(null)
        } else {
            holder.chip.setOnClickListener(null)
            holder.chip.setOnCloseIconClickListener { onRemoveChip(item) }
        }
    }

    override fun getItemCount() = chips.size

    fun updateChips(newChips: List<FilterChipItem>) {
        chips.clear()
        chips.addAll(newChips)
        notifyDataSetChanged()
    }
}
