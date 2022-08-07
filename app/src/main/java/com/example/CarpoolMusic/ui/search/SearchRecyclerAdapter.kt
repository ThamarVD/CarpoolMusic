package com.example.CarpoolMusic.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.SpotifyTokens
import com.example.CarpoolMusic.data.model.Song
import com.example.CarpoolMusic.data.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import com.example.CarpoolMusic.FirebaseReader


class SearchRecyclerAdapter (private var songList: JSONArray):RecyclerView.Adapter<SearchRecyclerAdapter.ViewHolder>() {
    private val imageList = getImages()
    private val songTitles = getSongTitles()
    private val songArtists = getSongArtist()
    private val songUris = getSongUri()
    private val database = Firebase.firestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.search_card_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int{
        return songList.length()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        holder.searchDetail.text = songArtists[position]
        holder.searchSongTitle.text = songTitles[position]
        holder.searchAddButton.setOnClickListener {addSongToQueue(holder, Song(songTitles[position], songArtists[position], imageList[position], songUris[position]))}
        holder.searchImage.load(imageList[position].toUri().buildUpon().scheme("https").build())
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var searchImage: ImageView
        var searchSongTitle: TextView
        var searchDetail: TextView
        var searchAddButton: Button
        init{
            searchImage = itemView.findViewById(R.id.search_album_cover)
            searchSongTitle = itemView.findViewById(R.id.search_song_title)
            searchDetail = itemView.findViewById(R.id.search_song_desc)
            searchAddButton = itemView.findViewById(R.id.search_add_btn)
        }
    }

    private fun getImages(): ArrayList<String> {
        val tempImageList : ArrayList<String> = ArrayList<String>()
        for(i in 0 until songList.length()){
            tempImageList.add(songList.getJSONObject(i).getJSONObject("album").getJSONArray("images").getJSONObject(1).getString("url"))
        }
        return tempImageList
    }

    private fun getSongTitles(): ArrayList<String> {
        val tempSongTitleList : ArrayList<String> = ArrayList<String>()
        for(i in 0 until songList.length()){
            tempSongTitleList.add(songList.getJSONObject(i).getString("name"))
        }
        return tempSongTitleList
    }

    private fun getSongArtist(): ArrayList<String> {
        val tempSongArtistsList : ArrayList<String> = ArrayList<String>()
        for(i in 0 until songList.length()){
            var artists : String = ""
            for(j in 0 until songList.getJSONObject(i).getJSONArray("artists").length()){
                artists += songList.getJSONObject(i).getJSONArray("artists").getJSONObject(j).getString("name")
                if (j < songList.getJSONObject(i).getJSONArray("artists").length() - 1)
                    artists += ", "
            }
            tempSongArtistsList.add(artists)
        }
        return tempSongArtistsList
    }

    private fun getSongUri(): ArrayList<String> {
        val tempSongUriList : ArrayList<String> = ArrayList<String>()
        for(i in 0 until songList.length()){
            tempSongUriList.add(songList.getJSONObject(i).getString("uri"))
        }
        return tempSongUriList
    }

    private fun addSongToQueue(holder: ViewHolder, song: Song){
        val context = holder.searchAddButton.context
        val roomID = context.getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null).toString()
        if(!(roomID == "null" || roomID.isNullOrEmpty())){
            database.collection("rooms").document(roomID).get().addOnSuccessListener{ result ->
                val songs = FirebaseReader().resultsToSongList(result)
                songs.add(song)
                database.collection("rooms").document(roomID).update("queue", songs).addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding song to queue", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Please Try Again", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            Toast.makeText(context, "Please join a room!", Toast.LENGTH_LONG).show()
        }
    }


}