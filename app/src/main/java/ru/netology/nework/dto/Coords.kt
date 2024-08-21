package ru.netology.nework.dto

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Coords(
    @SerializedName("lat")
    val lat: String?,
    @SerializedName("long")
    val long: String?,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(lat)
        parcel.writeString(long)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Coords> {
        override fun createFromParcel(parcel: Parcel): Coords {
            return Coords(parcel)
        }

        override fun newArray(size: Int): Array<Coords?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "Координаты: ${lat.toString().take(9)}, ${long.toString().take(9)}"
    }
}