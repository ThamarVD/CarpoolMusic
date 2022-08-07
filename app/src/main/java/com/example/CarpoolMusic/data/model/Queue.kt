package com.example.CarpoolMusic.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Queue(@SerializedName("Song Name") val songName:String = "",
                @SerializedName("Album Cover") val albumCover:String = "",
                @SerializedName("Song Uri") val songUri:String = ""):Serializable
