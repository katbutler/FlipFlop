// IMediaPlayerService.aidl
package com.katbutler.flipflop.services;

import com.katbutler.flipflop.services.IMediaPlayerCallback;

import com.katbutler.flipflop.spotifynet.models.Playlist;
import com.katbutler.flipflop.spotifynet.models.Track;

// Declare any non-default types here with import statements

interface IMediaPlayerService {

    void prepare(String accessToken, in Playlist playlistID1, in Playlist playlistID2);

    void playPause();

    void swap();

    void skipToNext();

    void skipToPrevious();

    void seekToPosition(int position);

    void shuffle();

    void register(IMediaPlayerCallback callback);

    void unregister(IMediaPlayerCallback callback);

    Track getCurrentTrack();

    boolean isPlaying();
}
