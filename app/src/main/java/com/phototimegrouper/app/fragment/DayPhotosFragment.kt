package com.phototimegrouper.app.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.phototimegrouper.app.*
import com.phototimegrouper.app.databinding.FragmentPhotosBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 某一天的照片列表，样式与相册内照片一致（网格）
 */
class DayPhotosFragment : Fragment() {
    private var _binding: FragmentPhotosBinding? = null
    private val binding get() = _binding!!

    private var dateLabel: String? = null
    private var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
    private var photoGridAdapter: PhotoSmallIconAdapter? = null

    private var isSelectionMode = false
    private val selectedPhotos = mutableSetOf<Long>()
    private val favoritePhotos = mutableSetOf<Long>()

    private lateinit var photoRepository: PhotoRepository

    companion object {
        private const val ARG_DATE_KEY = "date_key"
        private const val ARG_DATE_LABEL = "date_label"

        fun newInstance(dateKey: String, dateLabel: String): DayPhotosFragment {
            return DayPhotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATE_KEY, dateKey)
                    putString(ARG_DATE_LABEL, dateLabel)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dateLabel = arguments?.getString(ARG_DATE_LABEL)
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

        // 与「相册 > 某文件夹」保持一致：不再使用单独的大号日期标题栏，仅隐藏搜索/筛选控件，
        // 顶部结构和文件夹视图一致，由系统导航返回
        binding.searchView.visibility = View.GONE
        binding.filterButton.visibility = View.GONE
        binding.filterChipsRecyclerView.visibility = View.GONE
        binding.viewModeButton.visibility = View.GONE

        binding.dayHeaderBar.visibility = View.GONE
        binding.toolbar.visibility = View.VISIBLE

        // 使用 ActionBar 标题展示当前日期，风格与相册文件夹标题一致
        (activity as? MainActivityNew)?.supportActionBar?.title = dateLabel ?: ""

        setupRecyclerView()
        setupSelectionMode()
        loadFavorites()
        displayPhotos()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
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
            photoGridAdapter?.notifyDataSetChanged()
        }
        binding.shareButton.setOnClickListener { shareSelectedPhotos() }
        binding.deleteButton.setOnClickListener { deleteSelectedPhotos() }
        binding.cancelSelectionButton.setOnClickListener {
            isSelectionMode = false
            selectedPhotos.clear()
            binding.selectionActionBar.visibility = View.GONE
            photoGridAdapter?.notifyDataSetChanged()
        }
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            try {
                val ids = photoRepository.getFavoritePhotoIds()
                favoritePhotos.clear()
                favoritePhotos.addAll(ids)
            } catch (e: Exception) {
                favoritePhotos.clear()
            }
        }
    }

    private fun displayPhotos() {
        val app = activity?.application as? PhotoTimeGrouperApp
        val list = app?.dayPhotosForDetail
        app?.clearDayPhotosForDetail()

        if (list.isNullOrEmpty()) {
            binding.progressBar.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            return
        }

        allPhotosList = ArrayList(list)
        (activity?.application as? PhotoTimeGrouperApp)?.setAllPhotosList(allPhotosList)

        binding.progressBar.visibility = View.GONE
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE

        photoGridAdapter = PhotoSmallIconAdapter(
            requireContext(),
            allPhotosList,
            isSelectionMode = { isSelectionMode },
            isPhotoSelected = { selectedPhotos.contains(it) },
            isFavorite = { favoritePhotos.contains(it) },
            onToggleFavorite = { toggleFavorite(it) },
            onPhotoClick = { position ->
                if (isSelectionMode) {
                    togglePhotoSelection(allPhotosList.getOrNull(position)?.id ?: return@PhotoSmallIconAdapter)
                    photoGridAdapter?.notifyDataSetChanged()
                } else {
                    openPhotoDetail(allPhotosList, position)
                }
            },
            onPhotoLongClick = { position ->
                allPhotosList.getOrNull(position)?.id?.let {
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        selectedPhotos.add(it)
                        binding.selectionActionBar.visibility = View.VISIBLE
                        updateSelectionUI()
                        photoGridAdapter?.notifyDataSetChanged()
                    }
                }
            }
        )
        binding.recyclerView.adapter = photoGridAdapter
    }

    private fun updateSelectionUI() {
        val count = selectedPhotos.size
        binding.selectedCountTextView.text = getString(R.string.selected_count, count)
        binding.selectAllButton.text = if (count == allPhotosList.size && count > 0) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }
        binding.shareButton.isEnabled = count > 0
        binding.deleteButton.isEnabled = count > 0
    }

    private fun togglePhotoSelection(photoId: Long) {
        if (selectedPhotos.contains(photoId)) selectedPhotos.remove(photoId)
        else selectedPhotos.add(photoId)
        updateSelectionUI()
    }

    private fun shareSelectedPhotos() {
        val selected = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择要分享的照片", Toast.LENGTH_SHORT).show()
            return
        }
        val uris = selected.map { Uri.parse(it.uri) }
        val hasVideo = selected.any { it.mediaType == PhotoItem.MediaType.VIDEO }
        val hasImage = selected.any { it.mediaType == PhotoItem.MediaType.IMAGE }
        val mimeType = when {
            hasVideo && hasImage -> "*/*"
            hasVideo -> "video/*"
            else -> "image/*"
        }
        try {
            startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "分享媒体"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法分享: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedPhotos() {
        val selected = allPhotosList.filter { selectedPhotos.contains(it.id) }
        if (selected.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("移到回收站")
            .setMessage("${getString(R.string.delete_confirm, selected.size)}\n可在 30 天内恢复。")
            .setPositiveButton("移到回收站") { _, _ ->
                lifecycleScope.launch {
                    try {
                        photoRepository.softDeletePhotos(selected.map { it.id })
                        Toast.makeText(requireContext(), "已移到回收站", Toast.LENGTH_SHORT).show()
                        isSelectionMode = false
                        selectedPhotos.clear()
                        binding.selectionActionBar.visibility = View.GONE
                        (activity?.application as? PhotoTimeGrouperApp)?.setDayPhotosForDetail(
                            allPhotosList.filterNot { p -> selected.map { it.id }.contains(p.id) }
                        )
                        displayPhotos()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun toggleFavorite(photoId: Long) {
        lifecycleScope.launch {
            try {
                val newState = photoRepository.toggleFavorite(photoId)
                if (newState) favoritePhotos.add(photoId) else favoritePhotos.remove(photoId)
                withContext(Dispatchers.Main) { photoGridAdapter?.notifyDataSetChanged() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPhotoDetail(photosList: List<PhotoItem>, position: Int) {
        val photo = photosList.getOrNull(position) ?: return
        if (photo.mediaType == PhotoItem.MediaType.VIDEO) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(photo.uri), "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法播放视频", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                startActivity(Intent(requireContext(), PhotoDetailActivity::class.java).apply {
                    putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position)
                })
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "无法打开详情: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
