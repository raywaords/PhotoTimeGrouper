package com.phototimegrouper.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class FolderItem(
    val name: String,
    val count: Int,
    val coverUri: String
)

class FolderListAdapter(
    private var folders: List<FolderItem>,
    private val onFolderClick: (FolderItem) -> Unit
) : RecyclerView.Adapter<FolderListAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view, onFolderClick)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    fun updateFolders(newFolders: List<FolderItem>) {
        folders = newFolders
        notifyDataSetChanged()
    }

    class FolderViewHolder(
        itemView: View,
        private val onFolderClick: (FolderItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val coverImageView: ImageView = itemView.findViewById(R.id.folderCoverImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.folderNameTextView)
        private val countTextView: TextView = itemView.findViewById(R.id.folderCountTextView)

        fun bind(folder: FolderItem) {
            nameTextView.text = folder.name
            countTextView.text = "${folder.count} 项"

            Glide.with(itemView)
                .load(folder.coverUri)
                .centerCrop()
                .into(coverImageView)

            itemView.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }
}

