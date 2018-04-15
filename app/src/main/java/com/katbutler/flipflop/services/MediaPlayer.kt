package com.katbutler.flipflop.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder

class MediaPlayer(val context: Context, onConnected: (MediaPlayer) -> Unit) {
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

    fun disconnect() {
        context.unbindService(mediaServiceConnection)
    }

    fun prepare(accessToken: String, playlistID1: String, playlistID2: String) {
        mediaPlayerService?.prepare(accessToken, playlistID1, playlistID2)
    }

    fun playPause() {
        mediaPlayerService?.playPause()
    }

    fun swap() {
        mediaPlayerService?.swap()
    }

    fun skipToNext() {
        mediaPlayerService?.skipToNext()
    }

    fun skipToPrevious() {
        mediaPlayerService?.skipToPrevious()
    }

    fun seekToPosition(position: Int) {
        mediaPlayerService?.seekToPosition(position)
    }

    fun shuffle() {
        mediaPlayerService?.shuffle()
    }

    fun isPlaying() = mediaPlayerService?.isPlaying ?: false

    fun registerCallbacks(callback: IMediaPlayerCallback) {
        mediaPlayerService?.register(callback)
    }

    fun unregisterCallbacks(callback: IMediaPlayerCallback) {
        mediaPlayerService?.unregister(callback)
    }
}