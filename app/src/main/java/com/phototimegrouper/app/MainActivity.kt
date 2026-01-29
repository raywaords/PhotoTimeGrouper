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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.widget.SearchView
import android.app.Activity

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_PERMISSIONS = 100
    private val REQUEST_CODE_DELETE_MEDIA = 101
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
    private var currentSizeFilter: SizeFilter? = null // 当前大小筛选
    private var currentSearchQuery: String? = null // 当前搜索关键词
    private val searchQueryFlow = MutableStateFlow<String?>(null) // 搜索查询 Flow（用于 debounce）
    
    // 排序相关
    private var currentSortOrder: SortOrder = SortOrder.DATE_DESC // 当前排序方式
    
    // SharedPreferences 用于持久化查看模式等配置（收藏现在使用 Room）
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_VIEW_MODE = "pref_view_mode"
    private val PREF_BROWSE_MODE = "pref_browse_mode" // 浏览模式：TIME 或 FOLDER
    private val PREF_SHOW_FAVORITES_ON_RESUME = "pref_show_favorites_on_resume" // 从其他Activity返回后是否显示收藏
    private val PREF_RETURN_TO_FOLDER_BROWSE = "pref_return_to_folder_browse" // 从收藏视图返回时是否应该回到文件夹浏览
    private val PREF_MEDIA_TYPE_FILTER = "pref_media_type_filter" // 媒体类型筛选偏好
    private val PREF_DAYS_FILTER = "pref_days_filter" // 日期筛选偏好
    private val PREF_SIZE_FILTER = "pref_size_filter" // 大小筛选偏好
    private val PREF_SORT_ORDER = "pref_sort_order" // 排序方式偏好
    private var isFolderBrowseMode: Boolean = false // 是否处于文件夹浏览模式
    
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

        // 初始化 Repository（数据访问层）
        photoRepository = PhotoRepository.getInstance(this)

        // 初始�?SharedPreferences
        sharedPreferences = getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)
        
        // �?SharedPreferences 加载上次的查看模�?
        val savedViewModeName = sharedPreferences.getString(PREF_VIEW_MODE, ViewMode.LARGE_ICON.name)
        currentViewMode = try {
            ViewMode.valueOf(savedViewModeName ?: ViewMode.LARGE_ICON.name)
        } catch (e: Exception) {
            ViewMode.LARGE_ICON
        }
        
        // 从 SharedPreferences 加载浏览模式
        isFolderBrowseMode = sharedPreferences.getBoolean(PREF_BROWSE_MODE, false)
        
        // 从 SharedPreferences 加载筛选和排序偏好
        loadFilterAndSortPreferences()

        // 加载收藏列表
        loadFavorites()
        
        // TODO: Demo版本 - 临时添加按钮启动新UI
        // 在实际使用中，可以移除这个按钮，直接使用MainActivityNew作为启动Activity
        binding.viewModeMenuButton.setOnLongClickListener {
            val intent = Intent(this, MainActivityNew::class.java)
            startActivity(intent)
            true
        }
        
        setupSwipeRefresh()
        setupViewModeMenu()
        setupSelectionMode()
        setupSearchView()
        setupSearchFlow()
        updateFavoritesViewUI() // 初始化UI状态
    }
    
    override fun onResume() {
        super.onResume()
        // 从其他Activity返回时，重新读取浏览模式状态，确保菜单文字正确
        val savedBrowseMode = sharedPreferences.getBoolean(PREF_BROWSE_MODE, false)
        if (isFolderBrowseMode != savedBrowseMode) {
            isFolderBrowseMode = savedBrowseMode
        }
        
        // 检查是否需要显示收藏（从FolderListActivity返回时设置）
        val shouldShowFavorites = sharedPreferences.getBoolean(PREF_SHOW_FAVORITES_ON_RESUME, false)
        if (shouldShowFavorites) {
            sharedPreferences.edit().putBoolean(PREF_SHOW_FAVORITES_ON_RESUME, false).apply()
            // 如果当前不在收藏视图，切换到收藏视图
            if (!showFavoritesOnly) {
                toggleFavoritesFilter()
            }
        }
        
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
    
    private var pendingDeleteCount: Int = 0

    private fun deleteSelectedPhotos() {
        val selectedItems = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的照片", Toast.LENGTH_SHORT).show()
            return
        }

        // 回收站模式：软删除，标记为已删除，可在回收站恢复
        AlertDialog.Builder(this)
            .setTitle("移到回收站")
            .setMessage("${getString(R.string.delete_confirm, selectedItems.size)}\n照片/视频将被移到回收站，可在 30 天内恢复。")
            .setPositiveButton("移到回收站") { _, _ ->
                moveToRecycleBin(selectedItems)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun moveToRecycleBin(photosToDelete: List<PhotoItem>) {
        lifecycleScope.launch {
            try {
                val ids = photosToDelete.map { it.id }
                photoRepository.softDeletePhotos(ids)
                Toast.makeText(
                    this@MainActivity,
                    "已移到回收站：${ids.size} 项",
                    Toast.LENGTH_SHORT
                ).show()
                // 退出选择模式并刷新列表（从主视图中隐藏这些项目）
                exitSelectionMode()
                loadPhotos()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "移动到回收站失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Android 11+ 使用系统删除对话框，Android 10 及以下使用旧的 ContentResolver.delete 流程
     */
    private fun requestDeleteWithSystemDialog(photosToDelete: List<PhotoItem>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uris = photosToDelete.map { Uri.parse(it.uri) }
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                pendingDeleteCount = photosToDelete.size
                startIntentSenderForResult(
                    pendingIntent.intentSender,
                    REQUEST_CODE_DELETE_MEDIA,
                    null,
                    0,
                    0,
                    0
                )
            } catch (e: Exception) {
                Toast.makeText(this, "无法请求系统删除: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 10 及以下仍然使用旧的删除方式
            performDeleteLegacy(photosToDelete)
        }
    }

    private fun performDeleteLegacy(photosToDelete: List<PhotoItem>) {
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
            
            // 每次打开菜单时，重新读取当前状态（可能从其他Activity返回后状态已更新）
            val currentBrowseMode = sharedPreferences.getBoolean(PREF_BROWSE_MODE, false)
            isFolderBrowseMode = currentBrowseMode
            
            // Fix bug #1: Handle favorites view mode
            if (showFavoritesOnly) {
                popupMenu.menu.findItem(R.id.menu_privacy_policy).isVisible = false
                popupMenu.menu.findItem(R.id.menu_search).isVisible = false
                popupMenu.menu.findItem(R.id.menu_recycle_bin_demo).isVisible = false
                popupMenu.menu.findItem(R.id.menu_browse_folders).isVisible = false
                popupMenu.menu.findItem(R.id.menu_extra_large_icon).isVisible = false
                popupMenu.menu.findItem(R.id.menu_large_icon).isVisible = false
                popupMenu.menu.findItem(R.id.menu_small_icon).isVisible = false
                popupMenu.menu.findItem(R.id.menu_details).isVisible = false
                popupMenu.menu.findItem(R.id.menu_show_favorites)?.let { item ->
                    item.title = getString(R.string.go_back)
                }
            } else {
                // Fix bug #3: Update browse mode menu text based on current state
                // 如果当前是文件夹浏览模式，菜单显示"按时间浏览"（点击后切换回时间浏览）
                // 如果当前是时间浏览模式，菜单显示"按文件夹浏览"（点击后切换到文件夹浏览）
                popupMenu.menu.findItem(R.id.menu_browse_folders)?.let { item ->
                    if (isFolderBrowseMode) {
                        item.title = getString(R.string.browse_by_time)
                    } else {
                        item.title = getString(R.string.browse_folders)
                    }
                }
            }
            
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
                        if (showFavoritesOnly) {
                            // 如果当前在收藏视图，点击返回
                            toggleFavoritesFilter() // 先关闭收藏视图
                            // 检查是否需要返回到文件夹浏览
                            val shouldReturnToFolder = sharedPreferences.getBoolean(PREF_RETURN_TO_FOLDER_BROWSE, false)
                            if (shouldReturnToFolder) {
                                sharedPreferences.edit().putBoolean(PREF_RETURN_TO_FOLDER_BROWSE, false).apply()
                                // 返回到文件夹浏览：不finish() MainActivity，而是启动FolderListActivity
                                // 这样Activity栈保持：FolderListActivity -> MainActivity
                                // 当用户点击"按时间浏览"时，FolderListActivity finish()后，MainActivity还在栈中
                                val intent = Intent(this, FolderListActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                // 不调用finish()，保持MainActivity在栈中
                            }
                        } else {
                            toggleFavoritesFilter()
                        }
                        true
                    }
                    R.id.menu_search -> {
                        showSearchDialog()
                        true
                    }
                    R.id.menu_recycle_bin_demo -> {
                        openRecycleBinDemo()
                        true
                    }
                    R.id.menu_browse_folders -> {
                        // 使用当前状态（已在菜单打开时更新）
                        if (isFolderBrowseMode) {
                            // 如果当前是文件夹浏览模式，切换回时间浏览
                            isFolderBrowseMode = false
                            sharedPreferences.edit().putBoolean(PREF_BROWSE_MODE, false).apply()
                            loadPhotos()
                        } else {
                            // 如果当前是时间浏览模式，切换到文件夹浏览
                            isFolderBrowseMode = true
                            sharedPreferences.edit().putBoolean(PREF_BROWSE_MODE, true).apply()
                            val intent = Intent(this@MainActivity, FolderListActivity::class.java)
                            startActivity(intent)
                        }
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
        }
    }

    private fun openRecycleBinDemo() {
        try {
            val intent = Intent(this, RecycleBinDemoActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开回收站预览: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFolderList() {
        // This method is kept for compatibility, but menu_browse_folders now handles it directly
        try {
            isFolderBrowseMode = true
            sharedPreferences.edit().putBoolean(PREF_BROWSE_MODE, true).apply()
            val intent = Intent(this, FolderListActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件夹列表: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 从 SharedPreferences 加载筛选和排序偏好
     */
    private fun loadFilterAndSortPreferences() {
        // 加载媒体类型筛选
        val mediaTypeName = sharedPreferences.getString(PREF_MEDIA_TYPE_FILTER, null)
        currentMediaTypeFilter = mediaTypeName?.let {
            try {
                PhotoItem.MediaType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        
        // 加载日期筛选
        val daysFilter = sharedPreferences.getInt(PREF_DAYS_FILTER, -999) // -999表示未设置
        currentDaysFilter = if (daysFilter != -999) daysFilter else null
        
        // 加载大小筛选
        val sizeFilterName = sharedPreferences.getString(PREF_SIZE_FILTER, null)
        currentSizeFilter = sizeFilterName?.let {
            try {
                SizeFilter.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        
        // 加载排序方式
        val sortOrderName = sharedPreferences.getString(PREF_SORT_ORDER, SortOrder.DATE_DESC.name)
        currentSortOrder = try {
            SortOrder.valueOf(sortOrderName ?: SortOrder.DATE_DESC.name)
        } catch (e: Exception) {
            SortOrder.DATE_DESC
        }
    }
    
    /**
     * 保存筛选和排序偏好到 SharedPreferences
     */
    private fun saveFilterAndSortPreferences() {
        sharedPreferences.edit().apply {
            // 保存媒体类型筛选
            if (currentMediaTypeFilter != null) {
                putString(PREF_MEDIA_TYPE_FILTER, currentMediaTypeFilter!!.name)
            } else {
                remove(PREF_MEDIA_TYPE_FILTER)
            }
            
            // 保存日期筛选
            if (currentDaysFilter != null) {
                putInt(PREF_DAYS_FILTER, currentDaysFilter!!)
            } else {
                remove(PREF_DAYS_FILTER)
            }
            
            // 保存大小筛选
            if (currentSizeFilter != null) {
                putString(PREF_SIZE_FILTER, currentSizeFilter!!.name)
            } else {
                remove(PREF_SIZE_FILTER)
            }
            
            // 保存排序方式
            putString(PREF_SORT_ORDER, currentSortOrder.name)
            
            apply()
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
        updateFavoritesViewUI() // 更新UI（标题栏和搜索栏显示/隐藏）
        loadPhotos() // 重新加载照片，应用筛选
    }
    
    private fun updateFavoritesViewUI() {
        if (showFavoritesOnly) {
            // 显示收藏视图：显示标题栏，隐藏搜索栏
            binding.titleTextView.visibility = View.VISIBLE
            binding.titleTextView.text = getString(R.string.favorites_title)
            binding.searchView.visibility = View.GONE
        } else {
            // 正常视图：隐藏标题栏，显示搜索栏
            binding.titleTextView.visibility = View.GONE
            binding.searchView.visibility = View.VISIBLE
        }
    }
    
    /**
     * 设置搜索栏
     */
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 用户按下搜索按钮时，直接执行搜索（不等待 debounce）
                currentSearchQuery = query?.takeIf { it.isNotBlank() }
                searchQueryFlow.value = currentSearchQuery
                loadPhotos()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // 实时搜索（通过 Flow debounce）
                val query = newText?.takeIf { it.isNotBlank() }
                searchQueryFlow.value = query
                return true
            }
        })
        
        binding.searchView.setOnCloseListener {
            // 关闭搜索时，清空搜索条件
            currentSearchQuery = null
            searchQueryFlow.value = null
            loadPhotos()
            false
        }
    }
    
    /**
     * 设置搜索 Flow（debounce 300ms）
     */
    private fun setupSearchFlow() {
        searchQueryFlow
            .debounce(300) // 300ms 防抖
            .distinctUntilChanged() // 避免重复搜索相同关键词
            .onEach { query ->
                // 搜索关键词变化时，更新并重新加载
                currentSearchQuery = query
                loadPhotos()
            }
            .launchIn(lifecycleScope)
    }
    
    private fun showSearchDialog() {
        // 增强的筛选和排序对话框
        val options = mutableListOf<String>()
        
        // 筛选选项
        options.add("━━━ 筛选 ━━━")
        options.add("全部（清除筛选）")
        options.add("仅照片")
        options.add("仅视频")
        options.add("最近7天")
        options.add("最近30天")
        options.add("今年")
        options.add("━━━ 按大小筛选 ━━━")
        options.add("小于100KB")
        options.add("100KB - 1MB")
        options.add("1MB - 10MB")
        options.add("10MB - 100MB")
        options.add("大于100MB")
        options.add("━━━ 排序 ━━━")
        options.add("日期（新到旧）")
        options.add("日期（旧到新）")
        options.add("名称（A-Z）")
        options.add("名称（Z-A）")
        options.add("大小（大到小）")
        options.add("大小（小到大）")
        
        AlertDialog.Builder(this)
            .setTitle("筛选和排序")
            .setItems(options.toTypedArray()) { _, which ->
                when {
                    which == 1 -> filterPhotos(null, null, null) // 全部（清除筛选）
                    which == 2 -> filterPhotos(PhotoItem.MediaType.IMAGE, null, null) // 仅照片
                    which == 3 -> filterPhotos(PhotoItem.MediaType.VIDEO, null, null) // 仅视频
                    which == 4 -> filterPhotos(null, 7, null) // 最近7天
                    which == 5 -> filterPhotos(null, 30, null) // 最近30天
                    which == 6 -> filterPhotos(null, -1, null) // 今年
                    which == 8 -> filterPhotos(currentMediaTypeFilter, currentDaysFilter, SizeFilter.TINY) // 小于100KB
                    which == 9 -> filterPhotos(currentMediaTypeFilter, currentDaysFilter, SizeFilter.SMALL) // 100KB - 1MB
                    which == 10 -> filterPhotos(currentMediaTypeFilter, currentDaysFilter, SizeFilter.MEDIUM) // 1MB - 10MB
                    which == 11 -> filterPhotos(currentMediaTypeFilter, currentDaysFilter, SizeFilter.LARGE) // 10MB - 100MB
                    which == 12 -> filterPhotos(currentMediaTypeFilter, currentDaysFilter, SizeFilter.HUGE) // 大于100MB
                    which == 14 -> setSortOrder(SortOrder.DATE_DESC) // 日期（新到旧）
                    which == 15 -> setSortOrder(SortOrder.DATE_ASC) // 日期（旧到新）
                    which == 16 -> setSortOrder(SortOrder.NAME_ASC) // 名称（A-Z）
                    which == 17 -> setSortOrder(SortOrder.NAME_DESC) // 名称（Z-A）
                    which == 18 -> setSortOrder(SortOrder.SIZE_DESC) // 大小（大到小）
                    which == 19 -> setSortOrder(SortOrder.SIZE_ASC) // 大小（小到大）
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun filterPhotos(mediaType: PhotoItem.MediaType?, days: Int?, sizeFilter: SizeFilter? = null) {
        // 设置筛选条件
        currentMediaTypeFilter = mediaType
        currentDaysFilter = days
        if (sizeFilter != null) {
            currentSizeFilter = sizeFilter
        }
        
        // 保存偏好
        saveFilterAndSortPreferences()
        
        // 重新加载照片，应用筛选
        loadPhotos()
    }
    
    /**
     * 设置排序方式
     */
    private fun setSortOrder(sortOrder: SortOrder) {
        currentSortOrder = sortOrder
        saveFilterAndSortPreferences()
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
                val photos: List<PhotoItem>
                
                // 如果有搜索关键词，从数据库搜索；否则从 MediaStore 加载
                if (currentSearchQuery != null && currentSearchQuery!!.isNotBlank()) {
                    // 搜索模式：从数据库搜索
                    val searchResults = withContext(Dispatchers.IO) {
                        photoRepository.searchPhotos(currentSearchQuery!!)
                    }
                    
                    // 将 PhotoMetadataEntity 转换为 PhotoItem
                    photos = searchResults.map { entity ->
                        PhotoItem(
                            id = entity.id,
                            uri = entity.uri,
                            displayName = entity.displayName,
                            dateAdded = entity.dateAdded,
                            dateModified = entity.dateModified,
                            size = entity.size,
                            width = entity.width,
                            height = entity.height,
                            mimeType = entity.mimeType,
                            bucketDisplayName = entity.bucketDisplayName,
                            data = entity.data,
                            mediaType = PhotoItem.MediaType.valueOf(entity.mediaType),
                            iso = entity.iso
                        )
                    }
                } else {
                    // 正常模式：从 MediaStore 加载
                    photos = withContext(Dispatchers.IO) {
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
                }

                // 应用筛选条件
                var filteredPhotos = photos

                // 0. 根据数据库中的删除状态过滤（回收站中的项目不在主列表中显示）
                val deletedIds = withContext(Dispatchers.IO) {
                    photoRepository.getDeletedPhotoIds()
                }
                if (deletedIds.isNotEmpty()) {
                    filteredPhotos = filteredPhotos.filterNot { deletedIds.contains(it.id) }
                }
                
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
                
                // 4. 应用大小筛选
                filteredPhotos = FilterAndSortUtils.filterBySize(filteredPhotos, currentSizeFilter)
                
                // 5. 应用排序
                filteredPhotos = FilterAndSortUtils.sortPhotos(filteredPhotos, currentSortOrder)
                
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
                        isPhotoSelected = { photoId -> this@MainActivity.isPhotoSelected(photoId) },
                        isFavorite = { photoId -> this@MainActivity.isFavorite(photoId) },
                        onToggleFavorite = { photoId -> this@MainActivity.toggleFavorite(photoId) }
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
     * 删除单张照片或视频（带确认弹窗）
     */
    private fun deleteSinglePhoto(photo: PhotoItem) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除“${photo.displayName}”吗？\n此操作不可恢复，将从设备中永久删除该照片/视频。")
            .setPositiveButton("删除") { _, _ ->
                requestDeleteWithSystemDialog(listOf(photo))
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DELETE_MEDIA) {
            if (resultCode == Activity.RESULT_OK) {
                // 用户在系统对话框中确认删除
                Toast.makeText(
                    this,
                    getString(R.string.delete_success, pendingDeleteCount),
                    Toast.LENGTH_SHORT
                ).show()
                pendingDeleteCount = 0
                // 退出选择模式并刷新列表
                exitSelectionMode()
                loadPhotos()
            } else {
                // 用户取消或删除失败
                Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
                pendingDeleteCount = 0
            }
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
