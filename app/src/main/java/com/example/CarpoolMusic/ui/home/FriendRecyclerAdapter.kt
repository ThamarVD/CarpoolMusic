package com.example.CarpoolMusic.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.data.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FriendRecyclerAdapter(private val result: ArrayList<User>) :RecyclerView.Adapter<FriendRecyclerAdapter.ViewHolder>(){
    private val profilePicList = getProfilePics()
    private val usernameList = getUsernames()
    val database = Firebase.firestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder{
        val v = LayoutInflater.from(parent.context).inflate(R.layout.friend_card_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int{
        return profilePicList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        holder.username.text = usernameList[position]
        holder.profilePic.load(profilePicList[position].toUri().buildUpon().scheme("https").build())
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var profilePic: ImageView
        var username: TextView
        init{
            profilePic = itemView.findViewById(R.id.friend_profile_picture)
            username = itemView.findViewById(R.id.friend_Username)
        }
    }

    private fun getProfilePics(): ArrayList<String> {
        val thisProfilePicList = ArrayList<String>()
        for(user in result){
            thisProfilePicList.add(user.profilePic)
        }
        return  thisProfilePicList
    }

    private fun getUsernames(): ArrayList<String> {
        val thisUsernameList = ArrayList<String>()
        for(user in result){
            thisUsernameList.add(user.userName + if(user.host)" (Host)" else "")
        }
        return thisUsernameList
    }
}