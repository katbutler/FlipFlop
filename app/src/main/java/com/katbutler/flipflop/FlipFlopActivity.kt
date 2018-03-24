package com.katbutler.flipflop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.katbutler.flipflop.BuildConfig.CLIENT_ID
import com.spotify.sdk.android.authentication.LoginActivity.REQUEST_CODE
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.*


class FlipFlopActivity : AppCompatActivity(), ConnectionStateCallback, Player.NotificationCallback {

    companion object {
        const val TAG = "FlipFlopActivity"
        const val SPOTIFY_TOKEN_KEY = "spotify_token"
    }

    private val loginButton by lazy {
        findViewById<Button>(R.id.login_button)
    }

    var player: SpotifyPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_flop)

        loginButton.setOnClickListener {
                        spotifyLogin()
        }
    }

    //region Spotify Authentication
    private fun spotifyLogin() {
        val builder = AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                getRedirectUri().toString())
                .setScopes(arrayOf("user-read-private", "streaming"))
                .setShowDialog(true)
        val request = builder.build()

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE -> onLoginResult(resultCode, data)
            else -> Log.d(TAG, "Unhandled request code $requestCode")
        }
    }

    private fun onLoginResult(resultCode: Int, data: Intent?) {
        val response = AuthenticationClient.getResponse(resultCode, data)

        saveLoginToken(response.accessToken)

        val playerConfig = Config(this, response.accessToken, CLIENT_ID)
        Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
            override fun onInitialized(spotifyPlayer: SpotifyPlayer) {
                player = spotifyPlayer
                player?.addConnectionStateCallback(this@FlipFlopActivity)
                player?.addNotificationCallback(this@FlipFlopActivity)
            }

            override fun onError(throwable: Throwable) {
                Log.e(TAG, "Could not initialize player: " + throwable.message)
            }
        })
    }

    private fun getRedirectUri(): Uri {
        return Uri.Builder()
                .scheme(getString(R.string.com_spotify_sdk_redirect_scheme))
                .authority(getString(R.string.com_spotify_sdk_redirect_host))
                .build()
    }

    private fun saveLoginToken(token: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(SPOTIFY_TOKEN_KEY, token)
            apply()
        }
    }
    //endregion


    //region ConnectionStateCallback methods
    override fun onLoggedOut() {
        Log.d(TAG, "logged out")
    }

    override fun onLoggedIn() {
        Log.d(TAG, "logged in")
        player?.playUri(null, "spotify:track:2E6IvFl2GxvfFKoZWBtQso", 0, 0)
    }

    override fun onConnectionMessage(p0: String?) {
        Log.d(TAG, "$p0")
    }

    override fun onLoginFailed(p0: Error?) {
        Log.d(TAG, "$p0")
    }

    override fun onTemporaryError() {
        Log.d(TAG, "temp error")
    }
    //endregion


    //region Player.NotificationCallback methods
    override fun onPlaybackError(p0: Error?) {
        Log.d(TAG, "$p0")
    }

    override fun onPlaybackEvent(p0: PlayerEvent?) {
        Log.d(TAG, "$p0")
    }
    //endregion


}
