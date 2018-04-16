package com.katbutler.flipflop.spotifynet.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.katbutler.flipflop.helpers.KParcelable
import com.katbutler.flipflop.helpers.parcelableCreator

/**
 * Created by kat on 2018-03-24.
 */
data class Playlist(
        //From json model
        val id: String,
        val images: List<SpotifyImage>,
        val name: String,
        val tracks: PlaylistTracks,
        val owner: PlaylistProfile,
        val type: String,
        val uri: String,

        //Added states
        var selected: Boolean = false
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.createTypedArrayList(SpotifyImage.CREATOR),
            parcel.readString(),
            parcel.readParcelable(PlaylistTracks::class.java.classLoader),
            parcel.readParcelable(PlaylistProfile::class.java.classLoader),
            parcel.readString(),
            parcel.readString(),
            parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeTypedList(images)
        parcel.writeString(name)
        parcel.writeParcelable(tracks, flags)
        parcel.writeParcelable(owner, flags)
        parcel.writeString(type)
        parcel.writeString(uri)
        parcel.writeByte(if (selected) 1 else 0)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Playlist)
    }
}

data class Playlists(
        val href: String,
        val items: List<Playlist>,
        val limit: Int,
        val offset: Int,
        val total: Int
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.createTypedArrayList(Playlist.CREATOR),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(href)
        parcel.writeTypedList(items)
        parcel.writeInt(limit)
        parcel.writeInt(offset)
        parcel.writeInt(total)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Playlists)
    }
}

data class SpotifyImage (
        val height: Int,
        val width: Int,
        val url: String
) : KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(height)
        parcel.writeInt(width)
        parcel.writeString(url)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::SpotifyImage)
    }
}

data class PlaylistTracks(
        val total: Int,
        val href: String
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(total)
        parcel.writeString(href)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::PlaylistTracks)
    }
}

data class PlaylistProfile(
        val id: String,
        @SerializedName("display_name") val displayName: String,
        val type: String,
        val uri: String
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(displayName)
        parcel.writeString(type)
        parcel.writeString(uri)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::PlaylistProfile)
    }
}
