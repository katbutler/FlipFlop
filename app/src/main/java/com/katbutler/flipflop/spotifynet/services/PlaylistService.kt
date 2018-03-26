package com.katbutler.flipflop.spotifynet.services

import com.katbutler.flipflop.spotifynet.models.Playlists
import com.katbutler.flipflop.spotifynet.models.Tracks
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


/**
 * Created by kat on 2018-03-24.
 */
interface PlaylistService {
    //	https://api.spotify.com/v1/me/playlists
    @GET("v1/me/playlists")
    fun listMePlaylists(): Call<Playlists>

    //  https://api.spotify.com/v1/users/{user_id}/playlists/{playlist_id}/tracks
    @GET("v1/users/{user_id}/playlists/{playlist_id}/tracks")
    fun getPlaylistTracks(@Path("user_id") userId: String, @Path("playlist_id") playlistId: String): Call<Tracks>
}