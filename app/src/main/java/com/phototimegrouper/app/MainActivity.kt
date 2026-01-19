package com.phototimegrouper.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.databinding.ActivityMainBinding
import com.phototimegrouper.app.repository.PhotoRepository
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_PERMISSIONS = 100
    private var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
    private var currentViewMode: ViewMode = ViewMode.LARGE_ICON
    private var photoGroupAdapter: PhotoGroupAdapter? = null
    
    // 选择模式相关
    private var isSelectionMode = false
    private val selectedPhotos = mutableSetOf<Long>()
    
    // 收藏功能相关
    private val favoritePhotos = mutableSetOf<Long>()      // 收藏的照片/视频 ID 集合
    private val PREF_FAVORITES = "pref_favorites"         // 收藏持久化用的 key
    private var showFavoritesOnly: Boolean = false        // 是否只显示收藏内容
    
    // 搜索筛选相关
    private var currentMediaTypeFilter: PhotoItem.MediaType? = null // 当前媒体类型筛选
    private var currentDaysFilter: Int? = null // 当前日期筛选（天数，-1表示今年）
    
    // SharedPreferences 用于持久化查看模式等配置（收藏现在使用 Room）
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_VIEW_MODE = "pref_view_mode"
    
    // Repository 用于数据访问
    private lateinit var photoRepository: PhotoRepository
    
    // Permissions for different Android versions
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始�?SharedPreferences
        sharedPreferences = getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)
        
        // �?SharedPreferences 加载上次的查看模�?
        val savedViewModeName = sharedPreferences.getString(PREF_VIEW_MODE, ViewMode.LARGE_ICON.name)
        currentViewMode = try {
            ViewMode.valueOf(savedViewModeName ?: ViewMode.LARGE_ICON.name)
        } catch (e: Exception) {
            ViewMode.LARGE_ICON
        }

        // 加载收藏列表
        loadFavorites()
        
        setupSwipeRefresh()
        setupViewModeMenu()
        setupSelectionMode()
        checkPermissions()
    }
    
    private fun setupSelectionMode() {
        // 全选按钮
        binding.selectAllButton.setOnClickListener {
            if (selectedPhotos.size == allPhotosList.size) {
                // 取消全选
                selectedPhotos.clear()
                binding.selectAllButton.text = getString(R.string.select_all)
            } else {
                // 全选
                selectedPhotos.clear()
                selectedPhotos.addAll(allPhotosList.map { it.id })
                binding.selectAllButton.text = getString(R.string.deselect_all)
            }
            updateSelectionUI()
            photoGroupAdapter?.notifyDataSetChanged()
        }
        
        // 分享按钮
        binding.shareButton.setOnClickListener {
            shareSelectedPhotos()
        }
        
        // 删除按钮
        binding.deleteButton.setOnClickListener {
            deleteSelectedPhotos()
        }
        
        // 取消选择按钮
        binding.cancelSelectionButton.setOnClickListener {
            exitSelectionMode()
        }
    }
    
    private fun enterSelectionMode(initialPhotoId: Long? = null) {
        isSelectionMode = true
        selectedPhotos.clear()
        initialPhotoId?.let { selectedPhotos.add(it) }
        
        // 显示操作栏，隐藏菜单按钮
        binding.selectionActionBar.visibility = View.VISIBLE
        binding.viewModeMenuButton.visibility = View.GONE
        
        updateSelectionUI()
        photoGroupAdapter?.notifyDataSetChanged()
    }
    
    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        
        // 隐藏操作栏，显示菜单按钮
        binding.selectionActionBar.visibility = View.GONE
        binding.viewModeMenuButton.visibility = View.VISIBLE
        
        photoGroupAdapter?.notifyDataSetChanged()
    }
    
    private fun updateSelectionUI() {
        val count = selectedPhotos.size
        binding.selectedCountTextView.text = getString(R.string.selected_count, count)
        binding.selectAllButton.text = if (count == allPhotosList.size && count > 0) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }
        
        // 根据选中数量启用/禁用按钮
        val hasSelection = count > 0
        binding.shareButton.isEnabled = hasSelection
        binding.deleteButton.isEnabled = hasSelection
    }
    
    fun isPhotoSelected(photoId: Long): Boolean {
        return selectedPhotos.contains(photoId)
    }
    
    fun togglePhotoSelection(photoId: Long) {
        if (selectedPhotos.contains(photoId)) {
            selectedPhotos.remove(photoId)
        } else {
            selectedPhotos.add(photoId)
        }
        updateSelectionUI()
    }
    
    private fun shareSelectedPhotos() {
        val selectedItems = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要分享的照片", Toast.LENGTH_SHORT).show()
            return
        }
        
        val uris = selectedItems.map { Uri.parse(it.uri) }
        
        // 判断是否包含视频
        val hasVideo = selectedItems.any { it.mediaType == PhotoItem.MediaType.VIDEO }
        val hasImage = selectedItems.any { it.mediaType == PhotoItem.MediaType.IMAGE }
        
        // 根据内容类型设置MIME类型
        val mimeType = when {
            hasVideo && hasImage -> "*/*" // 混合类型
            hasVideo -> "video/*" // 只有视频
            else -> "image/*" // 只有图片
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            startActivity(Intent.createChooser(shareIntent, "分享媒体"))
        } catch (e: Exception) {
            Toast.makeText(this, "无法分享: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteSelectedPhotos() {
        val selectedItems = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的照片", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage(getString(R.string.delete_confirm, selectedItems.size))
            .setPositiveButton("删除") { _, _ ->
                performDelete(selectedItems)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performDelete(photosToDelete: List<PhotoItem>) {
        lifecycleScope.launch {
            try {
                var successCount = 0
                withContext(Dispatchers.IO) {
                    photosToDelete.forEach { photo ->
                        try {
                            val uri = Uri.parse(photo.uri)
                            val deleted = contentResolver.delete(uri, null, null)
                            if (deleted > 0) {
                                successCount++
                            }
                        } catch (e: Exception) {
                            // 忽略单个删除失败
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (successCount > 0) {
                        Toast.makeText(this@MainActivity, getString(R.string.delete_success, successCount), Toast.LENGTH_SHORT).show()
                        // 退出选择模式并刷新列表
                        exitSelectionMode()
                        loadPhotos()
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupViewModeMenu() {
        binding.viewModeMenuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.view_mode_menu, popupMenu.menu)
            
            // 设置当前选中�?
            when (currentViewMode) {
                ViewMode.EXTRA_LARGE_ICON -> popupMenu.menu.findItem(R.id.menu_extra_large_icon).isChecked = true
                ViewMode.LARGE_ICON -> popupMenu.menu.findItem(R.id.menu_large_icon).isChecked = true
                ViewMode.SMALL_ICON -> popupMenu.menu.findItem(R.id.menu_small_icon).isChecked = true
                ViewMode.DETAILS -> popupMenu.menu.findItem(R.id.menu_details).isChecked = true
            }
            
            // 设置为单选模�?
            popupMenu.menu.setGroupCheckable(R.id.view_mode_group, true, true)
            
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_extra_large_icon -> {
                        updateViewMode(ViewMode.EXTRA_LARGE_ICON)
                        true
                    }
                    R.id.menu_large_icon -> {
                        updateViewMode(ViewMode.LARGE_ICON)
                        true
                    }
                    R.id.menu_small_icon -> {
                        updateViewMode(ViewMode.SMALL_ICON)
                        true
                    }
                    R.id.menu_details -> {
                        updateViewMode(ViewMode.DETAILS)
                        true
                    }
                    R.id.menu_privacy_policy -> {
                        openPrivacyPolicy()
                        true
                    }
                    R.id.menu_show_favorites -> {
                        toggleFavoritesFilter()
                        true
                    }
                    R.id.menu_search -> {
                        showSearchDialog()
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
        }
    }
    
    private fun loadFavorites() {
        lifecycleScope.launch {
            try {
                val favoriteIds = photoRepository.getFavoritePhotoIds()
                favoritePhotos.clear()
                favoritePhotos.addAll(favoriteIds)
            } catch (e: Exception) {
                // 如果加载失败，使用空集合（不影响应用启动）
                favoritePhotos.clear()
            }
        }
    }
    
    fun toggleFavorite(photoId: Long) {
        lifecycleScope.launch {
            try {
                val newState = photoRepository.toggleFavorite(photoId)
                // 更新本地集合
                if (newState) {
                    favoritePhotos.add(photoId)
                } else {
                    favoritePhotos.remove(photoId)
                }
                withContext(Dispatchers.Main) {
                    photoGroupAdapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    fun isFavorite(photoId: Long): Boolean {
        // 先从内存集合检查（快速），如果不在集合中，再从数据库查询
        return favoritePhotos.contains(photoId)
    }
    
    private fun toggleFavoritesFilter() {
        showFavoritesOnly = !showFavoritesOnly
        if (showFavoritesOnly && favoritePhotos.isEmpty()) {
            Toast.makeText(this, "您还没有收藏任何照片", Toast.LENGTH_SHORT).show()
            showFavoritesOnly = false
            return
        }
        loadPhotos() // 重新加载照片，应用筛选
    }
    
    private fun showSearchDialog() {
        // 简单的筛选对话框
        val options = arrayOf("全部", "仅照片", "仅视频", "最近7天", "最近30天", "今年")
        AlertDialog.Builder(this)
            .setTitle("筛选")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> filterPhotos(null, null) // 全部
                    1 -> filterPhotos(PhotoItem.MediaType.IMAGE, null) // 仅照片
                    2 -> filterPhotos(PhotoItem.MediaType.VIDEO, null) // 仅视频
                    3 -> filterPhotos(null, 7) // 最近7天
                    4 -> filterPhotos(null, 30) // 最近30天
                    5 -> filterPhotos(null, -1) // 今年
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun filterPhotos(mediaType: PhotoItem.MediaType?, days: Int?) {
        // 设置筛选条件
        currentMediaTypeFilter = mediaType
        currentDaysFilter = days
        
        // 重新加载照片，应用筛选
        loadPhotos()
    }
    
    private fun updateViewMode(newViewMode: ViewMode) {
        if (currentViewMode != newViewMode) {
            currentViewMode = newViewMode
            
            // 保存�?SharedPreferences
            sharedPreferences.edit()
                .putString(PREF_VIEW_MODE, newViewMode.name)
                .apply()
            
            // 更新适配�?
            photoGroupAdapter?.updateViewMode(newViewMode)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPhotos()
        }
        
        // 设置刷新指示器颜�?- 使用主题颜色
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_700,
            R.color.teal_200,
            R.color.teal_700
        )
    }

    private fun checkPermissions() {
        val permissionsToRequest = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else {
            loadPhotos()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadPhotos()
            } else {
                Toast.makeText(this, "需要存储权限才能加载照片", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadPhotos() {
        // 只有在非下拉刷新时才显示进度�?
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = android.view.View.VISIBLE
        }
        
        lifecycleScope.launch {
            try {
                // 使用 Repository 加载照片
                val photos = withContext(Dispatchers.IO) {
                    photoRepository.loadPhotosFromMediaStore()
                }
                
                // 同步数据到数据库（在后台执行，不阻塞 UI）
                withContext(Dispatchers.IO) {
                    photoRepository.syncMediaStoreToDatabase(photos)
                    // 同步后重新加载收藏列表
                    val favoriteIds = photoRepository.getFavoritePhotoIds()
                    favoritePhotos.clear()
                    favoritePhotos.addAll(favoriteIds)
                }
                
                // 应用筛选条件
                var filteredPhotos = photos
                
                // 1. 应用收藏筛选
                if (showFavoritesOnly) {
                    filteredPhotos = filteredPhotos.filter { favoritePhotos.contains(it.id) }
                }
                
                // 2. 应用媒体类型筛选
                currentMediaTypeFilter?.let { mediaType ->
                    filteredPhotos = filteredPhotos.filter { it.mediaType == mediaType }
                }
                
                // 3. 应用日期筛选
                currentDaysFilter?.let { days ->
                    val currentTime = System.currentTimeMillis() / 1000 // 转换为秒
                    filteredPhotos = filteredPhotos.filter { photo ->
                        when (days) {
                            -1 -> {
                                // 今年：检查年份
                                val photoYear = java.util.Calendar.getInstance().apply {
                                    timeInMillis = photo.dateAdded * 1000
                                }.get(java.util.Calendar.YEAR)
                                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                photoYear == currentYear
                            }
                            else -> {
                                // 最近N天
                                val daysAgo = currentTime - (days * 24 * 60 * 60)
                                photo.dateAdded >= daysAgo
                            }
                        }
                    }
                }
                
                val groupedPhotos = groupPhotosByDate(filteredPhotos)
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    // 将所有照片展平为一个列表，按日期顺序（用于详情页）
                    val dateList = groupedPhotos.keys.sortedDescending()
                    val flatPhotoList = dateList.flatMap { date ->
                        groupedPhotos[date] ?: emptyList()
                    }
                    allPhotosList = ArrayList(flatPhotoList)
                    
                    // 将照片列表存储到 Application 类中，避�?Intent 数据过大
                    (application as? PhotoTimeGrouperApp)?.setAllPhotosList(flatPhotoList)
                    
                    binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    photoGroupAdapter = PhotoGroupAdapter(
                        this@MainActivity,
                        groupedPhotos,
                        currentViewMode,
                        isSelectionMode = { isSelectionMode },
                        onPhotoClick = { photosList, position ->
                            if (isSelectionMode) {
                                // 选择模式下，切换选中状态
                                val photo = photosList.getOrNull(position)
                                photo?.let { togglePhotoSelection(it.id) }
                                photoGroupAdapter?.notifyDataSetChanged()
                            } else {
                                // 普通模式下，打开详情
                                openPhotoDetail(photosList, position)
                            }
                        },
                        onPhotoLongClick = { photosList, position ->
                            val photo = photosList.getOrNull(position)
                            photo?.let {
                                if (!isSelectionMode) {
                                    enterSelectionMode(it.id)
                                }
                            }
                        },
                        isPhotoSelected = { photoId -> this@MainActivity.isPhotoSelected(photoId) }
                    )
                    binding.recyclerView.adapter = photoGroupAdapter
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this@MainActivity, "加载照片失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun loadPhotosFromMediaStore(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val allMedia = mutableListOf<PhotoItem>()
        
        // 加载图片（所有图片的 mediaType 都是 IMAGE�?
        allMedia.addAll(loadImagesFromMediaStore())
        
        // 加载视频（所有视频的 mediaType 都是 VIDEO�?
        allMedia.addAll(loadVideosFromMediaStore())
        
        // 按日期排序（最新的在前�?
        return@withContext allMedia.sortedByDescending { it.dateAdded }
    }
    
    private suspend fun loadImagesFromMediaStore(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = it.getColumnIndex(MediaStore.Images.Media.SIZE)
            val widthColumn = it.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = it.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val mimeTypeColumn = it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val bucketColumn = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = it.getColumnIndex(MediaStore.Images.Media.DATA)
            
            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "未知"
                    val dateAdded = it.getLong(dateAddedColumn)
                    val dateModified = it.getLong(dateModifiedColumn)
                    
                    // 读取扩展字段
                    val size = if (sizeColumn >= 0) it.getLong(sizeColumn) else 0L
                    val width = if (widthColumn >= 0) it.getInt(widthColumn) else 0
                    val height = if (heightColumn >= 0) it.getInt(heightColumn) else 0
                    val mimeType = if (mimeTypeColumn >= 0) (it.getString(mimeTypeColumn) ?: "") else ""
                    val bucketName = if (bucketColumn >= 0) (it.getString(bucketColumn) ?: "") else ""
                    val data = if (dataColumn >= 0) (it.getString(dataColumn) ?: "") else ""
                    
                    val photoUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    
                    // 创建 PhotoItem（ISO 将在后台异步加载�?
                    val photoItem = PhotoItem(
                        id = id,
                        uri = photoUri.toString(),
                        displayName = name,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        bucketDisplayName = bucketName,
                        data = data,
                        iso = 0, // 将在后续异步加载
                        mediaType = PhotoItem.MediaType.IMAGE
                    )
                    
                    photos.add(photoItem)
                } catch (e: Exception) {
                    // 跳过无法读取的照片项
                    continue
                }
            }
        }
        
        photos
    }
    
    private suspend fun loadVideosFromMediaStore(): List<PhotoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<PhotoItem>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeColumn = it.getColumnIndex(MediaStore.Video.Media.SIZE)
            val widthColumn = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightColumn = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)
            val mimeTypeColumn = it.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
            val bucketColumn = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = it.getColumnIndex(MediaStore.Video.Media.DATA)
            
            while (it.moveToNext()) {
                try {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "未知"
                    val dateAdded = it.getLong(dateAddedColumn)
                    val dateModified = it.getLong(dateModifiedColumn)
                    
                    // 读取扩展字段
                    val size = if (sizeColumn >= 0) it.getLong(sizeColumn) else 0L
                    val width = if (widthColumn >= 0) it.getInt(widthColumn) else 0
                    val height = if (heightColumn >= 0) it.getInt(heightColumn) else 0
                    val mimeType = if (mimeTypeColumn >= 0) (it.getString(mimeTypeColumn) ?: "") else ""
                    val bucketName = if (bucketColumn >= 0) (it.getString(bucketColumn) ?: "") else ""
                    val data = if (dataColumn >= 0) (it.getString(dataColumn) ?: "") else ""
                    
                    val videoUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                    
                    // 创建 PhotoItem（视频类型）
                    val videoItem = PhotoItem(
                        id = id,
                        uri = videoUri.toString(),
                        displayName = name,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        size = size,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        bucketDisplayName = bucketName,
                        data = data,
                        iso = 0,
                        mediaType = PhotoItem.MediaType.VIDEO
                    )
                    
                    videos.add(videoItem)
                } catch (e: Exception) {
                    // 跳过无法读取的视频项
                    continue
                }
            }
        }
        
        videos
    }

    private fun groupPhotosByDate(photoList: List<PhotoItem>): Map<String, List<PhotoItem>> {
        return photoList.groupBy { photo ->
            DateFormatter.formatDateForGroup(photo.dateModified)
        }
    }

    private fun openPrivacyPolicy() {
        // GitHub Pages 隐私政策地址
        val privacyPolicyUrl = "https://raywaords.github.io/PhotoTimeGrouper/privacy-policy.html"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(privacyPolicyUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "无法打开隐私政策页面: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openPhotoDetail(_photosList: List<PhotoItem>, position: Int) {
        val photo = _photosList.getOrNull(position) ?: return

        // 视频：仍然使用系统视频播放器打开
        if (photo.mediaType == PhotoItem.MediaType.VIDEO) {
            try {
                val videoUri = Uri.parse(photo.uri)
                val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(videoUri, "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(videoIntent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "未找到视频播放器，请安装一个视频播放应用", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "无法播放视频: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 图片：使用应用内的全屏预览页（不再弹出外部应用选择框）
            try {
                val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                    putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开图片详情: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 长按单张照片/视频时弹出的操作对话框：删除 / 分享
     */
    private fun showPhotoActionsDialog(photosList: List<PhotoItem>, position: Int) {
        val photo = photosList.getOrNull(position) ?: return
        val isFav = isFavorite(photo.id)

        val items = arrayOf(
            if (isFav) getString(R.string.unfavorite) else getString(R.string.favorite),
            getString(R.string.delete),
            getString(R.string.share)
        )
        AlertDialog.Builder(this)
            .setTitle(photo.displayName)
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> {
                        // 收藏/取消收藏
                        toggleFavorite(photo.id)
                        Toast.makeText(this, if (isFav) "已取消收藏" else "已收藏", Toast.LENGTH_SHORT).show()
                    }
                    1 -> deleteSinglePhoto(photo)
                    2 -> shareSinglePhoto(photo)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    /**
     * 删除单张照片或视频
     */
    private fun deleteSinglePhoto(photo: PhotoItem) {
        try {
            val uri = Uri.parse(photo.uri)
            val rows = contentResolver.delete(uri, null, null)
            if (rows > 0) {
                Toast.makeText(this, "已删除：${photo.displayName}", Toast.LENGTH_SHORT).show()
                // 删除后重新加载列表
                loadPhotos()
            } else {
                Toast.makeText(this, "删除失败：${photo.displayName}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "没有删除权限：${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "删除失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 分享单张照片或视频
     */
    private fun shareSinglePhoto(photo: PhotoItem) {
        try {
            val uri = Uri.parse(photo.uri)
            val mimeType = when (photo.mediaType) {
                PhotoItem.MediaType.VIDEO -> "video/*"
                PhotoItem.MediaType.IMAGE -> "image/*"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "分享媒体"))
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
