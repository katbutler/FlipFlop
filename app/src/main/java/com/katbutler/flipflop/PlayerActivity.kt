package com.katbutler.flipflop

import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.katbutler.flipflop.spotifynet.SpotifyNet
import com.katbutler.flipflop.spotifynet.models.Track
import com.katbutler.flipflop.spotifynet.models.Tracks
import com.spotify.sdk.android.player.*
import kotlinx.android.synthetic.main.activity_player.*

/**
 * Created by kat on 2018-03-25.
 */
class PlayerActivity : AppCompatActivity(), ConnectionStateCallback, Player.NotificationCallback, Player.OperationCallback {

    companion object {
        const val TAG = "PlayerActivity"
    }

    val thread by lazy {
        val thread = HandlerThread("Queuer")
        thread.start()
        thread
    }
    val handler: Handler by lazy {
        object : Handler(thread.looper) {
            override fun handleMessage(msg: Message?) {
                if (msg?.what == 0xFEED) {
                    seekBar.progress = player.playbackState.positionMs.toInt()
                    handler.sendEmptyMessageDelayed(0xFEED, 1000)
                } else {
                    val trackUri = msg?.obj as? String
                    trackUri?.let { player.queue(this@PlayerActivity, it) }
                }
                return
            }
        }
    }

    lateinit var player: SpotifyPlayer

    private val spotifyNet by lazy {
        SpotifyNet(this)
    }

    private val playlistId1 by lazy { intent.getStringExtra("playlist1") }
    private val playlistId2 by lazy { intent.getStringExtra("playlist2") }
    private val playlistId1Uri by lazy { intent.getStringExtra("playlist1uri") }
    private val playlistId2Uri by lazy { intent.getStringExtra("playlist2uri") }
    private lateinit var playlist1Tracks: Tracks
    private lateinit var playlist2Tracks: Tracks
    private var hasFetched: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        initView()
        fetchTracks()

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

    var currentTrack: Track? = null

    private fun initView() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(seek: SeekBar?) {
                val progress = seek?.progress ?: 0
                currentTrack?.let {
                    player.seekToPosition(this@PlayerActivity, progress)
                }
            }

            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {

            }
        })

        play_pause_button.setOnClickListener {
            if (hasFetched) {
                if (player.playbackState.isPlaying) {
                    player.pause(this)
                } else {
                    currentTrack = playlist1Tracks.items.first()
                    player.playUri(this, currentTrack?.track?.uri, 0, 0)
                    seekBar.max = currentTrack?.track?.durationMs ?: Int.MAX_VALUE
                    seekBar.progress = 0
                    handler.sendEmptyMessageDelayed(0xFEED, 1000)

//                    playlist1Tracks.items
//                            .forEachIndexed { index, track ->
//                                if (index == 0) return@forEachIndexed
//                                Log.d("PlayerActivity", "track uri: ${track.track.uri}")
//                                handler.sendMessageDelayed(Message().apply {
//                                    obj = track.track.uri
//                                }, 300L * index)
//                            }

                }
            }
        }

        skip_next_button.setOnClickListener {
            if (hasFetched) {
                player.skipToNext(this)
            }
        }

        skip_previous_button.setOnClickListener {
            if (hasFetched) {
                player.skipToPrevious(this)
            }
        }
    }

    private fun fetchTracks() {
        val userID = SpotifyPrefs.getUserID(this) ?: return
        spotifyNet.getPlaylistTracks(userID, playlistId1, { tracks ->
            playlist1Tracks = tracks
            hasFetched = true
        }, { err ->
            Toast.makeText(this, err.toString(), Toast.LENGTH_LONG)
        })

        spotifyNet.getPlaylistTracks(userID, playlistId2, { tracks ->
            playlist2Tracks = tracks
            hasFetched = true
        }, { err ->
            Toast.makeText(this, err.toString(), Toast.LENGTH_LONG)
        })
    }

    //region Player.OperationCallback
    override fun onSuccess() {
        Log.d(TAG, "onSuccess")
    }

    override fun onError(err: Error?) {
        Log.d(TAG, err.toString())

        Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show()
    }
    //endregion

    //region ConnectionStateCallback methods
    override fun onLoggedOut() {
        Log.d(TAG, "logged out")
        SpotifyPrefs.clearAccessToken(this)

        LoginActivity.showLoginActivity(this)
    }

    override fun onLoggedIn() {
        Log.d(TAG, "logged in")
    }

    override fun onConnectionMessage(p0: String?) {
        Log.d(TAG, "onConnectionMessage $p0")
    }

    override fun onLoginFailed(err: Error?) {
        Log.d(TAG, "onLoginFailed $err")
        SpotifyPrefs.clearAccessToken(this)

        LoginActivity.showLoginActivity(this)
    }

    override fun onTemporaryError() {
        Log.d(TAG, "temp error")
    }
    //endregion


    //region Player.NotificationCallback methods
    override fun onPlaybackError(p0: Error?) {
        Log.d(TAG, "$p0")
    }

    override fun onPlaybackEvent(playerEvent: PlayerEvent?) {
        Log.d(TAG, "$playerEvent")
//        if (playerEvent?.name == "kSpPlaybackNotifyTrackChanged") {
//            player.resume(this)
//        }

        if (playerEvent == PlayerEvent.kSpPlaybackNotifyTrackDelivered) {

        }

    }
    //endregion

}