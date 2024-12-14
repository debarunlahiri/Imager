package com.summitcodeworks.imager.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

        fun uriToFile(uri: Uri, context: Context): File? {
            return try {
                val fileName = "temp_${System.currentTimeMillis()}.jpg"
                val tempFile = File(context.cacheDir, fileName)
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = tempFile.outputStream()

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }


    }


}