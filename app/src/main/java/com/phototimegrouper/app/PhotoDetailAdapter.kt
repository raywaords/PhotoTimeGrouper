package com.phototimegrouper.app

import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView

class PhotoDetailAdapter(
    private val photoList: List<PhotoItem>,
    private val viewPager2: ViewPager2,
    private val onPhotoClick: (() -> Unit)? = null
) : RecyclerView.Adapter<PhotoDetailAdapter.PhotoDetailViewHolder>() {

    class PhotoDetailViewHolder(
        view: View, 
        viewPager2: ViewPager2,
        onPhotoClick: (() -> Unit)?
    ) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.photoView)
        val loadingProgressBar: ProgressBar = view.findViewById(R.id.loadingProgressBar)
        
        init {
            // 解决 PhotoView �?ViewPager2 的触摸冲�?            // 使用自定义的触摸监听器，�?PhotoView 被缩放时禁用 ViewPager2 滑动
            photoView.setOnScaleChangeListener(object : com.github.chrisbanes.photoview.OnScaleChangedListener {
                override fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float) {
                    // 如果图片被缩放（scale > 1.0），禁用 ViewPager2 的滑�?                    viewPager2.isUserInputEnabled = scaleFactor <= 1.0f
                }
            })
            
            // 点击照片：如果已缩放则重置，否则返回
            photoView.setOnPhotoTapListener { _, _, _ ->
                if (photoView.scale > 1.0f) {
                    // 如果照片被缩放，重置到原始大�?                    photoView.setScale(1.0f, true)
                    viewPager2.isUserInputEnabled = true
                } else {
                    // 如果照片未缩放，返回主列�?                    onPhotoClick?.invoke()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_detail, parent, false)
        return PhotoDetailViewHolder(view, viewPager2, onPhotoClick)
    }

    override fun onBindViewHolder(holder: PhotoDetailViewHolder, position: Int) {
        val photo = photoList.getOrNull(position) ?: return
        
        // 清除之前的图�?        Glide.with(holder.itemView.context).clear(holder.photoView)
        holder.loadingProgressBar.visibility = View.VISIBLE
        
        // 使用 Glide 加载全尺寸图�?        Glide.with(holder.itemView.context)
            .load(Uri.parse(photo.uri))
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .fitCenter()
            )
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.loadingProgressBar.visibility = View.GONE
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.loadingProgressBar.visibility = View.GONE
                    return false
                }
            })
            .into(holder.photoView)
    }

    override fun getItemCount(): Int = photoList.size
}
