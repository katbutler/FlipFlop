package com.katbutler.flipflop.spotifynet

import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.katbutler.flipflop.spotifynet.models.Playlists
import com.katbutler.flipflop.spotifynet.models.Tracks
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

data class UnauthorizedException(override val message: String) : Exception(message)
data class UnknownSpotifyException(override val message: String) : Exception(message)

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
    fun getCurrentUserProfile(onSuccess: (UserProfile) -> Unit, onError: (Throwable?) -> Unit) {
        userService.getMe().enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it)
                        return
                    }
                }
                when (response.code()) {
                    401 -> onError(UnauthorizedException("Unauthorized"))
                    else -> {
                        logErrorResponse(response)
                        onError(UnknownSpotifyException("Unknown Error: ${response.code()} ${response.message()}"))
                    }
                }
            }

            override fun onFailure(call: Call<UserProfile>?, t: Throwable?) {
                Log.e(TAG, "getCurrentUserProfile onFailure")
                t?.printStackTrace()
                onError(t)
            }
        })
    }

    fun getPlaylistsForCurrentUser(onSuccess: (Playlists) -> Unit, onError: (Throwable?) -> Unit) {
        playlistService.listMePlaylists().enqueue(object : Callback<Playlists> {
            override fun onResponse(call: Call<Playlists>, response: Response<Playlists>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it)
                        return
                    }
                }

                when (response.code()) {
                    401 -> onError(UnauthorizedException("Unauthorized"))
                    else -> {
                        logErrorResponse(response)
                        onError(UnknownSpotifyException("Unknown Error: ${response.code()} ${response.message()}"))
                    }
                }
            }

            override fun onFailure(call: Call<Playlists>?, t: Throwable?) {
                Log.e(TAG, "getPlaylistsForCurrentUser onFailure")
                t?.printStackTrace()
                onError(t)
            }
        })
    }

    fun getPlaylistTracks(userID: String, playlistID: String, onSuccess: (Tracks) -> Unit, onError: (Throwable?) -> Unit) {
        playlistService.getPlaylistTracks(userID, playlistID).enqueue(object : Callback<Tracks> {
            override fun onResponse(call: Call<Tracks>, response: Response<Tracks>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it)
                        return
                    }
                }

                when (response.code()) {
                    401 -> onError(UnauthorizedException("Unauthorized"))
                    else -> {
                        logErrorResponse(response, "UserID: $userID   playlistID: $playlistID")
                        onError(UnknownSpotifyException("Unknown Error"))
                    }
                }
            }

            override fun onFailure(call: Call<Tracks>?, t: Throwable?) {
                Log.e(TAG, "getPlaylistTracks onFailure")
                t?.printStackTrace()
                onError(t)
            }
        })
    }

    private fun logErrorResponse(response: Response<*>, message: String = "") {
        val errorMessage = response.errorBody()?.string() ?: response.message()
        Crashlytics.log(Log.ERROR, TAG, "Unhandled response code: ${response.code()}; ${errorMessage ?: "No Message"} - $message")
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