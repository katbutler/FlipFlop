package com.katbutler.flipflop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crashlytics.android.Crashlytics
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.katbutler.flipflop.services.IMediaPlayerCallback
import com.katbutler.flipflop.services.MediaPlayer
import com.katbutler.flipflop.spotifynet.models.Track
import com.spotify.sdk.android.player.*
import kotlinx.android.synthetic.main.activity_player.*
import android.support.v4.app.NavUtils
import android.view.MenuItem


class NoPlaylistError : Exception("playlist is needed for the player activity")

/**
 * Created by kat on 2018-03-25.
 */
class PlayerActivity : AppCompatActivity(), IMediaPlayerCallback {

    companion object {
        const val TAG = "PlayerActivity"

        const val EXTRA_LAUNCHED_FROM_MEDIA_NOTIFICATION = "extra.LAUNCHED_FROM_MEDIA_NOTIFICATION"

        private const val PLAYLIST_ID_1_KEY = "playlist1"
        private const val PLAYLIST_ID_2_KEY = "playlist2"

        fun intentFor(
                context: Context,
                playlistID1: String,
                playlistID2: String,
                isFromMediaNotification: Boolean = false,
                intentFlags: Int = Intent.FLAG_ACTIVITY_CLEAR_TOP) =
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_LAUNCHED_FROM_MEDIA_NOTIFICATION, isFromMediaNotification)
                    putExtra(PLAYLIST_ID_1_KEY, playlistID1)
                    putExtra(PLAYLIST_ID_2_KEY, playlistID2)
                    flags = intentFlags
                }
    }

    private val prefs by lazy { getSharedPreferences("player_prefs", Context.MODE_PRIVATE) }

    private val launchedFromMedia by lazy { intent.getBooleanExtra(EXTRA_LAUNCHED_FROM_MEDIA_NOTIFICATION, false) }
    private val playlistId1 by lazy { intent.getStringExtra(PLAYLIST_ID_1_KEY) ?: throw NoPlaylistError() }
    private val playlistId2 by lazy { intent.getStringExtra(PLAYLIST_ID_2_KEY) ?: throw NoPlaylistError() }
    private val mediaPlayer by lazy { MediaPlayer(this) { mediaPlayer ->
        val accessToken = SpotifyPrefs.getAccessToken(this) ?: return@MediaPlayer LoginActivity.showLoginActivity(this)
        mediaPlayer.registerCallbacks(this@PlayerActivity)
        if (!launchedFromMedia) {
            mediaPlayer.prepare(accessToken, playlistId1, playlistId2)
        } else {
            mediaPlayer.getCurrentTrack()?.let { track ->
                updateTrackInfo(track)
            }
        }
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        mediaPlayer.connect()

        initView()
    }

    override fun onDestroy() {
        Spotify.destroyPlayer(this)
        mediaPlayer.unregisterCallbacks(this)
        mediaPlayer.disconnect()
        super.onDestroy()
    }

    private fun initView() {

//        supportActionBar =

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        track_info_textview.isSelected = true
        track_info_textview.setHorizontallyScrolling(true)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(seek: SeekBar?) {
                val position = seek?.progress ?: 0
                mediaPlayer.seekToPosition(position)
            }

            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {

            }
        })

        play_pause_button.setOnClickListener {
            mediaPlayer.playPause()
        }

        skip_next_button.setOnClickListener {
            mediaPlayer.skipToNext()
        }

        skip_previous_button.setOnClickListener {
            mediaPlayer.skipToPrevious()
        }

        swap_button.setOnClickListener {
            mediaPlayer.swap()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                startActivity(Intent(this, FlipFlopActivity::class.java))
                finish()
//                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updatePlayPauseImage() {
        runOnUiThread {
            val playImage = R.drawable.ic_play_arrow_secondary_60dp
            val pauseImage = R.drawable.ic_pause_black_24dp
            val playPauseResId = if (mediaPlayer.isPlaying()) pauseImage else playImage
            play_pause_button.setImageResource(playPauseResId)
        }
    }

    override fun onLoggedOut() {
        SpotifyPrefs.clearAccessToken(this)

        LoginActivity.showLoginActivity(this)
    }

    override fun onLoginFailure() {
        SpotifyPrefs.clearAccessToken(this)

        LoginActivity.showLoginActivity(this)
    }

    override fun onTrackChanged(track: Track) {
        updateTrackInfo(track)
    }

    private fun updateTrackInfo(track: Track) {
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

    override fun updateProgress(progress: Int, totalProgress: Int) {
        seekBar.max = totalProgress
        seekBar.progress = progress
    }

    override fun onPlayerEvent(event: Int) {
        updatePlayPauseImage()
    }

    override fun asBinder(): IBinder? {
        return null
    }
}

