package com.katbutler.flipflop.adapters

import android.content.Context
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.katbutler.flipflop.R
import com.katbutler.flipflop.spotifynet.models.Playlists
import kotlinx.android.synthetic.main.playlist_item.view.*

/**
 * Created by kat on 2018-03-24.
 */
class PlaylistsAdapter(private val playlists: Playlists): RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {

    class ViewHolder(val context: Context, val playlistItemView: View) : RecyclerView.ViewHolder(playlistItemView) {
        val playlistNameTextView: TextView = playlistItemView.findViewById(R.id.playlist_name)
        val playlistTrackCountTextView: TextView = playlistItemView.findViewById(R.id.track_count)
        val playlistImageView: ImageView = playlistItemView.findViewById(R.id.playlist_image)
    }

    //region RecyclerView.Adapter methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun getItemCount() = playlists.total

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists.items.get(position)

        val smallImg = playlist.images.firstOrNull { it.height < 100 } ?: playlist.images.firstOrNull()
        smallImg?.let {
            val options = RequestOptions().placeholder(R.drawable.flipflop_icon_grey)

            Glide.with(holder.context)
                    .setDefaultRequestOptions(options)
                    .load(it.url)
                    .transition(withCrossFade())
                    .into(holder.playlistImageView)
        }
        holder.playlistNameTextView.text = playlist.name
        holder.playlistTrackCountTextView.text = "${playlist.tracks.total} tracks"
    }
    //endregion
}