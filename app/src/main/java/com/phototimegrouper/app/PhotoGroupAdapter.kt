package com.phototimegrouper.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PhotoGroupAdapter(
    private val context: Context,
    private val groupedPhotos: Map<String, List<PhotoItem>>,
    private var viewMode: ViewMode = ViewMode.LARGE_ICON,
    private val isSelectionMode: (() -> Boolean)? = null,
    private val onPhotoClick: ((List<PhotoItem>, Int) -> Unit)? = null,
    private val onPhotoLongClick: ((List<PhotoItem>, Int) -> Unit)? = null,
    private val isPhotoSelected: ((Long) -> Boolean)? = null,
    private val isFavorite: ((Long) -> Boolean)? = null,
    private val onToggleFavorite: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<PhotoGroupAdapter.PhotoGroupViewHolder>() {

    // 按日期降序排序（最新的在前�?
    private val dateList = groupedPhotos.keys.sortedDescending()
    
    // 将所有照片展平为一个列表，用于详情�?
    private val allPhotosList: List<PhotoItem> = dateList.flatMap { date ->
        groupedPhotos[date] ?: emptyList()
    }

    class PhotoGroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val groupRecyclerView: RecyclerView = view.findViewById(R.id.groupRecyclerView)
    }

    /**
     * 更新查看模式
     */
    fun updateViewMode(newViewMode: ViewMode) {
        if (viewMode != newViewMode) {
            viewMode = newViewMode
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoGroupViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo_group, parent, false)
        return PhotoGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoGroupViewHolder, position: Int) {
        val date = dateList.getOrNull(position) ?: return
        val photos = groupedPhotos[date] ?: return

        // 使用工具类格式化日期
        holder.dateTextView.text = DateFormatter.formatDateHeader(date)
        
        // 计算当前日期组在所有照片中的起始位�?
        var startIndex = 0
        for (i in 0 until position) {
            val prevDate = dateList[i]
            startIndex += groupedPhotos[prevDate]?.size ?: 0
        }
        
        // 根据查看模式设置不同的布局和适配�?
        when (viewMode) {
            ViewMode.EXTRA_LARGE_ICON -> {
                // 超大图标：一张图片占满一天的宽度，横向滚�?
                holder.groupRecyclerView.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                val adapter = PhotoExtraLargeAdapter(
                    context,
                    photos,
                    isSelectionMode = { isSelectionMode?.invoke() ?: false },
                    isPhotoSelected = { photoId -> isPhotoSelected?.invoke(photoId) ?: false },
                    isFavorite = { photoId -> isFavorite?.invoke(photoId) ?: false },
                    onToggleFavorite = { photoId -> onToggleFavorite?.invoke(photoId) },
                    onPhotoClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoClick?.invoke(allPhotosList, globalPosition)
                    },
                    onPhotoLongClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoLongClick?.invoke(allPhotosList, globalPosition)
                    }
                )
                holder.groupRecyclerView.adapter = adapter
                // 添加滚动监听，检测可见性变�?
                setupScrollListenerForExtraLarge(holder.groupRecyclerView, adapter)
            }
            ViewMode.LARGE_ICON -> {
                // 大图标：最多三张图片占满一天的宽度，横向滚�?
                // 如果少于3张，动态调整列数避免空�?
                val spanCount = minOf(3, photos.size)
                val layoutManager = GridLayoutManager(
                    context,
                    if (spanCount > 0) spanCount else 1, // 至少1�?
                    GridLayoutManager.HORIZONTAL,
                    false
                )
                holder.groupRecyclerView.layoutManager = layoutManager
                val adapter = PhotoGridAdapter(
                    context,
                    photos,
                    spanCount = spanCount,
                    photoHeight = context.resources.getDimensionPixelSize(R.dimen.photo_item_height),
                    isSelectionMode = { isSelectionMode?.invoke() ?: false },
                    isPhotoSelected = { photoId -> isPhotoSelected?.invoke(photoId) ?: false },
                    isFavorite = { photoId -> isFavorite?.invoke(photoId) ?: false },
                    onToggleFavorite = { photoId -> onToggleFavorite?.invoke(photoId) },
                    onPhotoClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoClick?.invoke(allPhotosList, globalPosition)
                    },
                    onPhotoLongClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoLongClick?.invoke(allPhotosList, globalPosition)
                    }
                )
                holder.groupRecyclerView.adapter = adapter
                // 添加滚动监听，检测可见性变�?
                setupScrollListener(holder.groupRecyclerView, adapter)
            }
            ViewMode.SMALL_ICON -> {
                // 小图标：九宫格形式（3列网格），在当天单元格内显示，使用缩略图
                // 如果照片超过9张，显示多行，可通过垂直滚动查看所有照�?
                holder.groupRecyclerView.layoutManager = GridLayoutManager(
                    context,
                    3,
                    GridLayoutManager.VERTICAL,
                    false
                )
                // 启用嵌套滚动，允许在小图标模式下垂直滚动查看更多照片
                holder.groupRecyclerView.isNestedScrollingEnabled = true
                val adapter = PhotoSmallIconAdapter(
                    context,
                    photos,
                    isSelectionMode = { isSelectionMode?.invoke() ?: false },
                    isPhotoSelected = { photoId -> isPhotoSelected?.invoke(photoId) ?: false },
                    isFavorite = { photoId -> isFavorite?.invoke(photoId) ?: false },
                    onToggleFavorite = { photoId -> onToggleFavorite?.invoke(photoId) },
                    onPhotoClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoClick?.invoke(allPhotosList, globalPosition)
                    },
                    onPhotoLongClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoLongClick?.invoke(allPhotosList, globalPosition)
                    }
                )
                holder.groupRecyclerView.adapter = adapter
                // 添加滚动监听，检测可见性变�?
                setupScrollListenerForSmallIcon(holder.groupRecyclerView, adapter)
            }
            ViewMode.DETAILS -> {
                // 详细信息：垂直列表，显示名称、大小、创建日期、文件类型等
                holder.groupRecyclerView.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                holder.groupRecyclerView.adapter = PhotoDetailListAdapter(
                    context,
                    photos,
                    onPhotoClick = { localPosition ->
                        val globalPosition = startIndex + localPosition
                        onPhotoClick?.invoke(allPhotosList, globalPosition)
                    }
                )
            }
        }
    }

    override fun getItemCount(): Int = dateList.size
    
    /**
     * 设置滚动监听，用于检测视频可见性变�?
     */
    private fun setupScrollListener(recyclerView: RecyclerView, adapter: PhotoGridAdapter) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 滚动时检查可见项
                adapter.checkVisibleItems()
            }
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // 滚动停止时也检查一�?
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.checkVisibleItems()
                }
            }
        })
    }
    
    /**
     * 设置滚动监听，用于超大图标模式的视频可见性检�?
     */
    private fun setupScrollListenerForExtraLarge(recyclerView: RecyclerView, adapter: PhotoExtraLargeAdapter) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                adapter.checkVisibleItems()
            }
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.checkVisibleItems()
                }
            }
        })
    }
    
    /**
     * 设置滚动监听，用于小图标模式的视频可见性检�?
     */
    private fun setupScrollListenerForSmallIcon(recyclerView: RecyclerView, adapter: PhotoSmallIconAdapter) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                adapter.checkVisibleItems()
            }
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.checkVisibleItems()
                }
            }
        })
    }
}
