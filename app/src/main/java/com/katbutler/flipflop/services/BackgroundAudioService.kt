package com.katbutler.flipflop.services


import android.app.*
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.content.BroadcastReceiver
import android.content.Context
import android.media.AudioAttributes
import android.support.v4.media.session.MediaSessionCompat
import android.os.Bundle
import android.support.v4.media.session.MediaButtonReceiver
import android.os.PowerManager
import android.content.ComponentName
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.os.Build
import android.support.v4.media.app.NotificationCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.text.TextUtils
import com.katbutler.flipflop.R
import com.katbutler.flipflop.helpers.MediaStyleHelper


class BackgroundAudioService : MediaBrowserServiceCompat(), MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val TAG = "BackgroundAudioService"
        const val MEDIA_PLAYBACK_CHANNEL_ID = "com.katbutler.flipflop.media_playback"
        const val MEDIA_PLAYBACK_NOTIFICATION_ID = 0xEEEE
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

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            Log.d(TAG, "MediaSession - onPlay")

            if (!successfullyRetrievedAudioFocus()) {
                Log.d("BackgroundAudioService", "Could not retrieve audio focus")
                return
            }

            mediaSessionCompat.isActive = true
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)

            mediaPlayer.start()
        }

        override fun onPause() {
            Log.d(TAG, "MediaSession - onPause")
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Log.d(TAG, "MediaSession - onPlayFromMediaId")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()

        showPlayingNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCompletion(p0: MediaPlayer?) {
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

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if (TextUtils.equals(clientPackageName, packageName))
            return BrowserRoot(getString(R.string.app_name), null)

        return null
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
                .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, "Test")
                .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, "katbut")
                .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Title")
                .build()
        mediaSessionCompat.setMetadata(metadata)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mediaSessionCompat.setMediaButtonReceiver(pendingIntent)

        sessionToken = mediaSessionCompat.sessionToken
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

    private fun showPlayingNotification() {
        Log.d(TAG, "showPlayingNotification")
        val notificationBuilder = MediaStyleHelper.from(this@BackgroundAudioService, mediaSessionCompat)
                .addAction(android.support.v4.app.NotificationCompat.Action(
                        R.drawable.ic_pause_black_24dp,
                        "Pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                .setStyle(NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSessionCompat.sessionToken))
                .setSmallIcon(R.drawable.filpflop_cutout_svg)


        val notification = if (Build.VERSION.SDK_INT >= 27) {
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


        NotificationManagerCompat.from(this@BackgroundAudioService).notify(MEDIA_PLAYBACK_NOTIFICATION_ID, notification)
    }
}