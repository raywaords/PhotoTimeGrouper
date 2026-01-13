package com.phototimegrouper.app

/**
 * 照片查看模式枚举
 * 参照 Windows 资源管理器的查看方式
 */
enum class ViewMode {
    EXTRA_LARGE_ICON,   // 超大图标：一张图片占满一天的宽度
    LARGE_ICON,         // 大图标：三张图片占满一天的宽度
    SMALL_ICON,         // 小图标：九宫格形式（3x3�?
    DETAILS             // 详细信息：列表形式，显示名称、大小、创建日期、文件类型等
}
