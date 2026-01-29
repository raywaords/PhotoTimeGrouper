package com.phototimegrouper.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.phototimegrouper.app.databinding.ActivityMainNewBinding
import com.phototimegrouper.app.fragment.AlbumsFragment
import com.phototimegrouper.app.fragment.FavoritesFragment
import com.phototimegrouper.app.fragment.MoreFragment
import com.phototimegrouper.app.fragment.PhotosFragment

/**
 * 新UI设计的MainActivity
 * 使用底部导航栏和Fragment架构
 */
class MainActivityNew : AppCompatActivity() {
    private lateinit var binding: ActivityMainNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置底部导航栏
        setupBottomNavigation()

        // 默认显示照片Fragment
        if (savedInstanceState == null) {
            replaceFragment(PhotosFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_photos -> {
                    replaceFragment(PhotosFragment())
                    true
                }
                R.id.nav_albums -> {
                    replaceFragment(AlbumsFragment())
                    true
                }
                R.id.nav_favorites -> {
                    replaceFragment(FavoritesFragment())
                    true
                }
                R.id.nav_more -> {
                    replaceFragment(MoreFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
