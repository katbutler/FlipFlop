package com.katbutler.flipflop

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.spotify.sdk.android.player.*
import com.spotify.sdk.android.player.Spotify

class FlipFlopActivity : AppCompatActivity(), ConnectionStateCallback, Player.NotificationCallback {

    companion object {
        const val TAG = "FlipFlopActivity"
    }

    var player: SpotifyPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flip_flop)
        Log.d(TAG, "onCreate")
        initSpotify()
    }

    override fun onDestroy() {
        Spotify.destroyPlayer(this)
        super.onDestroy()
    }


    //region ConnectionStateCallback methods
    override fun onLoggedOut() {
        Log.d(TAG, "logged out")
        SpotifyPrefs.clearAccessToken(this)

        showLoginActivity()
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


    private fun initSpotify() {
        val accessToken = SpotifyPrefs.getAccessToken(this) ?: return showLoginActivity()


        val playerConfig = Config(this, accessToken, BuildConfig.CLIENT_ID)
        Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
            override fun onInitialized(spotifyPlayer: SpotifyPlayer) {
                player = spotifyPlayer
                player?.addConnectionStateCallback(this@FlipFlopActivity)
                player?.addNotificationCallback(this@FlipFlopActivity)
            }

            override fun onError(throwable: Throwable) {
                Log.e(FlipFlopActivity.TAG, "Could not initialize player: " + throwable.message)
            }
        })
    }

    private fun showLoginActivity() {
        val loginIntent = Intent(this, LoginActivity::class.java)

        startActivity(loginIntent)
    }
}
