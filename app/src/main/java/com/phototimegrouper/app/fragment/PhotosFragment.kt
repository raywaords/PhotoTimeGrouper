package com.phototimegrouper.app.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.*
import com.phototimegrouper.app.databinding.FragmentPhotosBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotosFragment : Fragment() {
    private var _binding: FragmentPhotosBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_CODE_PERMISSIONS = 100
    private var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
    private var currentViewMode: ViewMode = ViewMode.LARGE_ICON
    private var photoGroupAdapter: PhotoGroupAdapter? = null
    private var photoDayAdapter: PhotoDayAdapter? = null
    
    // 缓存机制
    private var cachedPhotos: List<PhotoItem>? = null
    private var lastLoadTime: Long = 0
    private val CACHE_DURATION = 5 * 60 * 1000L // 5分钟缓存

    // 选择模式相关
    private var isSelectionMode = false
    private val selectedPhotos = mutableSetOf<Long>()

    // 收藏功能相关
    private val favoritePhotos = mutableSetOf<Long>()
    private var showFavoritesOnly: Boolean = false

    // 搜索筛选相关
    private var currentMediaTypeFilter: PhotoItem.MediaType? = null
    private var currentDaysFilter: Int? = null
    private var currentSizeFilter: SizeFilter? = null
    private var currentSearchQuery: String? = null
    private val searchQueryFlow = MutableStateFlow<String?>(null)

    // SharedPreferences（仅用于“照片”页自己的筛选/视图模式，不再和旧 MainActivity 共用同名 key）
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_VIEW_MODE = "photos_pref_view_mode"
    private val PREF_MEDIA_TYPE_FILTER = "photos_pref_media_type_filter"
    private val PREF_DAYS_FILTER = "photos_pref_days_filter"
    private val PREF_SIZE_FILTER = "photos_pref_size_filter"

    // Repository
    private lateinit var photoRepository: PhotoRepository

    // Permissions
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // 筛选条件Chips
    private val filterChips = mutableListOf<FilterChipItem>()
    private lateinit var filterChipAdapter: FilterChipAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化（使用 Application 的 prefs，确保与整个应用一致）
        photoRepository = PhotoRepository.getInstance(requireContext())
        sharedPreferences = requireContext().applicationContext.getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)

        // 加载偏好设置
        loadPreferences()

        // 设置UI
        setupRecyclerView()
        setupFilterChips()
        setupSearchView()
        setupSearchFlow()
        setupSwipeRefresh()
        setupSelectionMode()
        setupButtons()

        // 检查权限并加载照片
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        val hasFilter = currentMediaTypeFilter != null || currentDaysFilter != null ||
            currentSizeFilter != null || showFavoritesOnly || currentSearchQuery?.isNotBlank() == true
        val useCache = cachedPhotos != null &&
            System.currentTimeMillis() - lastLoadTime < CACHE_DURATION &&
            !hasFilter &&
            photoDayAdapter != null
        if (useCache) {
            return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            loadPhotos()
        }
    }

    private fun loadPreferences() {
        // 加载视图模式
        val savedViewModeName = sharedPreferences.getString(PREF_VIEW_MODE, ViewMode.LARGE_ICON.name)
        currentViewMode = try {
            ViewMode.valueOf(savedViewModeName ?: ViewMode.LARGE_ICON.name)
        } catch (e: Exception) {
            ViewMode.LARGE_ICON
        }

        // 加载筛选和排序偏好
        loadFilterAndSortPreferences()
    }

    private fun loadFilterAndSortPreferences() {
        val mediaTypeName = sharedPreferences.getString(PREF_MEDIA_TYPE_FILTER, null)
        currentMediaTypeFilter = mediaTypeName?.let {
            try {
                PhotoItem.MediaType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        val daysFilterRaw = sharedPreferences.getInt(PREF_DAYS_FILTER, -999)
        currentDaysFilter = when (daysFilterRaw) {
            7, 30, -1 -> daysFilterRaw
            else -> null
        }

        val sizeFilterName = sharedPreferences.getString(PREF_SIZE_FILTER, null)
        currentSizeFilter = sizeFilterName?.let {
            try {
                SizeFilter.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

    }

    private fun saveFilterAndSortPreferences() {
        val editor = sharedPreferences.edit()
        if (currentMediaTypeFilter != null) {
            editor.putString(PREF_MEDIA_TYPE_FILTER, currentMediaTypeFilter!!.name)
        } else {
            editor.remove(PREF_MEDIA_TYPE_FILTER)
        }
        if (currentDaysFilter != null) {
            editor.putInt(PREF_DAYS_FILTER, currentDaysFilter!!)
        } else {
            editor.remove(PREF_DAYS_FILTER)
        }
        if (currentSizeFilter != null) {
            editor.putString(PREF_SIZE_FILTER, currentSizeFilter!!.name)
        } else {
            editor.remove(PREF_SIZE_FILTER)
        }
        editor.commit()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupFilterChips() {
        filterChipAdapter = FilterChipAdapter(
            filterChips,
            onRemoveChip = { chip -> removeFilterChip(chip) },
            onOpenFilterClick = { showFilterDialog() }
        )
        binding.filterChipsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.filterChipsRecyclerView.adapter = filterChipAdapter
        updateFilterChips()
    }

    private fun updateFilterChips() {
        filterChips.clear()
        // 始终显示“筛选与排序”入口，方便用户找到筛选面板
        filterChips.add(FilterChipItem("open_filter", "筛选", FilterChipType.OPEN_FILTER))

        currentMediaTypeFilter?.let {
            filterChips.add(FilterChipItem("media_type", when(it) {
                PhotoItem.MediaType.IMAGE -> "仅照片"
                PhotoItem.MediaType.VIDEO -> "仅视频"
            }, FilterChipType.MEDIA_TYPE))
        }

        currentDaysFilter?.let {
            val label = when(it) {
                7 -> "最近7天"
                30 -> "最近30天"
                -1 -> "今年"
                else -> "最近${it}天"
            }
            filterChips.add(FilterChipItem("date_range", label, FilterChipType.DATE_RANGE))
        }

        currentSizeFilter?.let {
            filterChips.add(FilterChipItem("size", it.label, FilterChipType.SIZE))
        }

        filterChipAdapter.updateChips(filterChips)
        binding.filterChipsRecyclerView.visibility = View.VISIBLE
    }

    private fun removeFilterChip(chip: FilterChipItem) {
        when (chip.type) {
            FilterChipType.OPEN_FILTER -> { /* 入口芯片，不执行清除 */ }
            FilterChipType.MEDIA_TYPE -> currentMediaTypeFilter = null
            FilterChipType.DATE_RANGE -> currentDaysFilter = null
            FilterChipType.SIZE -> currentSizeFilter = null
            FilterChipType.SORT_ORDER -> { }
        }
        saveFilterAndSortPreferences()
        updateFilterChips()
        loadPhotos()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query?.takeIf { it.isNotBlank() }
                searchQueryFlow.value = currentSearchQuery
                loadPhotos()
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
            loadPhotos()
            false
        }
    }

    private fun setupSearchFlow() {
        searchQueryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                currentSearchQuery = query
                loadPhotos()
            }
            .launchIn(lifecycleScope)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPhotos()
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_700,
            R.color.teal_200,
            R.color.teal_700
        )
    }

    private fun setupSelectionMode() {
        binding.selectAllButton.setOnClickListener {
            if (selectedPhotos.size == allPhotosList.size) {
                selectedPhotos.clear()
                binding.selectAllButton.text = getString(R.string.select_all)
            } else {
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

    private fun setupButtons() {
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }

        binding.viewModeButton.setOnClickListener {
            toggleViewMode()
        }
    }

    private fun showFilterDialog() {
        // 由宿主传入当前筛选状态，弹窗只负责展示和回传选择结果
        val dialog = FilterBottomSheetDialog().apply {
            // 将当前 Fragment 中的筛选状态传给弹窗，用显式接收者避免同名属性自我赋值
            currentMediaType = this@PhotosFragment.currentMediaTypeFilter
            currentDaysFilter = this@PhotosFragment.currentDaysFilter
            currentSizeFilter = this@PhotosFragment.currentSizeFilter

            onApplyFilter = { mediaType, days, size, _ ->
                this@PhotosFragment.currentMediaTypeFilter = mediaType
                this@PhotosFragment.currentDaysFilter = days
                this@PhotosFragment.currentSizeFilter = size
                saveFilterAndSortPreferences()
                updateFilterChips()
                loadPhotos(daysFilter = days, mediaTypeFilter = mediaType, sizeFilter = size)
            }

            onClearFilter = {
                this@PhotosFragment.currentMediaTypeFilter = null
                this@PhotosFragment.currentDaysFilter = null
                this@PhotosFragment.currentSizeFilter = null
                saveFilterAndSortPreferences()
                updateFilterChips()
                loadPhotos()
            }
        }
        dialog.show(childFragmentManager, "FilterBottomSheet")
    }

    private fun toggleViewMode() {
        currentViewMode = when (currentViewMode) {
            ViewMode.LARGE_ICON -> ViewMode.SMALL_ICON
            ViewMode.SMALL_ICON -> ViewMode.LARGE_ICON
            else -> ViewMode.LARGE_ICON
        }
        sharedPreferences.edit().putString(PREF_VIEW_MODE, currentViewMode.name).apply()
        photoGroupAdapter?.updateViewMode(currentViewMode)
    }

    private fun checkPermissions() {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Fragment中需要使用Activity来请求权限
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else {
            // 权限已授予，直接加载
            loadPhotos()
            loadFavorites()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadPhotos()
                loadFavorites()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                Toast.makeText(requireContext(), "需要存储权限才能加载照片，请在设置中授予权限", Toast.LENGTH_LONG).show()
            }
        }
    }

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

    /**
     * @param daysFilter 若传入则优先使用（用于「应用」时立即生效），否则用 currentDaysFilter
     */
    private fun loadPhotos(
        daysFilter: Int? = null,
        mediaTypeFilter: PhotoItem.MediaType? = null,
        sizeFilter: SizeFilter? = null
    ) {
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }

        val daysFilterToApply = daysFilter ?: currentDaysFilter
        val mediaTypeToApply = mediaTypeFilter ?: currentMediaTypeFilter
        val sizeFilterToApply = sizeFilter ?: currentSizeFilter

        lifecycleScope.launch {
            try {
                val photos: List<PhotoItem> = if (currentSearchQuery != null && currentSearchQuery!!.isNotBlank()) {
                    val searchResults = withContext(Dispatchers.IO) {
                        photoRepository.searchPhotos(currentSearchQuery!!)
                    }
                    searchResults.map { entity ->
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
                    withContext(Dispatchers.IO) {
                        photoRepository.loadPhotosFromMediaStore()
                    }.also { loadedPhotos ->
                        withContext(Dispatchers.IO) {
                            photoRepository.syncMediaStoreToDatabase(loadedPhotos)
                            val favoriteIds = photoRepository.getFavoritePhotoIds()
                            favoritePhotos.clear()
                            favoritePhotos.addAll(favoriteIds)
                        }
                    }
                }

                // 缓存原始照片列表（仅在非搜索模式下）
                if (currentSearchQuery == null) {
                    cachedPhotos = photos
                    lastLoadTime = System.currentTimeMillis()
                }

                // 应用筛选条件
                var filteredPhotos = photos

                // 过滤已删除的
                val deletedIds = withContext(Dispatchers.IO) {
                    photoRepository.getDeletedPhotoIds()
                }
                if (deletedIds.isNotEmpty()) {
                    filteredPhotos = filteredPhotos.filterNot { deletedIds.contains(it.id) }
                }

                // 应用收藏筛选
                if (showFavoritesOnly) {
                    filteredPhotos = filteredPhotos.filter { favoritePhotos.contains(it.id) }
                }

                // 应用媒体类型筛选（使用调用 loadPhotos 时捕获的值）
                mediaTypeToApply?.let { mediaType ->
                    filteredPhotos = filteredPhotos.filter { it.mediaType == mediaType }
                }

                // 应用日期筛选（使用调用 loadPhotos 时捕获的值）
                daysFilterToApply?.let { days ->
                    val currentTime = System.currentTimeMillis() / 1000
                    filteredPhotos = filteredPhotos.filter { photo ->
                        when (days) {
                            -1 -> {
                                val photoYear = java.util.Calendar.getInstance().apply {
                                    timeInMillis = photo.dateAdded * 1000
                                }.get(java.util.Calendar.YEAR)
                                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                photoYear == currentYear
                            }
                            else -> {
                                val daysAgo = currentTime - (days * 24 * 60 * 60)
                                photo.dateAdded >= daysAgo
                            }
                        }
                    }
                }

                // 应用大小筛选
                filteredPhotos = FilterAndSortUtils.filterBySize(filteredPhotos, sizeFilterToApply)

                // 应用排序
                filteredPhotos = FilterAndSortUtils.sortPhotos(filteredPhotos, SortOrder.DATE_DESC)

                // 显示照片
                withContext(Dispatchers.Main) {
                    displayPhotos(filteredPhotos)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "加载照片失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayPhotos(filteredPhotos: List<PhotoItem>) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false

        if (filteredPhotos.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.viewModeButton.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.viewModeButton.visibility = View.GONE

            val groupedPhotos = groupPhotosByDate(filteredPhotos)
            val dateList = groupedPhotos.keys.sortedDescending()
            val flatPhotoList = dateList.flatMap { date -> groupedPhotos[date] ?: emptyList() }
            allPhotosList = ArrayList(flatPhotoList)
            (requireActivity().application as? PhotoTimeGrouperApp)?.setAllPhotosList(flatPhotoList)

            val days = dateList.map { date ->
                val photos = groupedPhotos[date] ?: emptyList()
                DayItem(
                    dateKey = date,
                    displayDate = DateFormatter.formatDateHeader(date),
                    count = photos.size,
                    coverUri = photos.firstOrNull()?.uri ?: ""
                )
            }
            photoDayAdapter = PhotoDayAdapter(days) { day ->
                (requireActivity().application as? PhotoTimeGrouperApp)?.setDayPhotosForDetail(
                    groupedPhotos[day.dateKey] ?: emptyList()
                )
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DayPhotosFragment.newInstance(day.dateKey, day.displayDate))
                    .addToBackStack(null)
                    .commit()
            }
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = photoDayAdapter
        }
    }

    private fun groupPhotosByDate(photoList: List<PhotoItem>): Map<String, List<PhotoItem>> {
        return photoList.groupBy { photo ->
            DateFormatter.formatDateForGroup(photo.dateModified)
        }
    }

    private fun enterSelectionMode(initialPhotoId: Long? = null) {
        isSelectionMode = true
        selectedPhotos.clear()
        initialPhotoId?.let { selectedPhotos.add(it) }
        binding.selectionActionBar.visibility = View.VISIBLE
        updateSelectionUI()
        photoGroupAdapter?.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        binding.selectionActionBar.visibility = View.GONE
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
            Toast.makeText(requireContext(), "请先选择要分享的照片", Toast.LENGTH_SHORT).show()
            return
        }

        val uris = selectedItems.map { Uri.parse(it.uri) }
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
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "分享媒体"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法分享: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedPhotos() {
        val selectedItems = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择要删除的照片", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "已移到回收站：${ids.size} 项", Toast.LENGTH_SHORT).show()
                exitSelectionMode()
                loadPhotos()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "移动到回收站失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleFavorite(photoId: Long) {
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
                    Toast.makeText(requireContext(), "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun isFavorite(photoId: Long): Boolean {
        return favoritePhotos.contains(photoId)
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
                Toast.makeText(requireContext(), "未找到视频播放器，请安装一个视频播放应用", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法播放视频: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                val intent = Intent(requireContext(), PhotoDetailActivity::class.java).apply {
                    putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法打开图片详情: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
