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

class PhotoAdapter(
    private val context: Context,
    private val photoList: List<PhotoItem>,
    private val onPhotoClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photoImageView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photoList.getOrNull(position) ?: return
        
        // 使用工具类格式化日期
        holder.timestampTextView.text = DateFormatter.formatDateTime(photo.dateModified)

        // 使用 Glide 加载图片，添加错误处理和缓存策略
        Glide.with(context)
            .load(Uri.parse(photo.uri))
            .apply(
                RequestOptions()
                    .placeholder(R.color.teal_200)
                    .error(R.color.teal_200)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
            )
            .into(holder.imageView)

        // 添加点击事件
        holder.itemView.setOnClickListener {
            onPhotoClick?.invoke(position)
        }
    }

    override fun getItemCount(): Int = photoList.size
}
