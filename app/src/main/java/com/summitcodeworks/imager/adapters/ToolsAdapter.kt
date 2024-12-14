package com.summitcodeworks.imager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.summitcodeworks.imager.databinding.ItemListToolsLayoutBinding
import com.summitcodeworks.imager.models.ToolsData



class ToolsAdapter(private val mContext: Context, private val toolsDataList: List<ToolsData>, private val onToolsAdapterListener: OnToolsAdapterListener):
    RecyclerView.Adapter<ToolsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemListToolsLayoutBinding): RecyclerView.ViewHolder(binding.root) {

    }

    interface OnToolsAdapterListener {
        fun onToolsAdapterClick(toolsData: ToolsData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListToolsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return toolsDataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val toolsData = toolsDataList[position]
        holder.binding.ivToolImage.setImageResource(toolsData.toolImage)
        holder.binding.tvToolName.text = toolsData.toolName
        holder.binding.flTool.setOnClickListener {
            onToolsAdapterListener.onToolsAdapterClick(toolsData)
        }
    }
}