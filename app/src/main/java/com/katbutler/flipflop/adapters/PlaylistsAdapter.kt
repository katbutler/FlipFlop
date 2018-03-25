package com.katbutler.flipflop.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.katbutler.flipflop.R
import com.katbutler.flipflop.spotifynet.models.Playlists
import kotlinx.android.synthetic.main.playlist_item.view.*

/**
 * Created by kat on 2018-03-24.
 */
class PlaylistsAdapter(private val playlists: Playlists): RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {

    class ViewHolder(val playlistItemView: View) : RecyclerView.ViewHolder(playlistItemView) {
        val playlistNameTextView: TextView = playlistItemView.findViewById(R.id.playlist_name)
    }

    //region RecyclerView.Adapter methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = playlists.total

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists.items.get(position)
        holder.playlistNameTextView.text = playlist.name
    }
    //endregion
}