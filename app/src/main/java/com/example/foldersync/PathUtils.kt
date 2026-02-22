package com.example.foldersync

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import java.io.File

object PathUtils {
    fun getPath(context: Context, uri: Uri): String? {
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
                    // Try to find volume by UUID using StorageManager
                    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    val volumes = storageManager.storageVolumes
                    for (volume in volumes) {
                        val uuid = volume.uuid
                        if (uuid != null && uuid == type) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                val dir = volume.directory
                                if (dir != null) {
                                    return File(dir, path).absolutePath
                                }
                            } else {
                                // Reflection for older APIs
                                try {
                                    val getPathMethod = volume.javaClass.getMethod("getPath")
                                    val volumePath = getPathMethod.invoke(volume) as String
                                    return File(volumePath, path).absolutePath
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    // Fallback to /storage/UUID if not found in StorageManager
                    val storageDir = File("/storage/$type")
                    if (storageDir.exists() && storageDir.isDirectory) {
                        return File(storageDir, path).absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
