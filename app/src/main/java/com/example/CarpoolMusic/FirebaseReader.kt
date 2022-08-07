package com.example.CarpoolMusic

import androidx.appcompat.app.AppCompatActivity
import com.example.CarpoolMusic.data.model.Song
import com.example.CarpoolMusic.data.model.User
import com.google.firebase.firestore.DocumentSnapshot
import kotlin.coroutines.coroutineContext

class FirebaseReader {
    fun resultsToSongList(result: DocumentSnapshot): ArrayList<Song> {
        val songList = ArrayList<Song>()
        if(result.data?.contains("queue") == true){
            val songs = result.data?.get("queue") as ArrayList<HashMap<String, Any>>
            for(song in songs){
                val songTitle = song["name"] as String
                val songArtist = song["artist"] as String
                val songImage = song["cover"] as String
                val songUri = song["uri"] as String
                songList.add(Song(songTitle, songArtist, songImage, songUri))
            }
        }
        return songList
    }

    fun resultsToProgress(result: DocumentSnapshot): Int{
        var progress = 0
        if(result.data?.contains("progress") == true){
            progress = (result.data?.get("progress") as Long).toInt()
        }
        return progress
    }

    fun resultsToUserList(result: DocumentSnapshot): ArrayList<User> {
        val userList = ArrayList<User>()
        if(result.data?.contains("users") == true){
            val users = result.data?.get("users") as ArrayList<HashMap<String, Any>>
            for (user in users) {
                val userName = user["userName"] as String
                val profilePic = user["profilePic"] as String
                val hasPremium = user["hasPremium"] as Boolean
                val isHost = user["host"] as Boolean
                val user = User(userName, profilePic, hasPremium, isHost)
                userList.add(user)
            }
        }
        return userList
    }
}