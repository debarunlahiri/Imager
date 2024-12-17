package com.summitcodeworks.imager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.databinding.ItemListGalleryLayoutBinding
import java.io.File

class GalleryAdapter(val imageUris: List<File>, val mContext: Context, val onGalleryListener: OnGalleryListener): RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListGalleryLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListGalleryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUri = imageUris[position]
        Glide.with(mContext)
            .load(imageUri)
            .into(holder.binding.ivRedStrip)
        holder.binding.flRedStrip.setOnClickListener {
            onGalleryListener.onGalleryClick(imageUri, position)
        }

        holder.binding.ibImageDelete.setOnClickListener {
            // Delete image
            onGalleryListener.onGalleryDelete(imageUri)
        }
    }

    interface OnGalleryListener {
        fun onGalleryClick(imageUri: File, position: Int)
        fun onGalleryDelete(imageUri: File)
    }
}