package com.summitcodeworks.imager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.databinding.ItemListRedStripLayoutBinding
import com.summitcodeworks.imager.models.RedStripData

class RedStripAdapter(private val redStripDataList: List<RedStripData>, private val mContext: Context, private val onRedStripAdapterListener: OnRedStripAdapterListener):
    RecyclerView.Adapter<RedStripAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListRedStripLayoutBinding): RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListRedStripLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return redStripDataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val redStripData = redStripDataList[position]
        Glide.with(mContext).load(redStripData.bitmap).into(holder.binding.ivRedStrip)
        holder.binding.flRedStrip.setOnClickListener {
            onRedStripAdapterListener.onRedStripAdapterClick(redStripData)
        }
        holder.binding.ibRedStripImageDelete.setOnClickListener {
            onRedStripAdapterListener.onRedStripAdapterDelete(redStripData)
            notifyItemRemoved(position)
        }
    }

    interface OnRedStripAdapterListener {
        fun onRedStripAdapterClick(redStripData: RedStripData)
        fun onRedStripAdapterDelete(redStripData: RedStripData)
    }
}