package com.katbutler.flipflop

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.spotify.sdk.android.player.*
import kotlinx.android.synthetic.main.activity_player.*

/**
 * Created by kat on 2018-03-25.
 */
class PlayerActivity : AppCompatActivity(), ConnectionStateCallback, Player.NotificationCallback, Player.OperationCallback {

    lateinit var player: SpotifyPlayer

    private val playlistId1 by lazy { intent.getStringExtra("playlist1") }
    private val playlistId2 by lazy { intent.getStringExtra("playlist2") }
    private val playlistId1Uri by lazy { intent.getStringExtra("playlist1uri") }
    private val playlistId2Uri by lazy { intent.getStringExtra("playlist2uri") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        initView()

        val accessToken = SpotifyPrefs.getAccessToken(this) ?: return LoginActivity.showLoginActivity(this)

        val playerConfig = Config(this, accessToken, BuildConfig.CLIENT_ID)
        Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
            override fun onInitialized(spotifyPlayer: SpotifyPlayer) {
                player = spotifyPlayer
                player.addConnectionStateCallback(this@PlayerActivity)
                player.addNotificationCallback(this@PlayerActivity)
            }

            override fun onError(throwable: Throwable) {
                Log.e(FlipFlopActivity.TAG, "Could not initialize player: " + throwable.message)
            }
        })
    }

    override fun onDestroy() {
        Spotify.destroyPlayer(this)
        super.onDestroy()
    }

    private fun initView() {
        play_pause_button.setOnClickListener {
            if (player.playbackState.isPlaying) {
                player.pause(this)
            } else {
                player.playUri(this, playlistId1Uri, 0,0)
            }
        }
    }

    //region Player.OperationCallback
    override fun onSuccess() {

    }

    override fun onError(err: Error?) {
        Log.d("PlayerActivity", err.toString())

        Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show()
    }
    //endregion

    //region ConnectionStateCallback methods
    override fun onLoggedOut() {
        Log.d(FlipFlopActivity.TAG, "logged out")
        SpotifyPrefs.clearAccessToken(this)

        LoginActivity.showLoginActivity(this)
    }

    override fun onLoggedIn() {
        Log.d(FlipFlopActivity.TAG, "logged in")
    }

    override fun onConnectionMessage(p0: String?) {
        Log.d(FlipFlopActivity.TAG, "onConnectionMessage $p0")
    }

    override fun onLoginFailed(err: Error?) {
        Log.d(FlipFlopActivity.TAG, "onLoginFailed $err")
        SpotifyPrefs.clearAccessToken(this)

        LoginActivity.showLoginActivity(this)
    }

    override fun onTemporaryError() {
        Log.d(FlipFlopActivity.TAG, "temp error")
    }
    //endregion


    //region Player.NotificationCallback methods
    override fun onPlaybackError(p0: Error?) {
        Log.d(FlipFlopActivity.TAG, "$p0")
    }

    override fun onPlaybackEvent(p0: PlayerEvent?) {
        Log.d(FlipFlopActivity.TAG, "$p0")
    }
    //endregion

}