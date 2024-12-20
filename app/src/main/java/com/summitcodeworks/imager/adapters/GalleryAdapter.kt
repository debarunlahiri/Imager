package com.summitcodeworks.imager.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.listeners.OnImageChangeListener
import com.summitcodeworks.imager.activities.ImageEditActivity
import com.summitcodeworks.imager.databinding.ItemListGalleryLayoutBinding
import com.summitcodeworks.imager.fragments.ImageActionsBottomDialogFragment
import com.summitcodeworks.imager.fragments.ImageActionsBottomDialogFragment.OnImageActionsBottomDialogListener
import java.io.File

class GalleryAdapter(private val imageFileList: MutableList<File>, private val mContext: Context, private val fragmentManager: FragmentManager): RecyclerView.Adapter<GalleryAdapter.ViewHolder>(),
    OnImageActionsBottomDialogListener {

    class ViewHolder(val binding: ItemListGalleryLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListGalleryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return imageFileList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageFile = imageFileList[position]
        Glide.with(mContext)
            .load(imageFile)
            .into(holder.binding.ivRedStrip)
        

        holder.binding.flRedStrip.setOnClickListener {
//            StfalconImageViewer.Builder(mContext, imageFileList) { view, image ->
//                Glide.with(mContext).load(image).into(view)
//            }.withStartPosition(position).withHiddenStatusBar(false).withImageChangeListener { }.show()
            val editIntent = Intent(mContext, ImageEditActivity::class.java)
            editIntent.putExtra("imageFilePath", imageFile.absolutePath)
            mContext.startActivity(editIntent)
        }



        holder.binding.flRedStrip.setOnLongClickListener {
            val imageActionsBottomDialogFragment = ImageActionsBottomDialogFragment.newInstance(imageFile.absolutePath, "")
            imageActionsBottomDialogFragment.show(fragmentManager, "image_actions_bottom_dialog")
            true
        }
    }

    private fun shareImage(imageFile: File?) {
        val uri = imageFile?.let {
            androidx.core.content.FileProvider.getUriForFile(
                mContext,
                "${mContext.packageName}.fileprovider",
                it
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        mContext.startActivity(Intent.createChooser(shareIntent, "Share image via"))
    }

    override fun onImageDelete(imageFile: File) {
        try {
            val file = imageFile
            if (file.exists()) {
                val isDeleted = file.delete()
                if (isDeleted) {
                    imageFileList.remove(imageFile)
                    notifyItemRemoved(imageFileList.indexOf(imageFile))
                } else {
                    Toast.makeText(mContext, "Failed to delete the image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(mContext, "File does not exist.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onImageEdit() {
    }

    override fun onImageShare(imageFile: File) {
        shareImage(imageFile)
    }

    override fun onImageDownload() {
    }

}