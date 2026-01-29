package com.phototimegrouper.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.phototimegrouper.app.PhotoItem
import com.phototimegrouper.app.SizeFilter
import com.phototimegrouper.app.SortOrder
import com.phototimegrouper.app.databinding.BottomSheetFilterBinding

/**
 * 通用筛选弹窗（不再直接操作 SharedPreferences）
 * - 由宿主 Fragment 传入当前筛选状态（currentMediaType/currentDaysFilter/currentSizeFilter）
 * - 用户点击“应用”后，通过 onApplyFilter 回调把最新状态回传给宿主
 * - “记住上次条件”的职责交给宿主自己（例如 PhotosFragment 里用 SharedPreferences 持久化）
 */
class FilterBottomSheetDialog : BottomSheetDialogFragment() {
    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    // 由宿主设置的当前筛选状态
    var currentMediaType: PhotoItem.MediaType? = null
    var currentDaysFilter: Int? = null
    var currentSizeFilter: SizeFilter? = null

    var onApplyFilter: ((PhotoItem.MediaType?, Int?, SizeFilter?, SortOrder) -> Unit)? = null
    var onClearFilter: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { sheet ->
                    val behavior = BottomSheetBehavior.from(sheet)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
        }

        // 使用宿主传入的 currentXXX 初始化 UI
        updateUI()

        binding.closeButton.setOnClickListener { dismiss() }
        binding.clearButton.setOnClickListener {
            clearFilters()
            onClearFilter?.invoke()
            dismiss()
        }
        binding.applyButton.setOnClickListener { applyFilters() }
    }

    private fun updateUI() {
        when (currentMediaType) {
            null -> binding.mediaTypeAll.isChecked = true
            PhotoItem.MediaType.IMAGE -> binding.mediaTypePhoto.isChecked = true
            PhotoItem.MediaType.VIDEO -> binding.mediaTypeVideo.isChecked = true
        }
        when (currentDaysFilter) {
            null -> binding.dateRangeAll.isChecked = true
            7 -> binding.dateRange7Days.isChecked = true
            30 -> binding.dateRange30Days.isChecked = true
            -1 -> binding.dateRangeThisYear.isChecked = true
            else -> binding.dateRangeAll.isChecked = true
        }
    }

    private fun clearFilters() {
        currentMediaType = null
        currentDaysFilter = null
        currentSizeFilter = null
    }

    private fun applyFilters() {
        val mediaType = when {
            binding.mediaTypePhoto.isChecked -> PhotoItem.MediaType.IMAGE
            binding.mediaTypeVideo.isChecked -> PhotoItem.MediaType.VIDEO
            else -> null
        }
        val daysFilter = when {
            binding.dateRange7Days.isChecked -> 7
            binding.dateRange30Days.isChecked -> 30
            binding.dateRangeThisYear.isChecked -> -1
            else -> null
        }
        currentMediaType = mediaType
        currentDaysFilter = daysFilter
        currentSizeFilter = null

        onApplyFilter?.invoke(mediaType, daysFilter, null, SortOrder.DATE_DESC)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
