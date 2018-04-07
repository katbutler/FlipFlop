package com.katbutler.flipflop

import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
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
                } else if (msg?.what == 0xDEAF) {
                    initFirstTrack()
                } else {
                    val trackUri = msg?.obj as? String
                    trackUri?.let { player.queue(this@PlayerActivity, it) }
                }
                return
            }
        }
    }

    lateinit var player: SpotifyPlayer
    lateinit var currentPlaylistID: String
    var currentTrack: Track? = null
    var swapTrack: Track? = null

    private val spotifyNet by lazy {
        SpotifyNet(this)
    }

    private val playlistId1 by lazy { intent.getStringExtra("playlist1") }
    private val playlistId2 by lazy { intent.getStringExtra("playlist2") }
    private val swapLists by lazy { listOf<String>(playlistId1, playlistId2) }
    private val playlistId1Uri by lazy { intent.getStringExtra("playlist1uri") }
    private val playlistId2Uri by lazy { intent.getStringExtra("playlist2uri") }
    private val playlistTracks = mutableMapOf<String, Tracks>()
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

    private fun initView() {
        track_info_textview.isSelected = true
        track_info_textview.setHorizontallyScrolling(true)
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
                    player.resume(this)
                }
            }
        }

        skip_next_button.setOnClickListener {
            if (hasFetched) {
                playNextTrack()
            }
        }

        skip_previous_button.setOnClickListener {
            if (hasFetched) {
                playPrevTrack()
            }
        }

        swap_button.setOnClickListener {
            if (hasFetched) {
                val tempTrack = swapTrack
                swapTrack = currentTrack
                currentTrack = tempTrack

                currentPlaylistID = swapLists.first { it != currentPlaylistID }
                val tracks = playlistTracks[currentPlaylistID]
                val nextTrack: Track? = currentTrack ?: tracks?.items?.firstOrNull()
                nextTrack?.let {
                    playTrack(it)
                }
            }
        }
    }

    private fun updatePlayPauseImage() {
        runOnUiThread {
            val playImage = R.drawable.ic_play_arrow_secondary_60dp
            val pauseImage = R.drawable.ic_pause_black_24dp
            val playPauseResId = if (player.playbackState.isPlaying) pauseImage else playImage
            play_pause_button.setImageResource(playPauseResId)
        }
    }

    private fun initFirstTrack() {
        handler.sendEmptyMessageDelayed(0xFEED, 1000)
        playlist1Tracks.items.firstOrNull()?.let {
            playTrack(it)
        }
    }

    private fun playTrack(track: Track) {
        currentTrack = track
        player.playUri(this, track.track.uri, 0, 0)
        seekBar.max = track.track.durationMs
        seekBar.progress = 0

        this.runOnUiThread {
            val trackInfo = "${track.track.name} - ${track.track.artists.joinToString(", ") { it.name }}"
            track_info_textview.text = trackInfo
            Glide.with(this)
                    .load(track.track.album.images.first { it.height > 500 }.url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(trackArtImageView)

            trackArtImageView.alpha = 0.9F
        }
    }

    private fun playNextTrack() {
        val tracks = playlistTracks[currentPlaylistID]
        val newHead: List<Track>? = tracks?.items?.dropWhile { it.track.id != currentTrack?.track?.id }?.drop(1)
        (newHead?.firstOrNull() ?: tracks?.items?.firstOrNull())?.let {
            playTrack(it)
        }
    }

    private fun playPrevTrack() {
        val tracks = playlistTracks[currentPlaylistID]
        val reversedItems = tracks?.items?.reversed()
        val newHead: List<Track>? = reversedItems?.dropWhile { it.track.id != currentTrack?.track?.id }?.drop(1)
        (newHead?.firstOrNull() ?: reversedItems?.firstOrNull())?.let {
            playTrack(it)
        }
    }

    private fun fetchTracks() {
        val userID = SpotifyPrefs.getUserID(this) ?: return
        currentPlaylistID = playlistId1

        spotifyNet.getPlaylistTracks(userID, playlistId1, { tracks ->
            playlist1Tracks = tracks
            hasFetched = true
            playlistTracks[playlistId1] = tracks
            handler.sendEmptyMessageDelayed(0xDEAF, 1000)
        }, { err ->
            Toast.makeText(this, err.toString(), Toast.LENGTH_LONG)
        })

        spotifyNet.getPlaylistTracks(userID, playlistId2, { tracks ->
            playlist2Tracks = tracks
            hasFetched = true
            playlistTracks[playlistId2] = tracks
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
            playNextTrack()
        }
        updatePlayPauseImage()
    }
//endregion

}