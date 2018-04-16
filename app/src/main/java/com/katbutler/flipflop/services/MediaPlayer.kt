package com.katbutler.flipflop.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.crashlytics.android.Crashlytics
import com.katbutler.flipflop.spotifynet.models.Playlist
import com.katbutler.flipflop.spotifynet.models.Track

class MediaPlayer(val context: Context, onConnected: (MediaPlayer) -> Unit) {

    companion object {
        private const val TAG = "MediaPlayer"
    }

    private var mediaPlayerService: IMediaPlayerService? = null

    private val mediaServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceDisconnected(componentName: ComponentName?) {

            }

            override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
                mediaPlayerService = service?.let { IMediaPlayerService.Stub.asInterface(service) }
                onConnected(this@MediaPlayer)
            }

        }
    }

    fun connect(): Boolean {
        val serviceIntent = Intent(context, MediaPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        return context.bindService(serviceIntent, mediaServiceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Run the function and log any exceptions to Crashlytics
     */
    private fun <T> handleRemoteException(fn: () -> T): T {
        try {
            return fn()
        } catch (e: Exception) {
            Crashlytics.logException(e)
            throw e
        }
    }

    fun disconnect() = handleRemoteException {
        context.unbindService(mediaServiceConnection)
    }

    fun prepare(accessToken: String, playlist1: Playlist, playlist2: Playlist) = handleRemoteException {
        mediaPlayerService?.prepare(accessToken, playlist1, playlist2)
    }

    fun playPause() = handleRemoteException {
        mediaPlayerService?.playPause()
    }

    fun swap() = handleRemoteException {
        mediaPlayerService?.swap()
    }

    fun skipToNext() = handleRemoteException {
        mediaPlayerService?.skipToNext()
    }

    fun skipToPrevious() = handleRemoteException {
        mediaPlayerService?.skipToPrevious()
    }

    fun seekToPosition(position: Int) = handleRemoteException {
        mediaPlayerService?.seekToPosition(position)
    }

    fun shuffle() = handleRemoteException {
        mediaPlayerService?.shuffle()
    }

    fun isPlaying(): Boolean = handleRemoteException {
        mediaPlayerService?.isPlaying == true
    }

    fun registerCallbacks(callback: IMediaPlayerCallback) = handleRemoteException {
        mediaPlayerService?.register(callback)
    }

    fun unregisterCallbacks(callback: IMediaPlayerCallback) = handleRemoteException {
        mediaPlayerService?.unregister(callback)
    }

    fun getCurrentTrack(): Track? = handleRemoteException {
        mediaPlayerService?.currentTrack
                ?: throw IllegalStateException("MediaPlayer has not been initialized")
    }
}