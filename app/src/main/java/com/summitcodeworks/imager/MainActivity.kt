package com.summitcodeworks.imager

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.summitcodeworks.imager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.bCapture.setOnClickListener {
            val cameraIntent = Intent(this, CameraActivity::class.java)
            startActivity(cameraIntent)
        }

        binding.bChooseFIle.setOnClickListener {
            val galleryIntent = Intent(this, GalleryActivity::class.java)
            galleryIntent.putExtra("isFromGallery", false)
            startActivity(galleryIntent)
        }

        binding.bGallery.setOnClickListener {
            val galleryIntent = Intent(this, GalleryActivity::class.java)
            galleryIntent.putExtra("isFromGallery", true)
            startActivity(galleryIntent)
        }

    }
}