package com.phototimegrouper.app

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

/**
 * 详情列表模式适配器
 * 显示照片的缩略图、名称、日期和大小等信息
 */
class PhotoDetailListAdapter(
    private val context: Context,
    private val photoList: List<PhotoItem>,
    private val onPhotoClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<PhotoDetailListAdapter.PhotoDetailListViewHolder>() {

    class PhotoDetailListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnailImageView: ImageView = view.findViewById(R.id.photoThumbnailImageView)
        val nameTextView: TextView = view.findViewById(R.id.photoNameTextView)
        val dateTextView: TextView = view.findViewById(R.id.photoDateTextView)
        val sizeTextView: TextView = view.findViewById(R.id.photoSizeTextView)
        val formatTextView: TextView = view.findViewById(R.id.photoFormatTextView)
        val videoPlayIcon: ImageView = view.findViewById(R.id.videoPlayIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoDetailListViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo_detail_list, parent, false)
        return PhotoDetailListViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoDetailListViewHolder, position: Int) {
        val photo = photoList.getOrNull(position) ?: return
        
        // 设置照片名称
        holder.nameTextView.text = photo.displayName
        
        // 设置日期
        holder.dateTextView.text = DateFormatter.formatDateTime(photo.dateModified)
        
        // 设置文件大小和分辨率（使用标准乘号，避免显示问题）
        val resolution = photo.getResolution().replace("×", "x") // 使用标准 x 避免字体问题
        val sizeText = if (photo.size > 0) {
            "${photo.getFormattedSize()} · $resolution"
        } else {
            resolution
        }
        holder.sizeTextView.text = sizeText
        
        // 设置文件格式（单独显示在右侧）
        holder.formatTextView.text = photo.getFormat()
        
        // 判断是否为视频
        val isVideo = photo.isVideo()
        
        // 显示/隐藏视频播放图标
        holder.videoPlayIcon.visibility = if (isVideo) View.VISIBLE else View.GONE
        
        // 使用 Glide 加载缩略图
        if (isVideo) {
            // 对于视频，加载视频的第一帧作为缩略图
            Glide.with(context)
                .asBitmap()
                .load(Uri.parse(photo.uri))
                .apply(
                    RequestOptions()
                        .placeholder(R.color.surface_variant)
                        .error(R.color.surface_variant)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .override(160, 160) // 缩略图尺寸
                )
                .into(holder.thumbnailImageView)
        } else {
            // 对于图片，正常加载
            Glide.with(context)
                .load(Uri.parse(photo.uri))
                .apply(
                    RequestOptions()
                        .placeholder(R.color.surface_variant)
                        .error(R.color.surface_variant)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .override(160, 160) // 缩略图尺寸
                )
                .into(holder.thumbnailImageView)
        }
        
        // 添加点击事件
        holder.itemView.setOnClickListener {
            onPhotoClick?.invoke(position)
        }
    }

    override fun getItemCount(): Int = photoList.size
}
