package com.katbutler.flipflop.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by kat on 2018-03-24.
 */
object SpotifyPrefs {
    private const val SPOTIFY_TOKEN_KEY = "spotify_token"
    private const val SPOTIFY_USER_ID_KEY = "spotify_user_id"
    private const val PREF_KEY = "spotify_prefs"

    private fun getPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE) ?:
                    throw IllegalStateException("Cannot retrieve SharedPreferences")


    fun saveAccessToken(context: Context, token: String) {
        val sharedPref = getPrefs(context)
        with(sharedPref.edit()) {
            putString(SPOTIFY_TOKEN_KEY, token)
            apply()
        }
    }

    fun clearAccessToken(context: Context) {
        with(getPrefs(context).edit()) {
            remove(SPOTIFY_TOKEN_KEY)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? =
        getPrefs(context).getString(SPOTIFY_TOKEN_KEY, null)

    fun saveCurrentUserID(context: Context, userID: String) {
        val sharedPref = getPrefs(context)
        with(sharedPref.edit()) {
            putString(SPOTIFY_USER_ID_KEY, userID)
            apply()
        }
    }

    fun clearUserID(context: Context) {
        with(getPrefs(context).edit()) {
            remove(SPOTIFY_USER_ID_KEY)
            apply()
        }
    }

    fun getUserID(context: Context): String? =
            getPrefs(context).getString(SPOTIFY_USER_ID_KEY, null)
}