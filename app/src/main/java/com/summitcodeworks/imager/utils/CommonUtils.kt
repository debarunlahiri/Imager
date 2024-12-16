package com.summitcodeworks.imager.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CommonUtils {

    companion object {

        lateinit var mContext: Context

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

        fun convertBitmapToUri(context: Context, bitmap: Bitmap, fileName: String = "image_${System.currentTimeMillis()}.jpg"): Uri? {
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

        fun convertUriToFile(uri: Uri, context: Context): File? {
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

        fun convertBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 100
        ): File? {
            return try {
                // Create a file in the app's cache directory
                val file = File(context.cacheDir, filename)

                // Create output stream
                FileOutputStream(file).use { fos ->
                    // Compress bitmap to output stream
                    bitmap.compress(format, quality, fos)
                    // Flush and close the stream
                    fos.flush()
                }

                // Return the file if everything succeeded
                file
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }




    }


}