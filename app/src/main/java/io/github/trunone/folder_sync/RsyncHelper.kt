package io.github.trunone.folder_sync

import android.content.Context
import java.io.File

object RsyncHelper {

    fun execute(context: Context, args: List<String>, callback: (String) -> Unit): Int {
        // The binary is packaged as a native library so it is extracted to nativeLibraryDir
        // Note: The file must be named lib<name>.so for Android to extract it.
        val rsyncFile = File(context.applicationInfo.nativeLibraryDir, "librsync.so")

        if (!rsyncFile.exists()) {
             callback("Error: rsync binary not found at ${rsyncFile.absolutePath}")
             return -1
        }

        // Check if executable
        if (!rsyncFile.canExecute()) {
            callback("Error: rsync binary is not executable at ${rsyncFile.absolutePath}")
            // Try to fix permissions? Usually not possible in nativeLibraryDir on newer Android
            // But usually it IS executable by default.
            return -1
        }

        val command = mutableListOf(rsyncFile.absolutePath)
        command.addAll(args)

        try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    callback(line!!)
                }
            }

            return process.waitFor()
        } catch (e: Exception) {
            callback("Error executing rsync: ${e.message}")
            return -1
        }
    }
}
