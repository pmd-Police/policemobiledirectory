package com.example.policemobiledirectory.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {

    // ------------------------- BASE64 Helpers -------------------------

    fun encodeImageToBase64(bitmap: Bitmap, quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------- Bitmap Utilities -------------------------

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ------------------------- Camera / Gallery Intents -------------------------

    fun createCameraIntent(context: Context, authority: String): Pair<Intent, Uri?> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        val imageUri: Uri = FileProvider.getUriForFile(context, authority, imageFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        return Pair(intent, imageUri)
    }

    fun createGalleryIntent(): Intent {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        return intent
    }

    fun getBitmapFromResult(context: Context, resultCode: Int, data: Intent?, uri: Uri?): Bitmap? {
        return if (resultCode == Activity.RESULT_OK) {
            val finalUri = data?.data ?: uri
            if (finalUri != null) getBitmapFromUri(context, finalUri) else null
        } else null
    }

    // ------------------------- Cropping Support -------------------------

    fun getCropIntent(context: Context, sourceUri: Uri): Intent {
        val destinationUri = createCropDestinationUri(context)
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f) // square crop
            .withMaxResultSize(1024, 1024)
            .withOptions(getDefaultCropOptions(context))

        return uCrop.getIntent(context)
    }

    private fun getDefaultCropOptions(context: Context): UCrop.Options {
        return UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setHideBottomControls(true)
            setFreeStyleCropEnabled(false)
        }
    }

    private fun createCropDestinationUri(context: Context): Uri {
        val fileName = "CROP_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val file = File(context.cacheDir, fileName)
        return Uri.fromFile(file)
    }
}
