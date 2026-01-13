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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.databinding.ActivityMainBinding
import android.view.MenuItem
import android.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_PERMISSIONS = 100
    private var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
    private var currentViewMode: ViewMode = ViewMode.LARGE_ICON
    private var photoGroupAdapter: PhotoGroupAdapter? = null
    
    // SharedPreferences 用于持久化查看模�?
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_VIEW_MODE = "pref_view_mode"
    
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

        // 初始�?SharedPreferences
        sharedPreferences = getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)
        
        // �?SharedPreferences 加载上次的查看模�?
        val savedViewModeName = sharedPreferences.getString(PREF_VIEW_MODE, ViewMode.LARGE_ICON.name)
        currentViewMode = try {
            ViewMode.valueOf(savedViewModeName ?: ViewMode.LARGE_ICON.name)
        } catch (e: Exception) {
            ViewMode.LARGE_ICON
        }

        setupSwipeRefresh()
        setupViewModeMenu()
        checkPermissions()
    }
    
    private fun setupViewModeMenu() {
        binding.viewModeMenuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.view_mode_menu, popupMenu.menu)
            
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
                    else -> false
                }
            }
            
            popupMenu.show()
        }
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
                Toast.makeText(this, "需要存储权限才能加载照�?, Toast.LENGTH_LONG).show()
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
                val photos = withContext(Dispatchers.IO) {
                    loadPhotosFromMediaStore()
                }
                
                val groupedPhotos = groupPhotosByDate(photos)
                
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
                        onPhotoClick = { photosList, position ->
                            openPhotoDetail(photosList, position)
                        }
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
        // TODO: 将此处 URL 替换为您实际发布的隐私政策网页地址
        val privacyPolicyUrl = "https://your-domain.com/privacy-policy.html"
        
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
        
        // 如果是视频，使用系统视频播放器打开（直接使�?mediaType 判断�?
        if (photo.mediaType == PhotoItem.MediaType.VIDEO) {
            try {
                // 将字符串 URI 转换�?Uri 对象
                val videoUri = Uri.parse(photo.uri)
                
                // 创建 Intent，使用系统默认的视频播放�?
                val videoIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(videoUri, "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // 直接尝试启动，系统会自动选择合适的播放�?
                try {
                    startActivity(videoIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    // 如果没有找到播放器，提示用户
                    Toast.makeText(this, "未找到视频播放器，请安装一个视频播放应�?, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "无法播放视频: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "无法播放视频: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 如果是图片，打开图片详情�?
            // 不再通过 Intent 传递照片列表，而是通过 Application 类获�?
            // 避免 TransactionTooLargeException（Intent 数据过大�?
            val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                putExtra(PhotoDetailActivity.EXTRA_CURRENT_POSITION, position)
            }
            startActivity(intent)
        }
    }
}
