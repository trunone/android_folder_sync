package com.example.foldersync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SyncPairAdapter(
    private var pairs: List<SyncPair>,
    private val onItemClick: (SyncPair) -> Unit,
    private val onDeleteClick: (SyncPair) -> Unit
) : RecyclerView.Adapter<SyncPairAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvSource: TextView = view.findViewById(R.id.tv_source)
        val tvDest: TextView = view.findViewById(R.id.tv_dest)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sync_pair, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pair = pairs[position]
        holder.tvName.text = pair.name
        holder.tvSource.text = "Source: ${pair.sourceUri}"
        holder.tvDest.text = "Dest: ${pair.destUri}"

        holder.itemView.setOnClickListener { onItemClick(pair) }
        holder.btnDelete.setOnClickListener { onDeleteClick(pair) }
    }

    override fun getItemCount() = pairs.size

    fun updateList(newPairs: List<SyncPair>) {
        pairs = newPairs
        notifyDataSetChanged()
    }
}
