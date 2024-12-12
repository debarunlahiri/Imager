package com.summitcodeworks.imager

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.databinding.ItemListGalleryLayoutBinding

class GalleryAdapter(val imageUris: List<Uri>, val mContext: Context, val onItemClick: (Uri) -> Unit): RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

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
            onItemClick(imageUri)
        }

    }
}