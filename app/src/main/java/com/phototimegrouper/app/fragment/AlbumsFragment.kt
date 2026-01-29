package com.phototimegrouper.app.fragment

import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.FolderItem
import com.phototimegrouper.app.FolderListAdapter
import com.phototimegrouper.app.FolderPhotosActivity
import com.phototimegrouper.app.PhotoItem
import com.phototimegrouper.app.databinding.FragmentAlbumsBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumsFragment : Fragment() {
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoRepository: PhotoRepository
    private val allFolders = mutableListOf<FolderItem>()        // 当前显示的相册列表
    private var allFoldersRaw: List<FolderItem> = emptyList()   // 原始相册列表（用于筛选/搜索）
    private var folderAdapter: FolderListAdapter? = null

    // 相册首页：类型/时间筛选 + 搜索
    private var currentMediaTypeFilter: PhotoItem.MediaType? = null
    private var currentDaysFilter: Int? = null
    private var currentSearchQuery: String? = null

    // 相册首页自己的筛选偏好（与照片页、旧 MainActivity 分开）
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_MEDIA_TYPE_FILTER = "albums_pref_media_type_filter"
    private val PREF_DAYS_FILTER = "albums_pref_days_filter"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoRepository = PhotoRepository.getInstance(requireContext())
        sharedPreferences = requireContext().applicationContext
            .getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)

        // 先加载上次的相册首页筛选偏好
        loadFilterPreferences()

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()

        loadFolders()
    }

    private fun setupToolbar() {
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        folderAdapter = FolderListAdapter(allFolders) { folder ->
            openFolder(folder)
        }
        binding.recyclerView.adapter = folderAdapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query?.takeIf { it.isNotBlank() }
                applyFolderFiltersAndSearch()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText?.takeIf { it.isNotBlank() }
                applyFolderFiltersAndSearch()
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadFolders()
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            com.phototimegrouper.app.R.color.purple_500,
            com.phototimegrouper.app.R.color.purple_700,
            com.phototimegrouper.app.R.color.teal_200,
            com.phototimegrouper.app.R.color.teal_700
        )
    }

    /**
     * 从 SharedPreferences 读取相册首页的筛选偏好
     */
    private fun loadFilterPreferences() {
        val mediaTypeName = sharedPreferences.getString(PREF_MEDIA_TYPE_FILTER, null)
        currentMediaTypeFilter = mediaTypeName?.let {
            try {
                PhotoItem.MediaType.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }

        val daysRaw = sharedPreferences.getInt(PREF_DAYS_FILTER, Int.MIN_VALUE)
        currentDaysFilter = when (daysRaw) {
            7, 30, -1 -> daysRaw
            else -> null
        }
    }

    /**
     * 将当前相册首页的筛选条件写入 SharedPreferences
     */
    private fun saveFilterPreferences() {
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
        editor.apply()
    }

    private fun loadFolders() {
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val photos = withContext(Dispatchers.IO) {
                    photoRepository.loadPhotosFromMediaStore()
                }

                // 先按当前相册首页筛选条件过滤照片
                var filteredPhotos = photos
                currentMediaTypeFilter?.let { mediaType ->
                    filteredPhotos = filteredPhotos.filter { it.mediaType == mediaType }
                }
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

                // 按文件夹分组（基于已过滤的照片）
                val grouped = filteredPhotos.groupBy { it.bucketDisplayName.ifBlank { "未知文件夹" } }

                val folders = grouped.entries.map { (name, list) ->
                    val coverUri = list.firstOrNull()?.uri ?: ""
                    FolderItem(
                        name = name,
                        count = list.size,
                        coverUri = coverUri
                    )
                }.sortedByDescending { it.count }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (folders.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE

                        allFoldersRaw = folders
                        applyFolderFiltersAndSearch()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "加载相册失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 根据当前搜索关键字，对 allFoldersRaw 进行过滤并刷新列表
     */
    private fun applyFolderFiltersAndSearch() {
        val base = allFoldersRaw
        val query = currentSearchQuery
        val displayed = if (query.isNullOrBlank()) {
            base
        } else {
            base.filter { it.name.contains(query, ignoreCase = true) }
        }
        allFolders.clear()
        allFolders.addAll(displayed)
        folderAdapter?.notifyDataSetChanged()
    }

    private fun showFilterDialog() {
        val dialog = FilterBottomSheetDialog().apply {
            // 使用当前相册首页的筛选状态初始化弹窗
            currentMediaType = this@AlbumsFragment.currentMediaTypeFilter
            currentDaysFilter = this@AlbumsFragment.currentDaysFilter
            currentSizeFilter = null

            onApplyFilter = { mediaType, days, _, _ ->
                // 写回相册首页状态并持久化
                this@AlbumsFragment.currentMediaTypeFilter = mediaType
                this@AlbumsFragment.currentDaysFilter = days
                saveFilterPreferences()
                loadFolders()
            }

            onClearFilter = {
                this@AlbumsFragment.currentMediaTypeFilter = null
                this@AlbumsFragment.currentDaysFilter = null
                saveFilterPreferences()
                loadFolders()
            }
        }
        dialog.show(childFragmentManager, "AlbumsFilterBottomSheet")
    }

    private fun openFolder(folder: FolderItem) {
        try {
            // 使用Fragment替代Activity，保持UI一致性
            val fragment = com.phototimegrouper.app.fragment.FolderPhotosFragment.newInstance(folder.name)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(com.phototimegrouper.app.R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开相册: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从文件夹详情返回时刷新列表
        loadFolders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
