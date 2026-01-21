package com.phototimegrouper.app

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.database.PhotoMetadataEntity
import com.phototimegrouper.app.databinding.ActivityRecycleBinDemoBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 回收站功能页面：展示软删除的照片，并支持恢复（后续再补充彻底删除）
 */
class RecycleBinDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecycleBinDemoBinding
    private lateinit var photoRepository: PhotoRepository
    private lateinit var adapter: RecycleBinDemoAdapter
    private var pendingPermanentDeleteItem: RecycleBinDemoItem.Item? = null
    private var pendingAutoCleanIds: List<Long> = emptyList()

    companion object {
        private const val REQUEST_CODE_PERMANENT_DELETE = 2001
        /** 正式逻辑：回收站保留 30 天 */
        private const val AUTO_CLEAN_DAYS: Long = 1
        /**
         * 测试时可以临时改成小一点的值（例如 1），不用真的等 30 天。
         * 发布正式版本前再改回 30。
         */
        // private const val AUTO_CLEAN_DAYS: Long = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecycleBinDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        photoRepository = PhotoRepository.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        observeDeletedPhotos()
        checkAutoClean()
    }

    private fun setupToolbar() {
        binding.toolbarBack.setOnClickListener { finish() }
        binding.toolbarTitle.text = "回收站"
        binding.toolbarSubtitle.text = "已删除的照片会在这里保留 30 天，可恢复或彻底删除"
    }

    private fun setupRecyclerView() {
        adapter = RecycleBinDemoAdapter(emptyList()) { item ->
            showItemActionDialog(item)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    /**
     * 监听数据库中已删除的照片，实时刷新回收站列表
     */
    private fun observeDeletedPhotos() {
        lifecycleScope.launch {
            photoRepository.getDeletedPhotosFlow().collectLatest { entities ->
                val items = buildItemsFromEntities(entities)
                withContext(Dispatchers.Main) {
                    adapter.updateItems(items)
                }
            }
        }
    }

    /**
     * 检查是否有已在回收站超过 AUTO_CLEAN_DAYS 的照片，必要时提示自动清理
     */
    private fun checkAutoClean() {
        lifecycleScope.launch {
            try {
                val expired = photoRepository.getExpiredDeletedPhotos(AUTO_CLEAN_DAYS)
                if (expired.isEmpty()) return@launch

                val count = expired.size
                AlertDialog.Builder(this@RecycleBinDemoActivity)
                    .setTitle("自动清理回收站")
                    .setMessage("有 $count 项已在回收站超过 $AUTO_CLEAN_DAYS 天，是否现在彻底删除？\n\n你也可以随时在回收站中手动彻底删除。")
                    .setPositiveButton("立即清理") { _, _ ->
                        requestAutoPermanentDelete(expired)
                    }
                    .setNegativeButton("稍后再说", null)
                    .show()
            } catch (e: Exception) {
                // 自动清理检查失败不影响正常使用，不提示错误
            }
        }
    }

    private fun buildItemsFromEntities(entities: List<PhotoMetadataEntity>): List<RecycleBinDemoItem> {
        if (entities.isEmpty()) return emptyList()

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val autoDeleteFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 根据 deletedAt 分组，简单按日期分组
        val grouped = entities.groupBy { entity ->
            val ts = entity.deletedAt ?: 0L
            dateFormatter.format(Date(ts * 1000))
        }

        val result = mutableListOf<RecycleBinDemoItem>()
        val sortedKeys = grouped.keys.sortedDescending()
        for (key in sortedKeys) {
            result.add(RecycleBinDemoItem.Header(key))
            val list = grouped[key] ?: continue
            list.forEach { entity ->
                val deletedAt = entity.deletedAt ?: 0L
                val autoDeleteDate = autoDeleteFormatter.format(Date((deletedAt + 30L * 24 * 60 * 60) * 1000))

                result.add(
                    RecycleBinDemoItem.Item(
                        id = entity.id,
                        uri = entity.uri,
                        displayName = entity.displayName,
                        sizeText = formatSize(entity.size),
                        resolutionText = "${entity.width} × ${entity.height}",
                        formatText = entity.mimeType,
                        deletedAtText = dateFormatter.format(Date(deletedAt * 1000)),
                        autoDeleteDate = autoDeleteDate
                    )
                )
            }
        }

        return result
    }

    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.getDefault(), "%.1f GB", gb)
    }

    private fun showItemActionDialog(item: RecycleBinDemoItem.Item) {
        AlertDialog.Builder(this)
            .setTitle(item.displayName)
            .setItems(arrayOf("恢复", "彻底删除", "取消")) { dialog, which ->
                when (which) {
                    0 -> restoreItem(item)
                    1 -> confirmPermanentDelete(item)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun restoreItem(item: RecycleBinDemoItem.Item) {
        lifecycleScope.launch {
            try {
                photoRepository.restorePhoto(item.id)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@RecycleBinDemoActivity,
                        "已恢复：${item.displayName}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@RecycleBinDemoActivity,
                        "恢复失败: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun confirmPermanentDelete(item: RecycleBinDemoItem.Item) {
        AlertDialog.Builder(this)
            .setTitle("彻底删除")
            .setMessage("将从设备中永久删除该照片/视频，且无法恢复。\n\n文件名：${item.displayName}")
            .setPositiveButton("彻底删除") { _, _ ->
                requestPermanentDelete(item)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 调用系统删除对话框（Android 11+）或直接删除（Android 10 及以下）
     */
    private fun requestPermanentDelete(item: RecycleBinDemoItem.Item) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uri = Uri.parse(item.uri)
                val pendingIntent: IntentSender = MediaStore.createDeleteRequest(
                    contentResolver,
                    listOf(uri)
                ).intentSender

                pendingPermanentDeleteItem = item
                startIntentSenderForResult(
                    pendingIntent,
                    REQUEST_CODE_PERMANENT_DELETE,
                    null,
                    0,
                    0,
                    0
                )
            } catch (e: Exception) {
                Toast.makeText(this, "无法请求系统删除: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 10 及以下，直接通过 ContentResolver 删除
            lifecycleScope.launch {
                try {
                    val uri = Uri.parse(item.uri)
                    val deletedCount = withContext(Dispatchers.IO) {
                        contentResolver.delete(uri, null, null)
                    }
                    if (deletedCount > 0) {
                        // 删除成功后，移除数据库记录
                        photoRepository.hardDeletePhoto(item.id)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@RecycleBinDemoActivity,
                                "已彻底删除：${item.displayName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@RecycleBinDemoActivity,
                                "删除失败，文件可能已不存在",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RecycleBinDemoActivity,
                            "删除失败: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * 批量自动清理：对过期回收站条目做彻底删除
     */
    private fun requestAutoPermanentDelete(expiredEntities: List<PhotoMetadataEntity>) {
        if (expiredEntities.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uris = expiredEntities.mapNotNull { entity ->
                    runCatching { Uri.parse(entity.uri) }.getOrNull()
                }
                if (uris.isEmpty()) return

                val pendingIntent: IntentSender = MediaStore.createDeleteRequest(
                    contentResolver,
                    uris
                ).intentSender

                // 这里不需要逐个缓存条目，只需在结果 OK 后按 ID 批量删数据库
                startIntentSenderForResult(
                    pendingIntent,
                    REQUEST_CODE_PERMANENT_DELETE,
                    null,
                    0,
                    0,
                    0
                )

                // 将待清理的 ID 保存到 tag 中，结果回来时再处理
                pendingAutoCleanIds = expiredEntities.map { it.id }
            } catch (e: Exception) {
                Toast.makeText(this, "无法请求自动清理: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 10 及以下，直接循环删除
            lifecycleScope.launch {
                try {
                    var successCount = 0
                    withContext(Dispatchers.IO) {
                        expiredEntities.forEach { entity ->
                            val uri = runCatching { Uri.parse(entity.uri) }.getOrNull() ?: return@forEach
                            val deleted = contentResolver.delete(uri, null, null)
                            if (deleted > 0) {
                                photoRepository.hardDeletePhoto(entity.id)
                                successCount++
                            }
                        }
                    }
                    if (successCount > 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@RecycleBinDemoActivity,
                                "已自动清理 $successCount 项",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (_: Exception) {
                    // 忽略自动清理失败
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PERMANENT_DELETE) {
            if (resultCode == RESULT_OK) {
                // 区分：单个删除 vs 自动清理
                if (pendingAutoCleanIds.isNotEmpty()) {
                    val idsToClean = pendingAutoCleanIds
                    pendingAutoCleanIds = emptyList()
                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                idsToClean.forEach { id ->
                                    photoRepository.hardDeletePhoto(id)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@RecycleBinDemoActivity,
                                    "已自动清理 ${idsToClean.size} 项",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (_: Exception) {
                            // 忽略自动清理失败
                        }
                    }
                } else {
                    val item = pendingPermanentDeleteItem ?: return
                    pendingPermanentDeleteItem = null
                    lifecycleScope.launch {
                        try {
                            photoRepository.hardDeletePhoto(item.id)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@RecycleBinDemoActivity,
                                    "已彻底删除：${item.displayName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@RecycleBinDemoActivity,
                                    "更新数据库失败: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } else {
                // 用户取消了删除
                pendingAutoCleanIds = emptyList()
                pendingPermanentDeleteItem = null
                Toast.makeText(
                    this,
                    "已取消删除",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
