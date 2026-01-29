package com.phototimegrouper.app.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.phototimegrouper.app.R
import com.phototimegrouper.app.RecycleBinDemoActivity
import com.phototimegrouper.app.databinding.FragmentMoreBinding

class MoreFragment : Fragment() {
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 回收站
        binding.recycleBinLayout.setOnClickListener {
            openRecycleBin()
        }

        // 隐私政策
        binding.privacyPolicyLayout.setOnClickListener {
            openPrivacyPolicy()
        }

        // 关于应用
        binding.aboutLayout.setOnClickListener {
            showAboutDialog()
        }

        // 设置
        binding.settingsLayout.setOnClickListener {
            Toast.makeText(requireContext(), "设置功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openRecycleBin() {
        try {
            val intent = Intent(requireContext(), RecycleBinDemoActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开回收站: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "无法打开隐私政策页面: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("关于 Photo Time Grouper")
            .setMessage(
                "Photo Time Grouper v1.0\n\n" +
                "一款简洁优雅的照片管理应用\n\n" +
                "按时间或文件夹浏览照片\n" +
                "智能筛选和排序\n" +
                "收藏和回收站功能\n\n" +
                "© 2025 Photo Time Grouper"
            )
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
