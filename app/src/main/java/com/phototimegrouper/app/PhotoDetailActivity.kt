package com.phototimegrouper.app

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.phototimegrouper.app.databinding.ActivityPhotoDetailBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 照片详情页（全屏查看 + 滑动浏览）
 */
class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding
    private lateinit var photoList: ArrayList<PhotoItem>
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 全屏显示，隐藏状态栏和导航栏
        hideSystemUI()

        // 从 Application 中获取完整照片列表（避免通过 Intent 传递大数组）
        val app = application as? PhotoTimeGrouperApp
        photoList = app?.allPhotosList ?: arrayListOf()
        currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)

        if (photoList.isEmpty()) {
            finish()
            return
        }

        setupViewPager()
        updatePhotoInfo(currentPosition)
    }

    private fun setupViewPager() {
        val adapter = PhotoDetailAdapter(photoList, binding.viewPager) {
            // 点击照片时返回主列表
            finish()
        }
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentPosition, false)

        // 监听页面切换，更新照片信息
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updatePhotoInfo(position)
            }
        })
    }

    private fun updatePhotoInfo(position: Int) {
        val photo = photoList.getOrNull(position) ?: return

        binding.photoNameTextView.text = photo.displayName
        binding.photoDateTextView.text = DateFormatter.formatDateTime(photo.dateModified)
        binding.photoIndexTextView.text = "${position + 1} / ${photoList.size}"

        // 基本信息：大小 / 分辨率 / 格式
        val sizeText = photo.getFormattedSize()
        val resolutionText = photo.getResolution()
        val formatText = photo.getFormat()
        binding.photoBasicInfoTextView.text = "大小：$sizeText    分辨率：$resolutionText    格式：$formatText"

        // 先清空 EXIF / 位置信息，避免复用旧数据
        binding.photoExifInfoTextView.text = ""
        binding.photoLocationTextView.text = ""

        // 异步加载 EXIF 信息，避免阻塞 UI
        lifecycleScope.launch {
            val exifInfo = withContext(Dispatchers.IO) {
                PhotoMetadataLoader.loadExifInfo(this@PhotoDetailActivity, photo)
            }

            val exifParts = mutableListOf<String>()
            exifInfo["ISO"]?.let { exifParts.add("ISO $it") }
            exifInfo["光圈"]?.let { exifParts.add(it) }
            exifInfo["曝光时间"]?.let { exifParts.add("曝光：$it") }
            exifInfo["焦距"]?.let { exifParts.add("焦距：$it") }
            exifInfo["相机品牌"]?.let { exifParts.add(it) }
            exifInfo["相机型号"]?.let { exifParts.add(it) }

            binding.photoExifInfoTextView.text = if (exifParts.isNotEmpty()) {
                exifParts.joinToString("    ")
            } else {
                "EXIF：无可用信息"
            }

            // TODO: 后续可扩展读取 GPS 信息并反向地理编码为城市名称
            binding.photoLocationTextView.text = ""
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    companion object {
        const val EXTRA_CURRENT_POSITION = "extra_current_position"
        // 保留此常量以兼容旧测试（实际已不再使用，照片列表从 Application 获取）
        @Deprecated("照片列表现在从 Application 获取，不再通过 Intent 传递")
        const val EXTRA_PHOTO_LIST = "extra_photo_list"
    }
}

