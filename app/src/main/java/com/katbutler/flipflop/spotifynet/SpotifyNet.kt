package com.katbutler.flipflop.spotifynet

import android.content.Context
import android.util.Log
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.katbutler.flipflop.spotifynet.models.Playlist
import com.katbutler.flipflop.spotifynet.models.UserProfile
import com.katbutler.flipflop.spotifynet.services.PlaylistService
import com.katbutler.flipflop.spotifynet.services.UserService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by kat on 2018-03-24.
 */
class SpotifyNet(val context: Context) {

    companion object {
        const val TAG = "SpotifyNet"
    }

    private val client by lazy {
        OkHttpClient.Builder().addInterceptor(AuthorizationInterceptor(context)).build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
    }

    private val userService by lazy {
        retrofit.create<UserService>(UserService::class.java)
    }

    private val playlistService by lazy {
        retrofit.create<PlaylistService>(PlaylistService::class.java)
    }

    //region API
    fun getCurrentUserProfile(callback: (UserProfile) -> Unit) {
        userService.getMe().enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        callback(it)
                    }
                }
            }

            override fun onFailure(call: Call<UserProfile>?, t: Throwable?) {
                Log.e(TAG, "getCurrentUserProfile onFailure")
            }
        })
    }

    fun getPlaylistsForCurrentUser(callback: (List<Playlist>) -> Unit) {
        playlistService.listMePlaylists().enqueue(object : Callback<List<Playlist>> {
            override fun onResponse(call: Call<List<Playlist>>, response: Response<List<Playlist>>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        callback(it)
                    }
                }
            }

            override fun onFailure(call: Call<List<Playlist>>?, t: Throwable?) {
                Log.e(TAG, "getPlaylistsForCurrentUser onFailure")
            }
        })
    }
    //endregion
}

class AuthorizationInterceptor(val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val accessToken = SpotifyPrefs.getAccessToken(context)

        val req: Request = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

        return chain.proceed(req)
    }

}