package com.strikelines.app.domain.models

import android.os.Parcel
import android.os.Parcelable

data class Chart(
    val description: String,
    val downloadurl: String,
    val imageurl: String,
    val latitude: String,
    val longitude: String,
    val name: String,
    val price: String,
    val region: String,
    val weburl: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(description)
        parcel.writeString(downloadurl)
        parcel.writeString(imageurl)
        parcel.writeString(latitude)
        parcel.writeString(longitude)
        parcel.writeString(name)
        parcel.writeString(price)
        parcel.writeString(region)
        parcel.writeString(weburl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Chart> {
        override fun createFromParcel(parcel: Parcel): Chart {
            return Chart(parcel)
        }

        override fun newArray(size: Int): Array<Chart?> {
            return arrayOfNulls(size)
        }
    }
}