package com.katbutler.flipflop.spotifynet.models

import android.os.Parcel
import com.google.gson.annotations.SerializedName
import com.katbutler.flipflop.helpers.KParcelable
import com.katbutler.flipflop.helpers.parcelableCreator

/**
 * Created by kat on 2018-03-24.
 */
data class UserProfile(
        val id: String,
        @SerializedName("display_name") val displayName: String,
        val images: List<SpotifyImage>,
        val product: String,
        val type: String,
        val uri: String
): KParcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.createTypedArrayList(SpotifyImage.CREATOR),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(displayName)
        parcel.writeTypedList(images)
        parcel.writeString(product)
        parcel.writeString(type)
        parcel.writeString(uri)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::UserProfile)
    }
}