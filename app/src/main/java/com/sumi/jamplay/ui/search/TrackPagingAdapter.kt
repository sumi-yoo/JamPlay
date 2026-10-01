package com.sumi.jamplay.ui.search

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.sumi.jamplay.R
import com.sumi.jamplay.databinding.ItemSearchTrackBinding
import com.sumi.jamplay.domain.model.Track

class TrackPagingAdapter(private val onClick: (Track) -> Unit) : PagingDataAdapter<Track, TrackPagingAdapter.Holder>(Diff) {
    private var currentTrackId: Long? = null
    private var playing = false

    fun updatePlayback(trackId: Long?, isPlaying: Boolean) {
        if (currentTrackId == trackId && playing == isPlaying) return
        val previous = currentTrackId
        currentTrackId = trackId
        playing = isPlaying
        snapshot().forEachIndexed { index, track ->
            if (track != null && (track.id == previous || track.id == trackId)) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSearchTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemSearchTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: Track?) = with(binding) {
            name.text = track?.name
            artist.text = track?.artistName
            val current = track != null && track.id == currentTrackId
            name.setTextColor(if (current) ContextCompat.getColor(root.context, R.color.jamplay_purple) else Color.WHITE)
            artwork.load(track?.artworkUrl) {
                placeholder(R.drawable.ic_playlist)
                fallback(R.drawable.ic_playlist)
                error(R.drawable.ic_playlist)
                transformations(RoundedCornersTransformation(4 * root.resources.displayMetrics.density))
            }
            playingIndicator.isVisible = current
            playingIndicator.isPlaying = current && playing
            root.setOnClickListener { if (track != null) onClick(track) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(old: Track, new: Track) = old.id == new.id
        override fun areContentsTheSame(old: Track, new: Track) = old == new
    }
}
