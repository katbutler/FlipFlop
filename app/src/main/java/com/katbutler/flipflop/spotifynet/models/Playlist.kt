package com.katbutler.flipflop.spotifynet.models

/**
 * Created by kat on 2018-03-24.
 */
data class Playlist(
        val id: String,
        val images: List<SpotifyImage>,
        val name: String,
        val tracks: PlaylistTracks,
        val type: String,
        val uri: String
)

data class Playlists(
        val href: String,
        val items: List<Playlist>,
        val limit: Int,
        val offset: Int,
        val total: Int
)

data class SpotifyImage(
        val height: Int,
        val width: Int,
        val url: String
)

data class PlaylistTracks(
        val total: Int,
        val href: String
)