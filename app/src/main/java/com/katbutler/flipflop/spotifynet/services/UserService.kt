package com.katbutler.flipflop.spotifynet.services

import com.katbutler.flipflop.spotifynet.models.UserProfile
import retrofit2.Call
import retrofit2.http.GET

/**
 * Created by kat on 2018-03-24.
 */
interface UserService {
    //  https://api.spotify.com/v1/me
    @GET("v1/me")
    fun getMe(): Call<UserProfile>
}