package com.example.CarpoolMusic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import coil.load
import org.json.JSONArray


class NotifRecyclerAdapter (private var songList: JSONArray):RecyclerView.Adapter<NotifRecyclerAdapter.ViewHolder>() {
    private val imageList = getImages()
    private val songTitles = getSongTitles()
    private val songArtists = getSongArtist()
    private val songUris = getSongUri()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifRecyclerAdapter.ViewHolder{
        val v = LayoutInflater.from(parent.context).inflate(R.layout.search_card_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int{
        return songList.length()
    }

    override fun onBindViewHolder(holder: NotifRecyclerAdapter.ViewHolder, position: Int){
        holder.searchDetail.text = songArtists[position]
        holder.searchSongTitle.text = songTitles[position]
        holder.searchAddButton.tag = songUris[position]
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
}