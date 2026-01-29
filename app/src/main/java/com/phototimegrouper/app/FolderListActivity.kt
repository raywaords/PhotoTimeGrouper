package com.phototimegrouper.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phototimegrouper.app.databinding.ActivityFolderListBinding
import com.phototimegrouper.app.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 文件夹列表页面：按 BUCKET_DISPLAY_NAME 分组展示照片文件夹
 */
class FolderListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderListBinding
    private lateinit var photoRepository: PhotoRepository
    private lateinit var adapter: FolderListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_BROWSE_MODE = "pref_browse_mode"
    
    // 搜索筛选相关
    private var currentSearchQuery: String? = null
    private val searchQueryFlow = MutableStateFlow<String?>(null)
    private var allFolders: List<FolderItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        photoRepository = PhotoRepository.getInstance(this)
        sharedPreferences = getSharedPreferences("PhotoTimeGrouperPrefs", Context.MODE_PRIVATE)

        setupToolbar()
        setupViewModeMenu()
        setupSearchView()
        setupSearchFlow()
        setupRecyclerView()
        loadFolders()
    }

    private fun setupToolbar() {
        binding.toolbarBack.setOnClickListener { finish() }
        binding.toolbarTitle.text = "按文件夹浏览"
        binding.toolbarSubtitle.text = "展示系统相册/文件夹中的照片"
    }

    private fun setupViewModeMenu() {
        binding.viewModeMenuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.view_mode_menu, popupMenu.menu)
            
            // Hide view mode options in folder browse mode
            popupMenu.menu.findItem(R.id.menu_extra_large_icon).isVisible = false
            popupMenu.menu.findItem(R.id.menu_large_icon).isVisible = false
            popupMenu.menu.findItem(R.id.menu_small_icon).isVisible = false
            popupMenu.menu.findItem(R.id.menu_details).isVisible = false
            // Change "Browse Folders" to "Browse by Time"
            popupMenu.menu.findItem(R.id.menu_browse_folders)?.let { item ->
                item.title = getString(R.string.browse_by_time)
            }
            
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_privacy_policy -> {
                        openPrivacyPolicy()
                        true
                    }
                    R.id.menu_show_favorites -> {
                        // 保持文件夹浏览模式状态，标记需要显示收藏
                        sharedPreferences.edit()
                            .putBoolean("pref_show_favorites_on_resume", true) // MainActivity中的常量
                            .putBoolean("pref_return_to_folder_browse", true) // 标记返回时应该回到文件夹浏览
                            .apply()
                        // 启动MainActivity显示收藏，MainActivity会在onResume中读取状态并显示收藏
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.menu_search -> {
                        showSearchDialog()
                        true
                    }
                    R.id.menu_recycle_bin_demo -> {
                        openRecycleBinDemo()
                        true
                    }
                    R.id.menu_browse_folders -> {
                        // Switch back to time browse mode
                        sharedPreferences.edit().putBoolean(PREF_BROWSE_MODE, false).apply()
                        // 启动MainActivity（如果MainActivity在栈中，会复用；如果不在，会创建新的）
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish() // 关闭FolderListActivity
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
        }
    }

    private fun openPrivacyPolicy() {
        val privacyPolicyUrl = "https://raywaords.github.io/PhotoTimeGrouper/privacy-policy.html"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(privacyPolicyUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开隐私政策页面: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openRecycleBinDemo() {
        try {
            val intent = Intent(this, RecycleBinDemoActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开回收站预览: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSearchDialog() {
        // 显示搜索对话框，让用户输入搜索关键词
        val searchEditText = EditText(this).apply {
            hint = "输入文件夹名称"
            setText(currentSearchQuery ?: "")
        }
        
        AlertDialog.Builder(this)
            .setTitle("搜索文件夹")
            .setView(searchEditText)
            .setPositiveButton("搜索") { _: android.content.DialogInterface, _: Int ->
                val query = searchEditText.text.toString().trim()
                currentSearchQuery = if (query.isBlank()) null else query
                searchQueryFlow.value = currentSearchQuery
                applySearchFilter()
                // 如果搜索栏可见，更新搜索栏内容
                binding.searchView.setQuery(currentSearchQuery ?: "", false)
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除") { _: android.content.DialogInterface, _: Int ->
                currentSearchQuery = null
                searchQueryFlow.value = null
                applySearchFilter()
                binding.searchView.setQuery("", false)
            }
            .show()
    }

    private fun setupRecyclerView() {
        adapter = FolderListAdapter(emptyList()) { folder ->
            // 打开文件夹内照片列表
            val intent = Intent(this, FolderPhotosActivity::class.java).apply {
                putExtra(FolderPhotosActivity.EXTRA_FOLDER_NAME, folder.name)
            }
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadFolders() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val folders = withContext(Dispatchers.IO) {
                    val photos = photoRepository.loadPhotosFromMediaStore()
                    FolderGrouper.groupByFolder(photos)
                }
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    allFolders = folders
                    applySearchFilter(folders)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@FolderListActivity,
                        "加载文件夹失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    // ====== 搜索功能 ======
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query?.takeIf { it.isNotBlank() }
                searchQueryFlow.value = currentSearchQuery
                applySearchFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.takeIf { it.isNotBlank() }
                currentSearchQuery = query
                searchQueryFlow.value = query
                // 实时搜索，直接应用过滤
                applySearchFilter()
                return true
            }
        })
        
        binding.searchView.setOnCloseListener {
            currentSearchQuery = null
            searchQueryFlow.value = null
            applySearchFilter()
            false
        }
    }
    
    private fun setupSearchFlow() {
        // Flow用于防抖，但主要搜索逻辑在onQueryTextChange中直接处理
        searchQueryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                // Flow防抖处理
                currentSearchQuery = query
                applySearchFilter()
            }
            .launchIn(lifecycleScope)
    }
    
    private fun applySearchFilter(folders: List<FolderItem>? = null) {
        // 使用 allFolders 作为源数据（确保已加载）
        val sourceFolders = folders ?: allFolders
        
        // 如果没有数据，直接返回
        if (sourceFolders.isEmpty()) {
            return
        }
        
        // 应用搜索过滤
        val filtered = if (currentSearchQuery.isNullOrBlank()) {
            sourceFolders
        } else {
            sourceFolders.filter { folder ->
                folder.name.contains(currentSearchQuery!!, ignoreCase = true)
            }
        }
        
        // 更新适配器
        adapter.updateFolders(filtered)
        
        // 更新空视图显示
        if (filtered.isEmpty() && currentSearchQuery.isNullOrBlank().not()) {
            // 有搜索关键词但没有结果
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = "未找到匹配的文件夹"
        } else if (filtered.isEmpty() && sourceFolders.isEmpty()) {
            // 完全没有数据
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = "未找到任何包含照片的文件夹"
        } else {
            // 有数据，隐藏空视图
            binding.emptyView.visibility = View.GONE
        }
    }
}

