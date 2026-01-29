package com.phototimegrouper.app

import android.app.Application

/**
 * Application 类，用于存储全局照片列表
 * 避免通过 Intent 传递大量数据导致的 TransactionTooLargeException
 */
class PhotoTimeGrouperApp : Application() {
    
    companion object {
        @Volatile
        private var instance: PhotoTimeGrouperApp? = null
        
        fun getInstance(): PhotoTimeGrouperApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    // 存储所有照片列表（按日期顺序展平后的列表）
    var allPhotosList: ArrayList<PhotoItem> = arrayListOf()
        private set
    
    fun setAllPhotosList(photos: List<PhotoItem>) {
        allPhotosList = ArrayList(photos)
    }

    /** 点击某日期前由 PhotosFragment 写入，供 DayPhotosFragment 读取并展示该日照片 */
    var dayPhotosForDetail: ArrayList<PhotoItem>? = null
        private set

    fun setDayPhotosForDetail(photos: List<PhotoItem>) {
        dayPhotosForDetail = ArrayList(photos)
    }

    fun clearDayPhotosForDetail() {
        dayPhotosForDetail = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
