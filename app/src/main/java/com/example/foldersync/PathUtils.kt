package com.example.foldersync

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object PathUtils {
    fun getPath(uri: Uri): String? {
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        try {
            if (DocumentsContract.isTreeUri(uri)) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                val path = if (split.size > 1) split[1] else ""

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + path
                } else {
                    // Handle SD card or other volumes
                    // Try to resolve volume by ID. Usually /storage/<UUID>
                    val storageDir = File("/storage/$type")
                    if (storageDir.exists() && storageDir.isDirectory) {
                        return "/storage/$type/$path"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
