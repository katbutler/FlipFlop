package com.katbutler.flipflop.spotifynet.models

import com.google.gson.annotations.SerializedName

/**
 * Created by kat on 2018-03-24.
 */
data class UserProfile(
        val id: String,
        @SerializedName("display_name") val displayName: String,
        val images: List<SpotifyImage>,
        val product: String,
        val type: String,
        val uri: String
)