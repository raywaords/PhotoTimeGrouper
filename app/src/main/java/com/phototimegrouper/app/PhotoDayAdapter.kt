package com.phototimegrouper.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * 按日期分组的一项（与相册卡片同结构：缩略图 + 标题 + 数量）
 */
data class DayItem(
    val dateKey: String,
    val displayDate: String,
    val count: Int,
    val coverUri: String
)

/**
 * 照片按日期列表适配器，样式与相册一致，仅将文件夹名换为日期
 */
class PhotoDayAdapter(
    private var days: List<DayItem>,
    private val onDayClick: (DayItem) -> Unit
) : RecyclerView.Adapter<PhotoDayAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return DayViewHolder(view, onDayClick)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<DayItem>) {
        days = newDays
        notifyDataSetChanged()
    }

    class DayViewHolder(
        itemView: View,
        private val onDayClick: (DayItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val coverImageView: ImageView = itemView.findViewById(R.id.folderCoverImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.folderNameTextView)
        private val countTextView: TextView = itemView.findViewById(R.id.folderCountTextView)

        fun bind(day: DayItem) {
            nameTextView.text = day.displayDate
            countTextView.text = "${day.count} \u9879"

            if (day.coverUri.isNotEmpty()) {
                Glide.with(itemView)
                    .load(day.coverUri)
                    .centerCrop()
                    .into(coverImageView)
            } else {
                coverImageView.setImageDrawable(null)
            }

            itemView.setOnClickListener {
                onDayClick(day)
            }
        }
    }
}
