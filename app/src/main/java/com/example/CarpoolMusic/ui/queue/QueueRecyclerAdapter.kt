package com.example.CarpoolMusic.ui.queue

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.data.model.Song
import java.util.ArrayList

class QueueRecyclerAdapter(private val songList: ArrayList<Song>) : RecyclerView.Adapter<QueueRecyclerAdapter.ViewHolder>() {
    private val titleList = getSongs()
    private val artistList = getArtists()
    private val coverList = getCovers()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueRecyclerAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.queue_card_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int{
        return titleList.size
    }

    override fun onBindViewHolder(holder: QueueRecyclerAdapter.ViewHolder, position: Int){
        holder.queueSongTitle.text = titleList[position]
        holder.queueDetail.text = artistList[position]
        holder.queueCover.load(coverList[position].toUri().buildUpon().scheme("https").build())
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var queueCover: ImageView
        var queueSongTitle: TextView
        var queueDetail: TextView
        init{
            queueCover = itemView.findViewById(R.id.queue_album_cover)
            queueSongTitle = itemView.findViewById(R.id.queue_song_title)
            queueDetail = itemView.findViewById(R.id.queue_song_desc)
        }
    }

    private fun getSongs(): ArrayList<String> {
        val songs = ArrayList<String>()
        for(song in songList){
            songs.add(song.name)
        }
        return songs
    }

    private fun getArtists(): ArrayList<String> {
        val artists = ArrayList<String>()
        for(song in songList){
            artists.add(song.artist)
        }
        return artists
    }

    private fun getCovers(): ArrayList<String> {
        val covers = ArrayList<String>()
        for(song in songList){
            covers.add(song.cover)
        }
        return covers
    }
}