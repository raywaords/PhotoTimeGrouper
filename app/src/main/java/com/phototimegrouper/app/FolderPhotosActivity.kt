package com.phototimegrouper.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.databinding.ActivityFolderPhotosBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.widget.SearchView
import java.util.ArrayList

/**
 * 某个文件夹内的照片列表页，复用按日期分组的列表展示
 */
class FolderPhotosActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOLDER_NAME = "extra_folder_name"
        private const val PREF_VIEW_MODE = "pref_view_mode"
    }

    private lateinit var binding: ActivityFolderPhotosBinding
    private lateinit var photoRepository: PhotoRepository
    private lateinit var sharedPreferences: SharedPreferences
    private var folderName: String = ""
    private var photoGroupAdapter: PhotoGroupAdapter? = null
    private var allPhotosList: List<PhotoItem> = emptyList()
    private var currentViewMode: ViewMode = ViewMode.LARGE_ICON

    // 选择模式相关
    private var isSelectionMode = false
    private val selectedPhotos = mutableSetOf<Long>()

    // 收藏功能相关
    private val favoritePhotos = mutableSetOf<Long>()
    
    // 搜索筛选相关
    private var currentSearchQuery: String? = null
    private val searchQueryFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderPhotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        photoRepository = PhotoRepository.getInstance(this)
        folderName = intent.getStringExtra(EXTRA_FOLDER_NAME) ?: ""

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)

        // 从 SharedPreferences 加载上次的查看模式（复用主界面的设置）
        val savedViewModeName = sharedPreferences.getString(PREF_VIEW_MODE, ViewMode.LARGE_ICON.name)
        currentViewMode = try {
            ViewMode.valueOf(savedViewModeName ?: ViewMode.LARGE_ICON.name)
        } catch (e: Exception) {
            ViewMode.LARGE_ICON
        }

        setupToolbar()
        setupViewModeMenu()
        setupSearchView()
        setupSearchFlow()
        setupSelectionMode()
        setupRecyclerView()
        loadFavorites()
        loadFolderPhotos()
    }

    private fun setupToolbar() {
        binding.toolbarBack.setOnClickListener { finish() }
        binding.toolbarTitle.text = folderName
        binding.toolbarSubtitle.text = "仅显示该文件夹下的照片/视频"
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadFolderPhotos() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val photos = withContext(Dispatchers.IO) {
                    val all = photoRepository.loadPhotosFromMediaStore()
                    var filtered = all.filter { photo ->
                        val bucket = photo.bucketDisplayName.ifBlank { "未知文件夹" }
                        bucket == folderName
                    }
                    // Apply search filter if exists
                    currentSearchQuery?.let { query ->
                        if (query.isNotBlank()) {
                            filtered = filtered.filter { photo ->
                                photo.displayName.contains(query, ignoreCase = true)
                            }
                        }
                    }
                    filtered
                }

                // 按日期分组
                val groupedPhotos = groupPhotosByDate(photos)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE

                    if (photos.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE

                        // 将照片列表存到 Application，供详情页使用
                        (application as? PhotoTimeGrouperApp)?.setAllPhotosList(photos)
                        allPhotosList = photos

                        photoGroupAdapter = PhotoGroupAdapter(
                            context = this@FolderPhotosActivity,
                            groupedPhotos = groupedPhotos,
                            viewMode = currentViewMode,
                            isSelectionMode = { isSelectionMode },
                            onPhotoClick = { list, position ->
                                if (isSelectionMode) {
                                    val photo = list.getOrNull(position)
                                    photo?.let { togglePhotoSelection(it.id) }
                                } else {
                                    openPhotoDetail(list, position)
                                }
                            },
                            onPhotoLongClick = { list, position ->
                                val photo = list.getOrNull(position)
                                photo?.let { enterSelectionMode(it.id) }
                            },
                            isPhotoSelected = { photoId -> isPhotoSelected(photoId) },
                            isFavorite = { photoId -> isFavorite(photoId) },
                            onToggleFavorite = { photoId -> toggleFavorite(photoId) }
                        )
                        binding.recyclerView.adapter = photoGroupAdapter
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@FolderPhotosActivity,
                        "加载文件夹内容失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun groupPhotosByDate(photoList: List<PhotoItem>): Map<String, List<PhotoItem>> {
        return photoList.groupBy { photo ->
            DateFormatter.formatDateForGroup(photo.dateModified)
        }
    }

    private fun openPhotoDetail(photosList: List<PhotoItem>, position: Int) {
        val photo = photosList.getOrNull(position) ?: return

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
            try {
                val indexInAll = allPhotosList.indexOfFirst { it.id == photo.id }
                val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                    putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, indexInAll)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开图片详情: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViewModeMenu() {
        binding.viewModeMenuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.view_mode_menu, popupMenu.menu)

            // 设置当前选中项
            when (currentViewMode) {
                ViewMode.EXTRA_LARGE_ICON -> popupMenu.menu.findItem(R.id.menu_extra_large_icon).isChecked = true
                ViewMode.LARGE_ICON -> popupMenu.menu.findItem(R.id.menu_large_icon).isChecked = true
                ViewMode.SMALL_ICON -> popupMenu.menu.findItem(R.id.menu_small_icon).isChecked = true
                ViewMode.DETAILS -> popupMenu.menu.findItem(R.id.menu_details).isChecked = true
            }

            // 设置为单选模式
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
                    R.id.menu_recycle_bin_demo -> {
                        openRecycleBinDemo()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private fun updateViewMode(newViewMode: ViewMode) {
        if (currentViewMode != newViewMode) {
            currentViewMode = newViewMode

            // 保存到 SharedPreferences（复用主界面的设置）
            sharedPreferences.edit()
                .putString(PREF_VIEW_MODE, newViewMode.name)
                .apply()

            // 更新适配器
            photoGroupAdapter?.updateViewMode(newViewMode)
        }
    }

    // ====== 多选相关 ======

    private fun setupSelectionMode() {
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

        binding.shareButton.setOnClickListener {
            shareSelectedPhotos()
        }

        binding.deleteButton.setOnClickListener {
            deleteSelectedPhotos()
        }

        binding.cancelSelectionButton.setOnClickListener {
            exitSelectionMode()
        }
    }

    private fun enterSelectionMode(initialPhotoId: Long? = null) {
        isSelectionMode = true
        selectedPhotos.clear()
        initialPhotoId?.let { selectedPhotos.add(it) }

        // 显示操作栏，隐藏查看方式菜单按钮
        binding.selectionActionBar.visibility = View.VISIBLE
        binding.viewModeMenuButton.visibility = View.GONE

        updateSelectionUI()
        photoGroupAdapter?.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()

        // 隐藏操作栏，显示查看方式菜单按钮
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

        val hasSelection = count > 0
        binding.shareButton.isEnabled = hasSelection
        binding.deleteButton.isEnabled = hasSelection
    }

    private fun isPhotoSelected(photoId: Long): Boolean {
        return selectedPhotos.contains(photoId)
    }

    private fun togglePhotoSelection(photoId: Long) {
        if (selectedPhotos.contains(photoId)) {
            selectedPhotos.remove(photoId)
        } else {
            selectedPhotos.add(photoId)
        }
        updateSelectionUI()
        photoGroupAdapter?.notifyDataSetChanged()
    }

    // ====== 收藏相关 ======

    private fun loadFavorites() {
        lifecycleScope.launch {
            try {
                val favoriteIds = photoRepository.getFavoritePhotoIds()
                favoritePhotos.clear()
                favoritePhotos.addAll(favoriteIds)
            } catch (e: Exception) {
                favoritePhotos.clear()
            }
        }
    }

    private fun isFavorite(photoId: Long): Boolean {
        return favoritePhotos.contains(photoId)
    }

    private fun toggleFavorite(photoId: Long) {
        lifecycleScope.launch {
            try {
                val newState = photoRepository.toggleFavorite(photoId)
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
                    Toast.makeText(
                        this@FolderPhotosActivity,
                        "操作失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ====== 分享 & 删除（移到回收站） ======

    private fun shareSelectedPhotos() {
        val selectedItems = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "请先选择要分享的照片", Toast.LENGTH_SHORT).show()
            return
        }

        val uris = ArrayList(selectedItems.map { Uri.parse(it.uri) })

        val hasVideo = selectedItems.any { it.mediaType == PhotoItem.MediaType.VIDEO }
        val hasImage = selectedItems.any { it.mediaType == PhotoItem.MediaType.IMAGE }

        val mimeType = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
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
            .setTitle("移到回收站")
            .setMessage("${getString(R.string.delete_confirm, selectedItems.size)}\n照片/视频将被移到回收站，可在 30 天内恢复。")
            .setPositiveButton("移到回收站") { _, _ ->
                moveToRecycleBinFolder(selectedItems)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun moveToRecycleBinFolder(photosToDelete: List<PhotoItem>) {
        lifecycleScope.launch {
            try {
                val ids = photosToDelete.map { it.id }
                photoRepository.softDeletePhotos(ids)
                Toast.makeText(
                    this@FolderPhotosActivity,
                    "已移到回收站：${ids.size} 项",
                    Toast.LENGTH_SHORT
                ).show()
                exitSelectionMode()
                loadFolderPhotos()
            } catch (e: Exception) {
                Toast.makeText(
                    this@FolderPhotosActivity,
                    "移动到回收站失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    // ====== 搜索功能 ======
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query?.takeIf { it.isNotBlank() }
                searchQueryFlow.value = currentSearchQuery
                loadFolderPhotos()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.takeIf { it.isNotBlank() }
                searchQueryFlow.value = query
                return true
            }
        })
        
        binding.searchView.setOnCloseListener {
            currentSearchQuery = null
            searchQueryFlow.value = null
            loadFolderPhotos()
            false
        }
    }
    
    private fun setupSearchFlow() {
        searchQueryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                currentSearchQuery = query
                loadFolderPhotos()
            }
            .launchIn(lifecycleScope)
    }
    
    // ====== 菜单功能 ======
    
    private fun openPrivacyPolicy() {
        val privacyPolicyUrl = "https://raywaords.github.io/PhotoTimeGrouper/privacy-policy.html"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(privacyPolicyUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开隐私政策页面: ${e.message}", Toast.LENGTH_SHORT).show()
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
}
