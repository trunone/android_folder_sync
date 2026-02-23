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
import java.util.UUID

class SyncActivity : AppCompatActivity() {

    private lateinit var btnSource: Button
    private lateinit var tvSourcePath: TextView
    private lateinit var btnDest: Button
    private lateinit var tvDestPath: TextView
    private lateinit var etRsyncArgs: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnSync: Button
    private lateinit var tvCurrentFile: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnSave: Button

    private var sourceUri: Uri? = null
    private var destUri: Uri? = null
    private var currentPairId: String? = null

    private val selectSourceLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            sourceUri = uri
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = DocumentFile.fromTreeUri(this, uri)
            val fullPath = PathUtils.getPath(this, uri)
            tvSourcePath.text = fullPath ?: file?.name ?: uri.path
            appendLog("Source selected: ${fullPath ?: file?.name}")
        }
    }

    private val selectDestLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            destUri = uri
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = DocumentFile.fromTreeUri(this, uri)
            val fullPath = PathUtils.getPath(this, uri)
            tvDestPath.text = fullPath ?: file?.name ?: uri.path
            appendLog("Destination selected: ${fullPath ?: file?.name}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)

        btnSource = findViewById(R.id.btn_source)
        tvSourcePath = findViewById(R.id.tv_source_path)
        btnDest = findViewById(R.id.btn_dest)
        tvDestPath = findViewById(R.id.tv_dest_path)
        etRsyncArgs = findViewById(R.id.et_rsync_args)
        btnSync = findViewById(R.id.btn_sync)
        tvCurrentFile = findViewById(R.id.tv_current_file)
        tvLog = findViewById(R.id.tv_log)
        scrollView = findViewById(R.id.scrollView)
        btnSave = findViewById(R.id.btn_save)

        btnSource.setOnClickListener {
            selectSourceLauncher.launch(null)
        }

        btnDest.setOnClickListener {
            selectDestLauncher.launch(null)
        }

        btnSync.setOnClickListener {
            startSync()
        }

        btnSave.setOnClickListener {
            saveSyncPair()
        }

        // Check for Intent Extras
        val pairId = intent.getStringExtra("SYNC_PAIR_ID")
        if (pairId != null) {
            currentPairId = pairId
            loadSyncPair(pairId)
        }
    }

    private fun loadSyncPair(id: String) {
        val pair = SyncPairRepository.getSyncPair(this, id)
        if (pair != null) {
            sourceUri = Uri.parse(pair.sourceUri)
            destUri = Uri.parse(pair.destUri)

            // Try to get display names
            val sourceFile = DocumentFile.fromTreeUri(this, sourceUri!!)
            val destFile = DocumentFile.fromTreeUri(this, destUri!!)
            val sourcePath = PathUtils.getPath(this, sourceUri!!)
            val destPath = PathUtils.getPath(this, destUri!!)

            tvSourcePath.text = sourcePath ?: sourceFile?.name ?: pair.sourceUri
            tvDestPath.text = destPath ?: destFile?.name ?: pair.destUri

            val args = if (pair.rsyncArgs.isNotEmpty()) {
                pair.rsyncArgs
            } else {
                if (pair.useHash) "-av --delete --progress -c" else "-av --delete --progress"
            }
            etRsyncArgs.setText(args)

            appendLog("Loaded Sync Pair: ${pair.name}")
        }
    }

    private fun saveSyncPair() {
        if (sourceUri == null || destUri == null) {
            appendLog("Cannot save: Source or Destination not selected.")
            return
        }

        val sourceFile = DocumentFile.fromTreeUri(this, sourceUri!!)
        val destFile = DocumentFile.fromTreeUri(this, destUri!!)
        val name = "${sourceFile?.name ?: "Source"} -> ${destFile?.name ?: "Dest"}"
        val rsyncArgs = etRsyncArgs.text.toString()
        val useHash = rsyncArgs.contains("-c")

        // If we are editing an existing pair, reuse its ID. Otherwise create a new one.
        val idToSave = currentPairId ?: UUID.randomUUID().toString()

        val pair = SyncPair(
            id = idToSave,
            name = name,
            sourceUri = sourceUri.toString(),
            destUri = destUri.toString(),
            useHash = useHash,
            rsyncArgs = rsyncArgs
        )

        SyncPairRepository.saveSyncPair(this, pair)
        finish()
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

        val rsyncArgs = etRsyncArgs.text.toString()

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

        val syncLogic = SyncLogic(this@SyncActivity, callback)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                syncLogic.syncFolder(sourceDir, destDir, rsyncArgs)
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
