package com.sumi.jamplay.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.ItemPlaylistBinding
import com.sumi.jamplay.domain.model.Playlist

data class PlaylistRow(
    val playlist: Playlist,
    val showCheckbox: Boolean = false,
    val checked: Boolean = false,
    val enabled: Boolean = true
)

class PlaylistAdapter(
    private val onClick: (PlaylistRow) -> Unit,
    private val onChecked: (Long, Boolean) -> Unit
) : ListAdapter<PlaylistRow, PlaylistAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: PlaylistRow) = with(binding) {
            name.text = row.playlist.name
            count.text = root.context.getString(R.string.playlist_track_count, row.playlist.tracks.size)
            artwork.load(row.playlist.tracks.firstOrNull()?.artworkUrl) {
                placeholder(R.drawable.ic_playlist)
                fallback(R.drawable.ic_playlist)
                error(R.drawable.ic_playlist)
                transformations(RoundedCornersTransformation(4 * root.resources.displayMetrics.density))
            }
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isVisible = row.showCheckbox
            checkbox.isChecked = row.checked
            checkbox.contentDescription = row.playlist.name
            checkbox.setOnCheckedChangeListener { _, checked -> onChecked(row.playlist.id, checked) }
            arrow.isVisible = !row.showCheckbox
            root.isEnabled = row.enabled
            root.setOnClickListener { if (row.enabled) onClick(row) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PlaylistRow>() {
        override fun areItemsTheSame(old: PlaylistRow, new: PlaylistRow) = old.playlist.id == new.playlist.id
        override fun areContentsTheSame(old: PlaylistRow, new: PlaylistRow) = old == new
    }
}
