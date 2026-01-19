package com.phototimegrouper.app

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.phototimegrouper.app.databinding.ActivityPhotoDetailBinding

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

