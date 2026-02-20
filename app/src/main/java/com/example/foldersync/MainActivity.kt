package com.example.foldersync

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var rvSyncPairs: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: SyncPairAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        rvSyncPairs = findViewById(R.id.rv_sync_pairs)
        fabAdd = findViewById(R.id.fab_add)

        adapter = SyncPairAdapter(emptyList(), { pair ->
            // On Item Click
            val intent = Intent(this, SyncActivity::class.java)
            intent.putExtra("SYNC_PAIR_ID", pair.id)
            startActivity(intent)
        }, { pair ->
            // On Delete Click
            SyncPairRepository.deleteSyncPair(this, pair.id)
            refreshList()
        })

        rvSyncPairs.layoutManager = LinearLayoutManager(this)
        rvSyncPairs.adapter = adapter

        fabAdd.setOnClickListener {
            val intent = Intent(this, SyncActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val list = SyncPairRepository.getAllSyncPairs(this)
        adapter.updateList(list)
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        } else {
            // For older versions
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            }
        }
    }
}
