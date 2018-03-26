package com.katbutler.flipflop.spotifynet.models

import com.google.gson.annotations.SerializedName

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
        val uri: String,
        @SerializedName("duration_ms") val durationMs: Int
)

data class Album(
        val href: String,
        val id: String,
        val name: String,
        val uri: String,
        val images: List<SpotifyImage>
)