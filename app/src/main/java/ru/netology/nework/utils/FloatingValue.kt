package ru.netology.nework.utils

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap


object FloatingValue {
    var textNewPost = ""
    var currentFragment = ""

    fun convertDatePublished(dateString: String): String {
        val date = dateString.substring(0..9)
        val time = dateString.substring(11..18)
        return "$date $time"
    }

    fun getExtensionFromUri(uri: Uri, contentResolver: ContentResolver): String? {
        val mimeType = contentResolver.getType(uri) ?: return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }
}