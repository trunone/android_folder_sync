package com.example.foldersync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.runBlocking

interface SyncCallback {
    suspend fun onLog(message: String)
    suspend fun onProgress(message: String)
}

class SyncLogic(private val context: Context, private val callback: SyncCallback) {

    suspend fun syncFolder(sourceDir: DocumentFile, destDir: DocumentFile, useHash: Boolean) {
        val sourcePath = PathUtils.getPath(context, sourceDir.uri)
        val destPath = PathUtils.getPath(context, destDir.uri)

        if (sourcePath != null && destPath != null) {
            callback.onLog("Resolving paths for rsync...")
            callback.onLog("Source: $sourcePath")
            callback.onLog("Dest: $destPath")
            runRsync(sourcePath, destPath, useHash)
        } else {
            callback.onLog("Error: Could not resolve paths for rsync. Ensure you are selecting local storage.")
            callback.onLog("Source resolved: $sourcePath")
            callback.onLog("Dest resolved: $destPath")
        }
    }

    private suspend fun runRsync(sourcePath: String, destPath: String, useHash: Boolean) {
        val args = mutableListOf<String>()
        args.add("-av")
        args.add("--delete")
        args.add("--progress")
        if (useHash) {
            args.add("-c")
        }
        // Ensure trailing slash for source to copy contents, not the directory itself
        val finalSource = if (sourcePath.endsWith("/")) sourcePath else "$sourcePath/"
        args.add(finalSource)
        args.add(destPath)

        callback.onLog("Executing rsync command: rsync ${args.joinToString(" ")}")

        // Execute is blocking, but we are in suspend function.
        // We use runBlocking to bridge to suspend callback.
        // This is running in Dispatchers.IO from SyncActivity.
        val exitCode = RsyncHelper.execute(context, args) { line ->
            runBlocking {
                if (line.contains("%")) {
                    callback.onProgress(line.trim())
                } else {
                    callback.onLog(line)
                }
            }
        }

        if (exitCode == 0) {
            callback.onLog("Rsync finished successfully.")
        } else {
            callback.onLog("Rsync failed with exit code: $exitCode")
        }
    }
}
