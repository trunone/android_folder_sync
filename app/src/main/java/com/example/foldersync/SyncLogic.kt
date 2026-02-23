package com.example.foldersync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.runBlocking

interface SyncCallback {
    suspend fun onLog(message: String)
    suspend fun onProgress(message: String)
}

class SyncLogic(private val context: Context, private val callback: SyncCallback) {

    suspend fun syncFolder(sourceDir: DocumentFile, destDir: DocumentFile, rsyncArgs: String) {
        val sourcePath = PathUtils.getPath(context, sourceDir.uri)
        val destPath = PathUtils.getPath(context, destDir.uri)

        if (sourcePath != null && destPath != null) {
            callback.onLog("Resolving paths for rsync...")
            callback.onLog("Source: $sourcePath")
            callback.onLog("Dest: $destPath")
            runRsync(sourcePath, destPath, rsyncArgs)
        } else {
            callback.onLog("Error: Could not resolve paths for rsync. Ensure you are selecting local storage.")
            callback.onLog("Source resolved: $sourcePath")
            callback.onLog("Dest resolved: $destPath")
        }
    }

    private suspend fun runRsync(sourcePath: String, destPath: String, rsyncArgs: String) {
        val args = tokenizeArgs(rsyncArgs).toMutableList()

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

    private fun tokenizeArgs(command: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote = false
        var quoteChar = ' '

        for (char in command) {
            when {
                char == ' ' && !inQuote -> {
                    if (current.isNotEmpty()) {
                        args.add(current.toString())
                        current.clear()
                    }
                }
                (char == '"' || char == '\'') && !inQuote -> {
                    inQuote = true
                    quoteChar = char
                }
                (char == '"' || char == '\'') && inQuote && char == quoteChar -> {
                    inQuote = false
                }
                else -> {
                    current.append(char)
                }
            }
        }
        if (current.isNotEmpty()) {
            args.add(current.toString())
        }
        return args
    }
}
