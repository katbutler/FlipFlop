package com.katbutler.flipflop.spotifynet.models

import android.os.Parcel
import com.google.gson.annotations.SerializedName
import com.katbutler.flipflop.helpers.KParcelable
import com.katbutler.flipflop.helpers.parcelableCreator


/**
 * Created by kat on 2018-03-25.
 */
data class Track(
    val track: TrackData
): KParcelable {
    constructor(parcel: Parcel) : this(TrackData.CREATOR.createFromParcel(parcel))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(track, flags)
    }


    companion object {
        @JvmField val CREATOR = parcelableCreator(::Track)
    }
}

data class Tracks(
        val href: String,
        val items: List<Track>,
        val limit: Int,
        val offset: Int,
        val total: Int
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.createTypedArrayList(Track.CREATOR),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(href)
        parcel.writeInt(limit)
        parcel.writeInt(offset)
        parcel.writeInt(total)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Tracks)
    }
}

data class TrackData(
        val album: Album,
        val artists: List<Artist>,
        val href: String,
        val id: String,
        val name: String,
        val uri: String,
        @SerializedName("duration_ms") val durationMs: Int
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readParcelable(Album::class.java.classLoader),
            parcel.createTypedArrayList(Artist.CREATOR),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(album, flags)
        parcel.writeTypedList(artists)
        parcel.writeString(href)
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(uri)
        parcel.writeInt(durationMs)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::TrackData)
    }
}

data class Album(
        val href: String,
        val id: String,
        val name: String,
        val uri: String,
        val images: List<SpotifyImage>
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.createTypedArrayList(SpotifyImage.CREATOR))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(href)
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(uri)
        parcel.writeTypedList(images)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Album)
    }
}

data class Artist(
        val name: String,
        val id: String,
        val href: String,
        val uri: String,
        val type: String
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(id)
        parcel.writeString(href)
        parcel.writeString(uri)
        parcel.writeString(type)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Artist)
    }
}