package com.katbutler.flipflop.spotifynet.models

/**
 * Created by kat on 2018-03-25.
 */
data class Track(
    val track: TrackData
)

data class Tracks(
        val href: String,
        val items: List<Track>,
        val limit: Int,
        val offset: Int,
        val total: Int
)

data class TrackData(
        val album: Album,
        val href: String,
        val id: String,
        val name: String,
        val uri: String
)

data class Album(
        val href: String,
        val id: String,
        val name: String,
        val uri: String,
        val images: List<SpotifyImage>
)