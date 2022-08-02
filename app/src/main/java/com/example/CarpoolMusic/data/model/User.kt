package com.example.CarpoolMusic.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(@SerializedName("Username") val userName:String  = "",
                @SerializedName("ProfilePic") val profilePic:String = ""):Serializable