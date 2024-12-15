package com.summitcodeworks.imager.activities

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityMemeMakerBinding
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class MemeMakerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemeMakerBinding
    private lateinit var mContext: Context
    private lateinit var imageFrame: FrameLayout

    private var activeTextView: TextView? = null
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemeMakerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this
        imageFrame = findViewById(R.id.imageFrame)

        // Image picker
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                originalBitmap = loadImageFromUri(it)
                originalBitmap?.let { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)

//                    originalBitmap?.let {
//                        extractTextFromImage(it)
//                    } ?: run {
//                        Toast.makeText(mContext, "No image loaded", Toast.LENGTH_SHORT).show()
//                    }
                } ?: run {
                    Toast.makeText(mContext, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        pickImage.launch("image/*")

        // Add text dynamically
        binding.addTextButton.setOnClickListener {
            addNewTextView()
        }

        // Apply styles to active text view
        binding.textColorButton.setOnClickListener {
            activeTextView?.setTextColor(Color.RED) // Example: Red color
        }

        binding.textSizeSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                activeTextView?.textSize = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.clearTextButton.setOnClickListener {
            activeTextView?.let {
                imageFrame.removeView(it)
                activeTextView = null
            }
        }
    }

    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun addNewTextView() {
        val newTextView = TextView(this).apply {
            text = "New Text"
            textSize = 24f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener(DragTouchListener())
            setOnClickListener {
                // Set this TextView as active
                activeTextView = this
                Toast.makeText(mContext, "Text Selected", Toast.LENGTH_SHORT).show()
            }
            setOnLongClickListener {
                // Open a dialog to edit text on long click
                showEditTextDialog(this)
                true
            }
        }

        imageFrame.addView(
            newTextView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    // Dialog to edit TextView content
    private fun showEditTextDialog(textView: TextView) {
        val editText = EditText(this).apply {
            setText(textView.text)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Text")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                textView.text = editText.text.toString()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Handle drag of text view
    inner class DragTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.tag = Pair(event.rawX, event.rawY)
                }

                MotionEvent.ACTION_MOVE -> {
                    val (prevX, prevY) = view.tag as Pair<Float, Float>
                    val deltaX = event.rawX - prevX
                    val deltaY = event.rawY - prevY

                    view.x = (view.x + deltaX).coerceIn(
                        0f,
                        (imageFrame.width - view.width).toFloat()
                    )
                    view.y = (view.y + deltaY).coerceIn(
                        0f,
                        (imageFrame.height - view.height).toFloat()
                    )

                    view.tag = Pair(event.rawX, event.rawY)
                }
            }
            return true
        }
    }
}


