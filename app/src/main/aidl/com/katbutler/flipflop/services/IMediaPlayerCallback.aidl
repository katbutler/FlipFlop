// IMediaPlayerCallback.aidl
package com.katbutler.flipflop.services;

import com.katbutler.flipflop.spotifynet.models.Track;

// Declare any non-default types here with import statements

interface IMediaPlayerCallback {

    void onLoggedOut();

    void onLoginFailure();

    void onTrackChanged(in Track track);

    void updateProgress(int progress, int totalProgress);

    void onPlayerEvent(int event);
}
