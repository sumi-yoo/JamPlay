package com.sumi.jamplay.ui.playlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.ItemPlaylistTrackBinding
import com.sumi.jamplay.domain.model.Track

data class PlaylistTrackRow(
    val track: Track,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
    val deleting: Boolean,
    val checked: Boolean
)

class PlaylistTrackAdapter(
    private val onClick: (PlaylistTrackRow) -> Unit,
    private val onChecked: (Long, Boolean) -> Unit
) : ListAdapter<PlaylistTrackRow, PlaylistTrackAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemPlaylistTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemPlaylistTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: PlaylistTrackRow) = with(binding) {
            name.text = row.track.name
            name.setTextColor(if (row.isCurrent) ContextCompat.getColor(root.context, R.color.jamplay_purple) else Color.WHITE)
            artist.text = row.track.artistName
            artwork.load(row.track.artworkUrl) {
                placeholder(R.drawable.ic_playlist)
                error(R.drawable.ic_playlist)
                fallback(R.drawable.ic_playlist)
                transformations(RoundedCornersTransformation(4 * root.resources.displayMetrics.density))
            }
            playingIndicator.isVisible = row.isCurrent
            playingIndicator.isPlaying = row.isCurrent && row.isPlaying
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isVisible = row.deleting
            checkbox.isChecked = row.checked
            checkbox.contentDescription = row.track.name
            checkbox.setOnCheckedChangeListener { _, checked -> onChecked(row.track.id, checked) }
            root.setOnClickListener { onClick(row) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PlaylistTrackRow>() {
        override fun areItemsTheSame(old: PlaylistTrackRow, new: PlaylistTrackRow) = old.track.id == new.track.id
        override fun areContentsTheSame(old: PlaylistTrackRow, new: PlaylistTrackRow) = old == new
    }
}
