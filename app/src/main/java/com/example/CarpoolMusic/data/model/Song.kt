package com.example.CarpoolMusic.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Song(@SerializedName("Title") val name:String  = "",
                @SerializedName("Artist") val artist:String = "",
                @SerializedName("Cover") val cover:String = "https://www.thebromie.com/CarpoolMusic/ProfilePic.jpg",
                @SerializedName("Uri") val uri: String = ""): Serializable