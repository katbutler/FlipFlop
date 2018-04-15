package com.katbutler.flipflop.services


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.content.BroadcastReceiver
import android.content.Context
import android.media.AudioAttributes
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.content.ComponentName
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.os.*
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.app.NotificationCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART
import android.view.KeyEvent
import android.widget.Toast
import com.katbutler.flipflop.*
import com.katbutler.flipflop.BuildConfig
import com.katbutler.flipflop.R
import com.katbutler.flipflop.helpers.MediaStyleHelper
import com.katbutler.flipflop.prefs.SpotifyPrefs
import com.katbutler.flipflop.spotifynet.SpotifyNet
import com.katbutler.flipflop.spotifynet.models.Track
import com.katbutler.flipflop.spotifynet.models.Tracks
import com.spotify.sdk.android.player.*
import java.net.URL

data class MediaPlayerServiceException(override val message: String) : Exception()

class MediaPlayerService : Service(),
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener,
        ConnectionStateCallback,
        Player.NotificationCallback,
        Player.OperationCallback {

    companion object {
        private const val TAG = "MediaPlayerService"
        private const val MEDIA_PLAYBACK_CHANNEL_ID = "com.katbutler.flipflop.media_playback"
        private const val MEDIA_PLAYBACK_NOTIFICATION_ID = 0xEEEE
        private const val ACTION_TEAR_DOWN = "com.katbutler.flipflop.action.TEAR_DOWN"
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()

    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            Log.d(TAG, "onReceive ${intent?.action}")
            mediaPlayer?.let {
                if (it.isPlaying) it.pause()
            }
        }
    }

    val thread by lazy {
        val thread = HandlerThread("ProgressUpdater")
        thread.start()
        thread
    }
    val handler: Handler by lazy {
        object : Handler(thread.looper) {
            override fun handleMessage(msg: Message?) {
                when {
                    msg?.what == 0xFEED -> {
                        val progress = player?.playbackState?.positionMs?.toInt()
                        progress?.let {
                            notifyUpdateProgressChanged(progress, currentTrack?.track?.durationMs ?: progress)
                        }
                        handler.sendEmptyMessageDelayed(0xFEED, 1000)
                    }
                    msg?.what == 0xDEAF -> initFirstTrack()
                    msg?.what == 0xABBA -> updateNotificationMetadata()
                    else -> {
                        Log.w(TAG, "Unknown message what ${msg?.what}")
                    }
                }
                return
            }
        }
    }

    var player: SpotifyPlayer? = null
    lateinit var currentPlaylistID: String
    var currentTrack: Track? = null
    var swapTrack: Track? = null
    private val playlistTracks = mutableMapOf<String, Tracks>()
    private lateinit var playlist1Tracks: Tracks
    private lateinit var playlist2Tracks: Tracks
    private var hasFetched: Boolean = false

    private val spotifyNet by lazy {
        SpotifyNet(this)
    }

    private val mediaButtonReceiver = MediaButtonReceiver()

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPrepare() {
            Log.d(TAG, "MediaSession - onPrepare")
            super.onPrepare()
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            Log.d(TAG, "MediaSession - onCommand")
            super.onCommand(command, extras, cb)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            Log.d(TAG, "MediaSession- onMediaButtonEvent")
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            when (keyEvent?.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { // SWAP
                    mediaPlayerBinding.swap()
                }

                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    mediaPlayerBinding.playPause()
                }

                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    mediaPlayerBinding.playPause()
                }

                KeyEvent.KEYCODE_MEDIA_NEXT-> {
                    mediaPlayerBinding.skipToNext()
                }

                KeyEvent.KEYCODE_MEDIA_PREVIOUS-> {
                    mediaPlayerBinding.skipToPrevious()
                }
                else -> {

                }
            }

            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlay() {
            Log.d(TAG, "MediaSession - onPlay")

            if (!successfullyRetrievedAudioFocus()) {
                Log.d("MediaPlayerService", "Could not retrieve audio focus")
                return
            }

            mediaSessionCompat.isActive = true
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)

            mediaPlayer.start()
        }

        override fun onPause() {
            Log.d(TAG, "MediaSession - onPause")
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            mediaPlayer.pause()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Log.d(TAG, "MediaSession - onPlayFromMediaId")
        }

        override fun onSkipToNext() {
            Log.d(TAG, "MediaSession - onSkipToNext")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        registerReceiver(mediaButtonReceiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))

        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()

        updateNotification()
    }

    override fun onBind(intent: Intent?): IBinder = mediaPlayerBinding

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(mediaButtonReceiver)
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (intent?.action == ACTION_TEAR_DOWN) {
            if (callbackHandlers.size == 0) {
                stopSelf()
            }
        }
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        Log.d(TAG, "onCompletion")
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.d(TAG, "onAudioFocusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                }
                mediaPlayer.setVolume(1.0f, 1.0f)
            }
        }
    }

    private fun initMediaPlayer() {
        Log.d(TAG, "initMediaPlayer")
        mediaPlayer = MediaPlayer()
        mediaPlayer.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.setVolume(1.0f, 1.0f)
    }

    private fun initMediaSession() {
        Log.d(TAG, "initMediaSession")
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSessionCompat = MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)

        mediaSessionCompat.setCallback(mediaSessionCallback)
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSessionCompat.isActive = true


        val metadata = MediaMetadataCompat.Builder()
                .build()
        mediaSessionCompat.setMetadata(metadata)


        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent)
    }

    private fun initNoisyReceiver() {
        Log.d(TAG, "initNoisyReceiver")
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
    }

    private fun successfullyRetrievedAudioFocus(): Boolean {
        Log.d(TAG, "successfullyRetrievedAudioFocus")
        val audioManager = getSystemService(AudioManager::class.java)

        val result = if (Build.VERSION.SDK_INT >= 26) {
            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .setAudioAttributes(audioAttributes)
                    .build()
            audioManager.requestAudioFocus(focusReq)
        } else {
            audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    private fun setMediaPlaybackState(state: Int) {
        Log.d(TAG, "setMediaPlaybackState")
        val playbackStateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
        } else {
            playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
        }
        playbackStateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mediaSessionCompat.setPlaybackState(playbackStateBuilder.build())
    }

    private fun updateNotification(isPlaying: Boolean = mediaPlayerBinding.isPlaying) {
        Log.d(TAG, "updateNotification")
        val notificationBuilder = MediaStyleHelper.from(this@MediaPlayerService, mediaSessionCompat)
                .addAction(android.support.v4.app.NotificationCompat.Action(
                        R.drawable.ic_skip_previous_black_24dp,
                        "Prev",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                .addAction(android.support.v4.app.NotificationCompat.Action(
                        if (isPlaying) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_black_24dp,
                        if (isPlaying) "Pause" else "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY)))
                .addAction(android.support.v4.app.NotificationCompat.Action(
                        R.drawable.ic_swap_secondary_24dp,
                        "Swap",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                .addAction(android.support.v4.app.NotificationCompat.Action(
                        R.drawable.ic_skip_next_black_24dp,
                        "Next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
                .setStyle(NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(*arrayOf(1, 2, 3).toIntArray())
                        .setMediaSession(mediaSessionCompat.sessionToken))
                .setSmallIcon(R.drawable.filpflop_cutout_svg)
                .setAutoCancel(false)
                .setOngoing(isPlaying)
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0x1,
                                Intent(this, PlayerActivity::class.java).apply {
                                    putExtra(PlayerActivity.EXTRA_LAUNCHED_FROM_MEDIA_NOTIFICATION, true)
                                },
                                PendingIntent.FLAG_CANCEL_CURRENT
                        )
                )
                .setDeleteIntent(
                        PendingIntent.getService(
                        this,
                                0xDE1,
                                Intent(ACTION_TEAR_DOWN).apply {
                                    component = ComponentName(this@MediaPlayerService, MediaPlayerService::class.java)
                                },
                                PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            val mediaPlaybackChannel = NotificationChannel(
                    MEDIA_PLAYBACK_CHANNEL_ID,
                    getString(R.string.media_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(mediaPlaybackChannel)

            notificationBuilder
                    .setChannelId(MEDIA_PLAYBACK_CHANNEL_ID)
                    .build()
        } else {
            notificationBuilder.build()
        }

        if (!isPlaying) {
            NotificationManagerCompat.from(this@MediaPlayerService).notify(MEDIA_PLAYBACK_NOTIFICATION_ID, notification)
            stopForeground(false)
        } else {
            startForeground(MEDIA_PLAYBACK_NOTIFICATION_ID, notification)
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
        player?.playUri(this, track.track.uri, 0, 0)
                ?: throw MediaPlayerServiceException("Player not initialized")
        notifyUpdateProgressChanged(0, track.track.durationMs);

        notifyOnTrackChanged(track)

        handler.sendEmptyMessage(0xABBA)
    }

    private fun updateNotificationMetadata() {
        if (mediaSessionCompat == null) return
        val track = currentTrack?.track ?: return

        val albumArtImage = track.album.images.firstOrNull()

        val art = albumArtImage?.let {
            BitmapFactory.decodeStream(URL(albumArtImage.url).openStream())
        }

        val metadata = MediaMetadataCompat.Builder()
                .putBitmap(METADATA_KEY_ALBUM_ART, art)
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, track.album.name)
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, track.artists.joinToString { it.name })
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.name)
                .build()
        mediaSessionCompat.setMetadata(metadata)

        updateNotification()
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

    private fun fetchTracks(playlistID1: String, playlistID2: String) {
        val userID = SpotifyPrefs.getUserID(this) ?: return
        currentPlaylistID = playlistID1

        spotifyNet.getPlaylistTracks(userID, playlistID1, { tracks ->
            playlist1Tracks = tracks
            hasFetched = true
            playlistTracks[playlistID1] = tracks
            handler.sendEmptyMessageDelayed(0xDEAF, 1000)
        }, { err ->
            Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show()
        })

        spotifyNet.getPlaylistTracks(userID, playlistID2, { tracks ->
            playlist2Tracks = tracks
            hasFetched = true
            playlistTracks[playlistID2] = tracks
        }, { err ->
            Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show()
        })
    }

    //region Player.OperationCallback
    override fun onSuccess() {
        Log.d(PlayerActivity.TAG, "onSuccess")
    }

    override fun onError(err: Error?) {
        Log.d(PlayerActivity.TAG, err.toString())

        Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show()
    }
    //endregion

    //region ConnectionStateCallback methods
    override fun onLoggedOut() {
        Log.d(PlayerActivity.TAG, "logged out")
        notificationManager.cancel(MEDIA_PLAYBACK_NOTIFICATION_ID)
        notifyOnLoggedOutListeners()
    }

    override fun onLoggedIn() {
        Log.d(PlayerActivity.TAG, "logged in")
    }

    override fun onConnectionMessage(p0: String?) {
        Log.d(PlayerActivity.TAG, "onConnectionMessage $p0")
    }

    override fun onLoginFailed(err: Error?) {
        Log.d(PlayerActivity.TAG, "onLoginFailed $err")
        notifyLoginFailureListeners()
    }

    override fun onTemporaryError() {
        Log.d(PlayerActivity.TAG, "temp error")
    }
    //endregion

    //region Player.NotificationCallback methods
    override fun onPlaybackError(p0: Error?) {
        Log.d(PlayerActivity.TAG, "$p0")
    }

    override fun onPlaybackEvent(playerEvent: PlayerEvent?) {
        Log.d(PlayerActivity.TAG, "$playerEvent")

        when (playerEvent) {
            PlayerEvent.kSpPlaybackNotifyPlay -> {
                updateNotification(true)
            }
            PlayerEvent.kSpPlaybackNotifyTrackDelivered -> {
                playNextTrack()
            }
            else -> { }
        }

        playerEvent?.let {
            notifyPlayerEventListeners(it)
        }
    }
    //endregion


    //region Media Player Binding
    private val callbackHandlers = mutableListOf<IMediaPlayerCallback>()

    private fun notifyPlayerEventListeners(playerEvent: PlayerEvent) {
        callbackHandlers.forEach {
            it.onPlayerEvent(playerEvent.ordinal)
        }
    }

    private fun notifyOnLoggedOutListeners() {
        callbackHandlers.forEach {
            it.onLoggedOut()
        }
    }

    private fun notifyLoginFailureListeners() {
        callbackHandlers.forEach {
            it.onLoginFailure()
        }
    }

    private fun notifyUpdateProgressChanged(progress: Int, totalProgress: Int) {
        callbackHandlers.forEach {
            it.updateProgress(progress, totalProgress)
        }
    }

    private fun notifyOnTrackChanged(track: Track) {
        callbackHandlers.forEach {
            it.onTrackChanged(track)
        }
    }

    private val mediaPlayerBinding = object : IMediaPlayerService.Stub() {

        override fun prepare(accessToken: String, playlistID1: String, playlistID2: String) {
            Log.d(TAG, "be prepared")

            fetchTracks(playlistID1, playlistID2)

            val playerConfig = Config(this@MediaPlayerService, accessToken, BuildConfig.CLIENT_ID)
            Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
                override fun onInitialized(spotifyPlayer: SpotifyPlayer) {
                    player = spotifyPlayer
                    player?.addConnectionStateCallback(this@MediaPlayerService)
                    player?.addNotificationCallback(this@MediaPlayerService)
                }

                override fun onError(throwable: Throwable) {
                    Log.e(FlipFlopActivity.TAG, "Could not initialize player: " + throwable.message)
                }
            })
        }

        override fun playPause() {
            Log.d(TAG, "Play pause")
            if (hasFetched) {
                val newPlayingState = if (isPlaying) {
                    player?.pause(this@MediaPlayerService) ?: throw MediaPlayerServiceException("Player not initialized")
                    false
                } else {
                    player?.resume(this@MediaPlayerService) ?: throw MediaPlayerServiceException("Player not initialized")
                    true
                }
                updateNotification(newPlayingState)
            }
        }

        override fun swap() {
            Log.d(TAG, "swap")
            if (hasFetched) {
                val tempTrack = swapTrack
                swapTrack = currentTrack
                this@MediaPlayerService.currentTrack = tempTrack


                currentPlaylistID = playlistTracks.keys.first { it != currentPlaylistID }
                val tracks = playlistTracks[currentPlaylistID]
                val nextTrack: Track? = currentTrack ?: tracks?.items?.firstOrNull()
                nextTrack?.let {
                    playTrack(it)
                }
            }
        }

        override fun skipToNext() {
            Log.d(TAG, "skip to next")
            if (hasFetched) {
                playNextTrack()
            }
        }

        override fun skipToPrevious() {
            Log.d(TAG, "skip to previous")
            if (hasFetched) {
                playPrevTrack()
            }
        }

        override fun seekToPosition(position: Int) {
            Log.d(TAG, "seek to position")
            currentTrack?.let {
                player?.seekToPosition(this@MediaPlayerService, position)
                        ?: throw MediaPlayerServiceException("Player not initialized")
            }
        }

        override fun shuffle() {
            Log.d(TAG, "shuffle")
        }

        override fun register(callback: IMediaPlayerCallback) {
            callbackHandlers.add(callback)
        }

        override fun unregister(callback: IMediaPlayerCallback) {
            callbackHandlers.remove(callback)
        }

        override fun isPlaying(): Boolean {
            return player?.playbackState?.isPlaying ?: false
        }

        override fun getCurrentTrack(): Track? {
            return this@MediaPlayerService.currentTrack
        }
    }
    //endregion
}