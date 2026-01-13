package com.phototimegrouper.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.phototimegrouper.app.databinding.ItemPhotoDetailBinding
import com.github.chrisbanes.photoview.PhotoView

class PhotoDetailPagerAdapter(
    private val photoList: List<PhotoItem>
) : ListAdapter<PhotoItem, PhotoDetailPagerAdapter.PhotoDetailViewHolder>(PhotoDiffCallback()) {

    class PhotoDetailViewHolder(
        private val binding: ItemPhotoDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        val photoView: PhotoView = binding.photoView

        fun bind(photo: PhotoItem) {
            Glide.with(binding.root.context)
                .load(Uri.parse(photo.uri))
                .apply(
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .fitCenter()
                )
                .into(photoView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoDetailViewHolder {
        val binding = ItemPhotoDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoDetailViewHolder, position: Int) {
        val photo = photoList.getOrNull(position) ?: return
        holder.bind(photo)
    }

    override fun getItemCount(): Int = photoList.size

    class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoItem>() {
        override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean {
            return oldItem == newItem
        }
    }
}
