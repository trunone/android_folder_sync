package com.example.foldersync

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest

interface SyncCallback {
    suspend fun onLog(message: String)
    suspend fun onProgress(message: String)
}

class SyncLogic(private val contentResolver: ContentResolver, private val callback: SyncCallback) {

    suspend fun syncFolder(sourceDir: DocumentFile, destDir: DocumentFile, useHash: Boolean, currentPath: String = "") {
        val files = sourceDir.listFiles()
        for (file in files) {
            val fileName = file.name ?: continue
            val filePath = if (currentPath.isEmpty()) fileName else "$currentPath/$fileName"

            if (file.isDirectory) {
                var destSubDir = destDir.findFile(fileName)

                if (destSubDir != null && !destSubDir.isDirectory) {
                    callback.onLog("Deleting file to replace with directory: $filePath")
                    if (!destSubDir.delete()) {
                        callback.onLog("Failed to delete file: $filePath")
                        continue
                    }
                    destSubDir = null
                }

                if (destSubDir == null) {
                    callback.onLog("Creating directory: $filePath")
                    destSubDir = destDir.createDirectory(fileName)
                }

                if (destSubDir != null) {
                     syncFolder(file, destSubDir, useHash, filePath)
                }
            } else {
                processFile(file, destDir, useHash, filePath)
            }
        }
    }

    private suspend fun processFile(sourceFile: DocumentFile, destDir: DocumentFile, useHash: Boolean, relativePath: String) {
        val fileName = sourceFile.name ?: return

        callback.onProgress("Processing: $relativePath")

        val destFile = destDir.findFile(fileName)

        if (destFile != null && destFile.isDirectory) {
             callback.onLog("Skipping file copy: Destination has directory with same name: $relativePath")
             return
        }

        if (destFile == null) {
            copyFile(sourceFile, destDir, relativePath)
        } else {
            if (useHash) {
                val sourceHash = calculateHash(sourceFile)
                val destHash = calculateHash(destFile)
                if (sourceHash != destHash) {
                    callback.onLog("Updating file (hash mismatch): $relativePath")
                    if (destFile.delete()) {
                        copyFile(sourceFile, destDir, relativePath)
                    } else {
                        callback.onLog("Failed to delete existing file: $relativePath")
                    }
                }
            }
        }
    }

    private suspend fun copyFile(sourceFile: DocumentFile, destDir: DocumentFile, relativePath: String) {
        val fileName = sourceFile.name ?: return
        callback.onLog("Copying: $relativePath")

        try {
            val mimeType = sourceFile.type ?: "application/octet-stream"
            val newFile = destDir.createFile(mimeType, fileName)
            if (newFile != null) {
                contentResolver.openInputStream(sourceFile.uri)?.use { input ->
                    contentResolver.openOutputStream(newFile.uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                callback.onLog("Failed to create file: $relativePath")
            }
        } catch (e: Exception) {
             callback.onLog("Error copying $relativePath: ${e.message}")
        }
    }

    private fun calculateHash(file: DocumentFile): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            contentResolver.openInputStream(file.uri)?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val bytes = digest.digest()
            val sb = StringBuilder()
            for (byte in bytes) {
                sb.append(String.format("%02x", byte))
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
