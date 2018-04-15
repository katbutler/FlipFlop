package com.katbutler.flipflop

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

/**
 * Created by kat on 2018-03-25.
 */
class PlayerActivity : AppCompatActivity(), IMediaPlayerCallback {

    companion object {
        const val TAG = "PlayerActivity"
    }

    private val playlistId1 by lazy { intent.getStringExtra("playlist1") }
    private val playlistId2 by lazy { intent.getStringExtra("playlist2") }
    private val mediaPlayer by lazy { MediaPlayer(this) {
        val accessToken = SpotifyPrefs.getAccessToken(this) ?: return@MediaPlayer LoginActivity.showLoginActivity(this)
        it.registerCallbacks(this@PlayerActivity)
        it.prepare(accessToken, playlistId1, playlistId2)
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