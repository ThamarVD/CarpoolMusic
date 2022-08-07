package com.example.CarpoolMusic.ui.queue

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.CarpoolMusic.FirebaseReader
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.SpotifyTokens
import com.example.CarpoolMusic.data.model.Song
import com.example.CarpoolMusic.databinding.FragmentQueueBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.ArrayList

class QueueFragment : Fragment() {

    private var _binding: FragmentQueueBinding? = null
    private val database = Firebase.firestore
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<QueueRecyclerAdapter.ViewHolder>? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateQueue()
    }

    private fun updateQueue() {
        val roomID = this.requireActivity().getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null)
        if(!roomID.isNullOrEmpty()) {
            val docRef = database.collection("rooms").document(roomID)
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                val source = if (snapshot != null && snapshot.metadata.hasPendingWrites())
                    "Local"
                else
                    "Server"

                if (snapshot != null && snapshot.exists()) {
                    Log.d(ContentValues.TAG, "$source data: ${snapshot.data}")
                    docRef.get().addOnSuccessListener { result ->
                        val songList = FirebaseReader().resultsToSongList(result)
                        updateQueueView(songList)
                    }.addOnFailureListener { exception ->
                        Log.w("DatabaseTest", "Error getting documents.", exception)
                    }
                } else {
                    Log.d(ContentValues.TAG, "$source data: null")
                }
            }
        }
    }

    private fun updateQueueView(songList: ArrayList<Song>) {
        try{
            layoutManager = LinearLayoutManager(requireContext())
            val queueRecycler = view?.findViewById<RecyclerView>(R.id.queueRecyclerView)
            queueRecycler?.layoutManager = layoutManager
            adapter = QueueRecyclerAdapter(songList)
            queueRecycler?.adapter = adapter
        }catch (e: Exception){
            Log.e("QueueFragment", e.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}