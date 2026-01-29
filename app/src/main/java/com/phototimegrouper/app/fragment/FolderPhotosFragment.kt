package com.phototimegrouper.app.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.*
import com.phototimegrouper.app.databinding.FragmentPhotosBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 显示指定文件夹内的照片
 * 复用PhotosFragment的布局和大部分逻辑
 */
class FolderPhotosFragment : Fragment() {
    private var _binding: FragmentPhotosBinding? = null
    private val binding get() = _binding!!

    private var folderName: String? = null
    private var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
    private var currentViewMode: ViewMode = ViewMode.LARGE_ICON
    private var photoGroupAdapter: PhotoGroupAdapter? = null

    // 选择模式相关
    private var isSelectionMode = false
    private val selectedPhotos = mutableSetOf<Long>()

    // 收藏功能相关
    private val favoritePhotos = mutableSetOf<Long>()

    // 筛选（相册内，不提供排序）
    private var currentMediaTypeFilter: PhotoItem.MediaType? = null
    private var currentDaysFilter: Int? = null

    private val filterChips = mutableListOf<FilterChipItem>()
    private lateinit var filterChipAdapter: FilterChipAdapter

    private lateinit var photoRepository: PhotoRepository
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_VIEW_MODE = "pref_view_mode"

    companion object {
        private const val ARG_FOLDER_NAME = "folder_name"

        fun newInstance(folderName: String): FolderPhotosFragment {
            return FolderPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FOLDER_NAME, folderName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        folderName = arguments?.getString(ARG_FOLDER_NAME)
    }

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

        photoRepository = PhotoRepository.getInstance(requireContext())
        sharedPreferences = requireContext().getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)

        // 加载视图模式
        val savedViewModeName = sharedPreferences.getString(PREF_VIEW_MODE, ViewMode.LARGE_ICON.name)
        currentViewMode = try {
            ViewMode.valueOf(savedViewModeName ?: ViewMode.LARGE_ICON.name)
        } catch (e: Exception) {
            ViewMode.LARGE_ICON
        }

        // 文件夹视图隐藏搜索框，但保留其占位，使筛选按钮始终在右侧
        binding.searchView.visibility = View.INVISIBLE

        // 文件夹内不再提供“查看方式/眼睛”图标，仅保留筛选入口
        binding.viewModeButton.visibility = View.GONE

        setupFilterChips()

        // 设置标题
        (activity as? MainActivityNew)?.supportActionBar?.title = folderName ?: "相册"

        setupRecyclerView()
        setupSwipeRefresh()
        setupSelectionMode()
        setupButtons()

        loadFolderPhotos()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadFolderPhotos()
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
        binding.filterButton.setOnClickListener { showFilterDialog() }
        binding.viewModeButton.setOnClickListener {
            toggleViewMode()
        }
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
        filterChips.add(FilterChipItem("open_filter", "筛选", FilterChipType.OPEN_FILTER))
        currentMediaTypeFilter?.let {
            filterChips.add(FilterChipItem("media_type", when (it) {
                PhotoItem.MediaType.IMAGE -> "仅照片"
                PhotoItem.MediaType.VIDEO -> "仅视频"
            }, FilterChipType.MEDIA_TYPE))
        }
        currentDaysFilter?.let {
            val label = when (it) {
                7 -> "最近7天"
                30 -> "最近30天"
                -1 -> "今年"
                else -> "最近${it}天"
            }
            filterChips.add(FilterChipItem("date_range", label, FilterChipType.DATE_RANGE))
        }
        filterChipAdapter.updateChips(filterChips)
        binding.filterChipsRecyclerView.visibility = View.VISIBLE
    }

    private fun removeFilterChip(chip: FilterChipItem) {
        when (chip.type) {
            FilterChipType.OPEN_FILTER -> { }
            FilterChipType.MEDIA_TYPE -> currentMediaTypeFilter = null
            FilterChipType.DATE_RANGE -> currentDaysFilter = null
            FilterChipType.SIZE -> { }
            FilterChipType.SORT_ORDER -> { }
        }
        updateFilterChips()
        loadFolderPhotos()
    }

    private fun showFilterDialog() {
        val dialog = FilterBottomSheetDialog().apply {
            currentMediaType = this@FolderPhotosFragment.currentMediaTypeFilter
            currentDaysFilter = this@FolderPhotosFragment.currentDaysFilter
            currentSizeFilter = null
            onApplyFilter = { mediaType, days, _, _ ->
                this@FolderPhotosFragment.currentMediaTypeFilter = mediaType
                this@FolderPhotosFragment.currentDaysFilter = days
                updateFilterChips()
                loadFolderPhotos()
            }
            onClearFilter = {
                this@FolderPhotosFragment.currentMediaTypeFilter = null
                this@FolderPhotosFragment.currentDaysFilter = null
                updateFilterChips()
                loadFolderPhotos()
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

    private fun loadFolderPhotos() {
        if (folderName == null) {
            Toast.makeText(requireContext(), "文件夹名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val allPhotos = withContext(Dispatchers.IO) {
                    photoRepository.loadPhotosFromMediaStore()
                }

                // 过滤指定文件夹的照片（“未知文件夹”对应 bucketDisplayName 为空）
                val folderPhotos = allPhotos.filter {
                    if (folderName == "未知文件夹") it.bucketDisplayName.isBlank()
                    else it.bucketDisplayName == folderName
                }

                // 过滤已删除的
                val deletedIds = withContext(Dispatchers.IO) {
                    photoRepository.getDeletedPhotoIds()
                }
                var filteredPhotos = folderPhotos.filterNot { deletedIds.contains(it.id) }

                // 应用媒体类型筛选
                currentMediaTypeFilter?.let { mediaType ->
                    filteredPhotos = filteredPhotos.filter { it.mediaType == mediaType }
                }
                // 应用日期范围筛选
                currentDaysFilter?.let { days ->
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
                // 按日期降序（与分组一致，不再提供排序选项）
                filteredPhotos = FilterAndSortUtils.sortPhotos(filteredPhotos, SortOrder.DATE_DESC)

                val groupedPhotos = groupPhotosByDate(filteredPhotos)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (filteredPhotos.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE

                        val dateList = groupedPhotos.keys.sortedDescending()
                        val flatPhotoList = dateList.flatMap { date ->
                            groupedPhotos[date] ?: emptyList()
                        }
                        allPhotosList = ArrayList(flatPhotoList)

                        photoGroupAdapter = PhotoGroupAdapter(
                            requireContext(),
                            groupedPhotos,
                            currentViewMode,
                            showGroupCover = false,
                            isSelectionMode = { isSelectionMode },
                            onPhotoClick = { photosList, position ->
                                if (isSelectionMode) {
                                    val photo = photosList.getOrNull(position)
                                    photo?.let { togglePhotoSelection(it.id) }
                                    photoGroupAdapter?.notifyDataSetChanged()
                                } else {
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
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "加载照片失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
        binding.viewModeButton.visibility = View.GONE
        updateSelectionUI()
        photoGroupAdapter?.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        binding.selectionActionBar.visibility = View.GONE
        // 文件夹内不显示视图模式切换按钮
        binding.viewModeButton.visibility = View.GONE
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
                loadFolderPhotos()
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
                // 详情页从 Application 读取列表，必须先写入当前文件夹的列表，否则会错位
                (activity?.application as? PhotoTimeGrouperApp)?.setAllPhotosList(ArrayList(photosList))
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
