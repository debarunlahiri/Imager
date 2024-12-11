package com.summitcodeworks.imager

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mContext = this

        val byteArray = intent.getByteArrayExtra("imageBitmap")
        byteArray?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            Glide.with(mContext).load(bitmap).into(binding.myZoomageView)
        }


        binding.cvImagePreviewBack.setOnClickListener {
            finish()
        }

        binding.bEditImage.setOnClickListener {
            val intent = Intent(mContext, ImageEditActivity::class.java)
            intent.putExtra("imageBitmap", byteArray)
            startActivity(intent)
        }
    }


}