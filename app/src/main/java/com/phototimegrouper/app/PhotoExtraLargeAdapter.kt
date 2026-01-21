package com.phototimegrouper.app

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

/**
 * 超大图标适配�?
 * 一张图片占满一天的宽度
 * 支持视频自动静默播放预览
 */
class PhotoExtraLargeAdapter(
    private val context: Context,
    private val photoList: List<PhotoItem>,
    private val isSelectionMode: (() -> Boolean)? = null,
    private val isPhotoSelected: ((Long) -> Boolean)? = null,
    private val isFavorite: ((Long) -> Boolean)? = null,
    private val onToggleFavorite: ((Long) -> Unit)? = null,
    private val onPhotoClick: ((Int) -> Unit)? = null,
    private val onPhotoLongClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<PhotoExtraLargeAdapter.PhotoExtraLargeViewHolder>() {

    // 当前正在播放的视频位�?
    private var currentPlayingPosition: Int = -1
    // 存储所�?ViewHolder，用于可见性检�?
    private val viewHolders = mutableMapOf<Int, PhotoExtraLargeViewHolder>()

    class PhotoExtraLargeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photoImageView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val videoPlayIcon: ImageView = view.findViewById(R.id.videoPlayIcon)
        val videoView: VideoView = view.findViewById(R.id.videoView)
        val checkMark: ImageView = view.findViewById(R.id.checkMark)
        val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
        val favoriteIcon: ImageView = view.findViewById(R.id.favoriteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoExtraLargeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo_extra_large, parent, false)
        return PhotoExtraLargeViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoExtraLargeViewHolder, position: Int) {
        val photo = photoList.getOrNull(position) ?: return
        
        // 存储 ViewHolder 引用
        viewHolders[position] = holder
        
        // 先重置所有视图状态，避免复用 ViewHolder 时的状态混�?
        holder.imageView.visibility = View.VISIBLE
        holder.videoView.visibility = View.GONE
        holder.videoPlayIcon.visibility = View.GONE
        
        // 使用工具类格式化日期
        holder.timestampTextView.text = DateFormatter.formatDateTime(photo.dateModified)

        // 判断是否为视频（直接使用 mediaType，确保准确性）
        val isVideo = photo.mediaType == PhotoItem.MediaType.VIDEO
        
        if (isVideo) {
            // 视频：先加载缩略图作为封�?
            holder.imageView.visibility = View.VISIBLE
            holder.videoView.visibility = View.GONE
            holder.videoPlayIcon.visibility = View.VISIBLE
            
            // 使用 Glide 加载视频的第一帧作为缩略图
            Glide.with(context)
                .asBitmap()
                .load(Uri.parse(photo.uri))
                .apply(
                    RequestOptions()
                        .placeholder(R.color.surface_variant)
                        .error(R.color.surface_variant)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                )
                .into(holder.imageView)
            
            // 设置视频 URI
            holder.videoView.setVideoURI(Uri.parse(photo.uri))
            holder.videoView.setOnPreparedListener { mediaPlayer ->
                // 再次检�?mediaType，确�?ViewHolder 没有被复用为图片
                val currentPhoto = photoList.getOrNull(position)
                if (currentPhoto?.mediaType == PhotoItem.MediaType.VIDEO) {
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f) // 静音播放
                    
                    // 设置视频缩放模式�?SCALE_MODE_SCALE_TO_FIT_WITH_CROPPING (类似centerCrop)
                    try {
                        val method = mediaPlayer.javaClass.getMethod("setVideoScalingMode", Int::class.java)
                        method.invoke(mediaPlayer, 2) // 2 = SCALE_MODE_SCALE_TO_FIT_WITH_CROPPING
                    } catch (e: Exception) {
                        // 如果反射失败，通过布局参数调整
                        holder.videoView.layoutParams.apply {
                            width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        }
                    }
                    
                    // 准备完成后，延迟检查可见性（确保布局完成�?
                    holder.itemView.postDelayed({
                        val currentPhoto2 = photoList.getOrNull(position)
                        if (currentPhoto2?.mediaType == PhotoItem.MediaType.VIDEO && isViewHolderVisible(holder)) {
                            startVideoPlayback(holder, position)
                        }
                    }, 100)
                } else {
                    // 如果是图片，确保不显示播放图�?
                    holder.videoPlayIcon.visibility = View.GONE
                    holder.videoView.visibility = View.GONE
                    holder.imageView.visibility = View.VISIBLE
                }
            }
            
            // 延迟检查可见性，确保布局完成
            holder.itemView.post {
                // 再次检�?mediaType，确�?ViewHolder 没有被复用为图片
                val currentPhoto = photoList.getOrNull(position)
                if (currentPhoto?.mediaType == PhotoItem.MediaType.VIDEO) {
                    if (isViewHolderVisible(holder) && position != currentPlayingPosition) {
                        startVideoPlayback(holder, position)
                    } else if (!isViewHolderVisible(holder)) {
                        stopVideoPlayback(holder)
                    }
                } else {
                    // 如果是图片，确保不显示播放图�?
                    holder.videoPlayIcon.visibility = View.GONE
                    holder.videoView.visibility = View.GONE
                    holder.imageView.visibility = View.VISIBLE
                }
            }
        } else {
            // 图片：显�?ImageView，隐�?VideoView 和播放图�?
            // 注意：不调用 stopVideoPlayback()，因为它可能会显示播放图�?
            holder.imageView.visibility = View.VISIBLE
            holder.videoView.visibility = View.GONE
            holder.videoPlayIcon.visibility = View.GONE // 确保图片不显示播放图�?
            
            // 直接停止 VideoView，不调用 stopVideoPlayback()（避免显示播放图标）
            try {
                holder.videoView.stopPlayback()
                holder.videoView.pause()
            } catch (e: Exception) {
                // 忽略错误
            }
            
            // 使用 Glide 加载图片
            Glide.with(context)
                .load(Uri.parse(photo.uri))
                .apply(
                    RequestOptions()
                        .placeholder(R.color.surface_variant)
                        .error(R.color.surface_variant)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                )
                .into(holder.imageView)
        }

        // 最后再次确认状态（关键修复：确保图片不显示播放图标�?
        // 在绑定完成后，强制检�?mediaType，确保状态正�?
        if (photo.mediaType == PhotoItem.MediaType.IMAGE) {
            holder.videoPlayIcon.visibility = View.GONE
            holder.videoView.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE
        }

        // 更新选择状态显示
        val inSelectionMode = isSelectionMode?.invoke() ?: false
        val isSelected = isPhotoSelected?.invoke(photo.id) ?: false
        
        if (inSelectionMode) {
            holder.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        } else {
            holder.checkMark.visibility = View.GONE
            holder.selectionOverlay.visibility = View.GONE
        }

        // 更新收藏星标状态
        val favorite = isFavorite?.invoke(photo.id) ?: false
        holder.favoriteIcon.setImageResource(
            if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )

        // 单击：预览（使用系统查看器）
        holder.itemView.setOnClickListener {
            onPhotoClick?.invoke(position)
        }

        // 长按：弹出操作菜单（删除 / 分享）
        holder.itemView.setOnLongClickListener {
            onPhotoLongClick?.invoke(position)
            true
        }

        // 长按星标：收藏/取消收藏
        holder.favoriteIcon.setOnLongClickListener {
            onToggleFavorite?.invoke(photo.id)
            true
        }
    }
    
    /**
     * 检�?ViewHolder 是否可见
     */
    private fun isViewHolderVisible(holder: PhotoExtraLargeViewHolder): Boolean {
        val view = holder.itemView
        if (!view.isShown || view.visibility != View.VISIBLE) {
            return false
        }
        val rect = android.graphics.Rect()
        val visible = view.getGlobalVisibleRect(rect)
        // 至少 50% 可见才认为是可见�?
        val visibleArea = rect.width() * rect.height()
        val totalArea = view.width * view.height
        return visible && totalArea > 0 && (visibleArea.toFloat() / totalArea) > 0.5f
    }
    
    /**
     * 开始视频播�?
     */
    private fun startVideoPlayback(holder: PhotoExtraLargeViewHolder, position: Int) {
        if (currentPlayingPosition != position) {
            // 停止之前播放的视�?
            if (currentPlayingPosition != -1) {
                val prevHolder = viewHolders[currentPlayingPosition]
                prevHolder?.let { stopVideoPlayback(it) }
            }
            currentPlayingPosition = position
        }
        
        try {
            // 确保 VideoView 的布局参数正确，填充整个容�?
            holder.videoView.layoutParams.apply {
                width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            }
            
            // 显示 VideoView，隐藏缩略图和播放图�?
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.VISIBLE
            holder.videoPlayIcon.visibility = View.GONE // 播放时隐藏播放图�?
            
            // 确保视频开始播�?
            if (!holder.videoView.isPlaying) {
                holder.videoView.start()
            }
            
            // 再次尝试设置缩放模式（在播放开始后�?
            holder.videoView.setOnPreparedListener { mediaPlayer ->
                try {
                    val method = mediaPlayer.javaClass.getMethod("setVideoScalingMode", Int::class.java)
                    method.invoke(mediaPlayer, 2) // 2 = SCALE_MODE_SCALE_TO_FIT_WITH_CROPPING
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        } catch (e: Exception) {
            // 播放失败时忽�?
        }
    }
    
    /**
     * 停止视频播放
     */
    private fun stopVideoPlayback(holder: PhotoExtraLargeViewHolder) {
        try {
            if (holder.videoView.isPlaying) {
                holder.videoView.pause()
            }
            holder.videoView.stopPlayback()
            
            // 检查当�?holder 对应�?photo 是否为视�?
            val position = holder.bindingAdapterPosition
            val photo = if (position != RecyclerView.NO_POSITION) {
                photoList.getOrNull(position)
            } else {
                null
            }
            
            // 只有视频才显示播放图�?
            if (photo?.mediaType == PhotoItem.MediaType.VIDEO) {
                // 停止播放时，显示缩略图和播放图标，隐�?VideoView
                holder.imageView.visibility = View.VISIBLE
                holder.videoView.visibility = View.GONE
                holder.videoPlayIcon.visibility = View.VISIBLE // 停止时显示播放图�?
            } else {
                // 图片：确保不显示播放图标
                holder.imageView.visibility = View.VISIBLE
                holder.videoView.visibility = View.GONE
                holder.videoPlayIcon.visibility = View.GONE
            }
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    override fun onViewRecycled(holder: PhotoExtraLargeViewHolder) {
        super.onViewRecycled(holder)
        // 回收时停止视频播�?
        stopVideoPlayback(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            viewHolders.remove(position)
            if (currentPlayingPosition == position) {
                currentPlayingPosition = -1
            }
        }
    }
    
    override fun onViewAttachedToWindow(holder: PhotoExtraLargeViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            val photo = photoList.getOrNull(position)
            if (photo != null) {
                // 确保 ViewHolder 被存�?
                viewHolders[position] = holder
                
                // 如果是图片，确保不显示播放图�?
                if (photo.mediaType == PhotoItem.MediaType.IMAGE) {
                    holder.videoPlayIcon.visibility = View.GONE
                    holder.videoView.visibility = View.GONE
                    holder.imageView.visibility = View.VISIBLE
                } else if (photo.mediaType == PhotoItem.MediaType.VIDEO) {
                    // 视频：延迟检查可见性并自动播放（允许替换当前播放的视频�?
                    holder.itemView.postDelayed({
                        val currentPhoto = photoList.getOrNull(position)
                        if (currentPhoto?.mediaType == PhotoItem.MediaType.VIDEO && isViewHolderVisible(holder)) {
                            // 检查是否需要播放（如果当前没有播放，或者这个视频可见度更高�?
                            if (currentPlayingPosition == -1 || position != currentPlayingPosition) {
                                checkVisibleItems() // 重新检查所有可见项，选择最佳视�?
                            }
                        }
                    }, 150)
                }
            }
        }
    }
    
    override fun onViewDetachedFromWindow(holder: PhotoExtraLargeViewHolder) {
        super.onViewDetachedFromWindow(holder)
        stopVideoPlayback(holder)
    }

    override fun getItemCount(): Int = photoList.size
    
    /**
     * 检查所有可见项，自动播放第一个可见的视频
     */
    fun checkVisibleItems() {
        // 先停止当前播放的视频
        if (currentPlayingPosition != -1) {
            val currentHolder = viewHolders[currentPlayingPosition]
            currentHolder?.let { stopVideoPlayback(it) }
        }
        
        // 获取 RecyclerView 的父视图（通过第一�?ViewHolder 获取�?
        val recyclerView = viewHolders.values.firstOrNull()?.itemView?.parent as? RecyclerView ?: return
        
        var bestVisiblePosition = -1
        var bestVisibleRatio = 0f
        
        // 遍历所有可见的 ViewHolder（不仅仅是已存储的）
        val layoutManager = recyclerView.layoutManager ?: return
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val position = layoutManager.getPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            
            val photo = photoList.getOrNull(position) ?: continue
            val holder = recyclerView.getChildViewHolder(child) as? PhotoExtraLargeViewHolder ?: continue
            
            // 确保 ViewHolder 被存�?
            viewHolders[position] = holder
            
            // 如果是图片，确保不显示播放图�?
            if (photo.mediaType == PhotoItem.MediaType.IMAGE) {
                holder.videoPlayIcon.visibility = View.GONE
                holder.videoView.visibility = View.GONE
                holder.imageView.visibility = View.VISIBLE
                try {
                    holder.videoView.stopPlayback()
                } catch (e: Exception) {
                    // 忽略错误
                }
            } else if (photo.mediaType == PhotoItem.MediaType.VIDEO) {
                // 检查视频是否可�?
                if (isViewHolderVisible(holder)) {
                    val ratio = getVisibilityRatio(holder)
                    if (ratio > bestVisibleRatio) {
                        bestVisibleRatio = ratio
                        bestVisiblePosition = position
                    }
                } else {
                    // 不可见的视频停止播放
                    if (currentPlayingPosition == position) {
                        stopVideoPlayback(holder)
                    }
                }
            }
        }
        
        // 播放可见度最高的视频
        if (bestVisiblePosition != -1 && bestVisiblePosition != currentPlayingPosition) {
            val bestHolder = viewHolders[bestVisiblePosition]
            bestHolder?.let {
                startVideoPlayback(it, bestVisiblePosition)
            }
        }
    }
    
    /**
     * 获取 ViewHolder 的可见比�?
     */
    private fun getVisibilityRatio(holder: PhotoExtraLargeViewHolder): Float {
        val view = holder.itemView
        val rect = android.graphics.Rect()
        val visible = view.getGlobalVisibleRect(rect)
        if (!visible) return 0f
        val visibleArea = rect.width() * rect.height()
        val totalArea = view.width * view.height
        return if (totalArea > 0) visibleArea.toFloat() / totalArea else 0f
    }
}
