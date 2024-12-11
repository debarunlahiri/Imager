package com.summitcodeworks.imager

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.summitcodeworks.imager.databinding.ActivityCameraBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class CommonUtils {

    companion object {
        fun convertUriToBitmap(context: Context, uri: Uri): Bitmap? {
            var bitmap: Bitmap? = null

            try {
                // Open the input stream from the URI
                val inputStream = context.contentResolver.openInputStream(uri)

                // Decode the InputStream into a Bitmap
                inputStream?.let {
                    bitmap = BitmapFactory.decodeStream(it)
                    it.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error while converting URI to Bitmap", Toast.LENGTH_SHORT).show()
            }

            return bitmap
        }

        fun bitmapToUri(context: Context, bitmap: Bitmap, fileName: String = "image_${System.currentTimeMillis()}.jpg"): Uri? {
            return try {
                // Create a file in the app's private files directory
                val file = File(context.filesDir, fileName)

                // Write the bitmap to the file
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Convert the file path to a URI
                Uri.fromFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

    }


}