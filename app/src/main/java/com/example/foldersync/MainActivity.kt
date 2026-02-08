package com.example.foldersync

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var btnSource: Button
    private lateinit var tvSourcePath: TextView
    private lateinit var btnDest: Button
    private lateinit var tvDestPath: TextView
    private lateinit var rgCompareMode: RadioGroup
    private lateinit var btnSync: Button
    private lateinit var tvCurrentFile: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollView: ScrollView

    private var sourceUri: Uri? = null
    private var destUri: Uri? = null

    private val selectSourceLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            sourceUri = uri
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = DocumentFile.fromTreeUri(this, uri)
            tvSourcePath.text = file?.name ?: uri.path
            appendLog("Source selected: ${file?.name}")
        }
    }

    private val selectDestLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            destUri = uri
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = DocumentFile.fromTreeUri(this, uri)
            tvDestPath.text = file?.name ?: uri.path
            appendLog("Destination selected: ${file?.name}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSource = findViewById(R.id.btn_source)
        tvSourcePath = findViewById(R.id.tv_source_path)
        btnDest = findViewById(R.id.btn_dest)
        tvDestPath = findViewById(R.id.tv_dest_path)
        rgCompareMode = findViewById(R.id.rg_compare_mode)
        btnSync = findViewById(R.id.btn_sync)
        tvCurrentFile = findViewById(R.id.tv_current_file)
        tvLog = findViewById(R.id.tv_log)
        scrollView = findViewById(R.id.scrollView)

        btnSource.setOnClickListener {
            selectSourceLauncher.launch(null)
        }

        btnDest.setOnClickListener {
            selectDestLauncher.launch(null)
        }

        btnSync.setOnClickListener {
            startSync()
        }
    }

    private fun appendLog(message: String) {
        val currentText = tvLog.text.toString()
        tvLog.text = "$currentText\n$message"
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun startSync() {
        if (sourceUri == null || destUri == null) {
            appendLog("Source or Destination not selected.")
            return
        }

        val sourceDir = DocumentFile.fromTreeUri(this, sourceUri!!)
        val destDir = DocumentFile.fromTreeUri(this, destUri!!)

        if (sourceDir == null || destDir == null || !sourceDir.exists() || !destDir.exists()) {
            appendLog("Invalid source or destination directory.")
            return
        }

        val useHash = rgCompareMode.checkedRadioButtonId == R.id.rb_hash

        btnSync.isEnabled = false
        appendLog("Starting sync...")

        val callback = object : SyncCallback {
            override suspend fun onLog(message: String) {
                 withContext(Dispatchers.Main) {
                     appendLog(message)
                 }
            }
            override suspend fun onProgress(message: String) {
                 withContext(Dispatchers.Main) {
                     tvCurrentFile.text = message
                 }
            }
        }

        val syncLogic = SyncLogic(contentResolver, callback)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                syncLogic.syncFolder(sourceDir, destDir, useHash)
                withContext(Dispatchers.Main) {
                    appendLog("Sync completed successfully.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLog("Sync failed: ${e.message}")
                    e.printStackTrace()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnSync.isEnabled = true
                    tvCurrentFile.text = "Current File: Done"
                }
            }
        }
    }
}
