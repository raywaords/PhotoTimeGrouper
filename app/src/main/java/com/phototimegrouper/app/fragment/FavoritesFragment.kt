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
import com.phototimegrouper.app.databinding.FragmentFavoritesBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val favoritePhotos = mutableSetOf<Long>()
    private var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
    private var currentViewMode: ViewMode = ViewMode.LARGE_ICON
    private var photoGroupAdapter: PhotoGroupAdapter? = null
    
    // 选择模式相关
    private var isSelectionMode = false
    private val selectedPhotos = mutableSetOf<Long>()

    private lateinit var photoRepository: PhotoRepository
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_VIEW_MODE = "pref_view_mode"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
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

        setupRecyclerView()
        setupSwipeRefresh()
        setupSelectionMode()
        setupButtons()

        loadFavoritePhotos()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadFavoritePhotos()
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
        binding.viewModeButton.setOnClickListener {
            toggleViewMode()
        }
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

    private fun loadFavoritePhotos() {
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                // 加载收藏的照片ID
                val favoriteIds = photoRepository.getFavoritePhotoIds()
                favoritePhotos.clear()
                favoritePhotos.addAll(favoriteIds)

                if (favoriteIds.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                    return@launch
                }

                // 加载所有照片
                val allPhotos = withContext(Dispatchers.IO) {
                    photoRepository.loadPhotosFromMediaStore()
                }

                // 过滤已删除的
                val deletedIds = withContext(Dispatchers.IO) {
                    photoRepository.getDeletedPhotoIds()
                }
                
                // 只保留收藏的且未删除的
                val favoritePhotosList = allPhotos
                    .filter { favoriteIds.contains(it.id) && !deletedIds.contains(it.id) }
                    .sortedByDescending { it.dateAdded }

                val groupedPhotos = groupPhotosByDate(favoritePhotosList)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (favoritePhotosList.isEmpty()) {
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
                    Toast.makeText(requireContext(), "加载收藏失败: ${e.message}", Toast.LENGTH_LONG).show()
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
        binding.viewModeButton.visibility = View.VISIBLE
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
                loadFavoritePhotos()
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
                    // 从收藏列表中移除后，重新加载
                    loadFavoritePhotos()
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
