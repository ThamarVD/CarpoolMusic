package com.example.CarpoolMusic.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(@SerializedName("userName") val userName:String  = "",
                @SerializedName("profilePic") val profilePic:String = "https://www.thebromie.com/CarpoolMusic/ProfilePic.jpg",
                @SerializedName("hasPremium") val hasPremium:Boolean = false,
                @SerializedName("host") val host: Boolean = false):Serializable