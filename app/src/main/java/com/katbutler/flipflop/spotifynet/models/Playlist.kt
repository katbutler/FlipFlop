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

data class SpotifyImage(
        val height: Int,
        val width: Int,
        val url: String
)

data class PlaylistTracks(
        val count: Int,
        val href: String
)