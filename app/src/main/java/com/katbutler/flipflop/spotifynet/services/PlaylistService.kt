package com.katbutler.flipflop.spotifynet.services

import com.katbutler.flipflop.spotifynet.models.Playlists
import retrofit2.Call
import retrofit2.http.GET


/**
 * Created by kat on 2018-03-24.
 */
interface PlaylistService {
    //	https://api.spotify.com/v1/me/playlists
    @GET("v1/me/playlists")
    fun listMePlaylists(): Call<Playlists>
}