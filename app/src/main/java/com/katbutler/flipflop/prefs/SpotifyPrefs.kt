package com.katbutler.flipflop.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by kat on 2018-03-24.
 */
object SpotifyPrefs {
    private const val SPOTIFY_TOKEN_KEY = "spotify_token"
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
}