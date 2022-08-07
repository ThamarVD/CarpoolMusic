package com.example.CarpoolMusic.ui.home

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.CarpoolMusic.FirebaseReader
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.SpotifyTokens
import com.example.CarpoolMusic.data.model.User
import com.example.CarpoolMusic.databinding.FragmentJoinBinding
import com.example.CarpoolMusic.ui.login.LoginActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection


class JoinFragment : Fragment() {

    private var _binding: FragmentJoinBinding? = null
    private val database = Firebase.firestore
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<FriendRecyclerAdapter.ViewHolder>? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val joinViewModel =
            ViewModelProvider(this).get(JoinViewModel::class.java)
        _binding = FragmentJoinBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.createBtn)?.setOnClickListener {
            GlobalScope.launch(Dispatchers.Default){
                createRoom()
            }
        }
        view.findViewById<Button>(R.id.joinBtn)?.setOnClickListener {
            GlobalScope.launch(Dispatchers.Default){
                joinRoom()
            }
        }
    }

    private fun joinRoom(){
        val documents: ArrayList<String> = ArrayList()
        database.collection("rooms").get().addOnSuccessListener { result ->
            for (document in result) {
                documents.add(document.id)
                Log.d("Database Test", document.id)
            }
            val roomID : String = view?.findViewById<EditText>(R.id.roomCode)?.text.toString().uppercase()
            if(documents.contains(roomID.uppercase())){
                userJoinRoom(roomID, false)
            }
            else{
                Toast.makeText(context, "Room does not exist", Toast.LENGTH_LONG).show()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun createRoom(){
        val documents: ArrayList<String> = ArrayList()
        database.collection("rooms").get().addOnSuccessListener { result ->
            for (document in result) {
                documents.add(document.data.toString())
                Log.d("Database Test", "${document.id} => ${document.data}")
            }
            var roomID = randomBase36(7)
            while (documents.contains(roomID)){
                roomID = randomBase36(7)
            }
            userJoinRoom(roomID, true)
        }.addOnFailureListener { exception ->
            Log.w("Database Test", "Error getting documents.", exception)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun userJoinRoom(roomID: String, isHost: Boolean = false) {
        var isHost = isHost
        GlobalScope.launch(Dispatchers.Default) {
            val userData = getUserData()
            withContext(Dispatchers.Default) {
                if (userData.hasPremium || !isHost) {
                    database.collection("rooms").document(roomID).get().addOnSuccessListener { result ->
                        val users = FirebaseReader().resultsToUserList(result)
                        val userName = userData.userName
                        val profilePic = userData.profilePic
                        val hasPremium = userData.hasPremium
                        var isInList = false
                        for (i in 0 until users.size) {
                            if (users[i].userName == userName) {
                                isInList = true
                                break
                            }
                        }
                        if (!isInList) {
                            val user = User(userName, profilePic, hasPremium, isHost)
                            users.add(user)
                            activity?.getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE)?.edit()
                                ?.putString(SpotifyTokens.USERNAME, userName)
                                ?.apply()
                        }
                        else{
                            requireActivity().runOnUiThread {
                                Toast.makeText(activity, "Already in room!", Toast.LENGTH_LONG).show()
                                var host : Boolean = false
                                for (i in 0 until users.size) {
                                    if (users[i].userName == userName) {
                                        host = users[i].host
                                    }
                                    isHost = host
                                }
                                requireActivity().runOnUiThread{
                                    requireActivity().getSharedPreferences(
                                        SpotifyTokens.SHARED_PREFS,
                                        AppCompatActivity.MODE_PRIVATE
                                    ).edit().putBoolean(SpotifyTokens.IS_HOST, host).apply()
                                }
                            }
                        }
                        if(isHost && !isInList){database.collection("rooms").document(roomID).set(mapOf("users" to users))}else{database.collection("rooms").document(roomID).update("users", users)}.addOnSuccessListener { documentReference ->
                            requireActivity().runOnUiThread{
                                requireActivity().getSharedPreferences(
                                    SpotifyTokens.SHARED_PREFS,
                                    AppCompatActivity.MODE_PRIVATE
                                ).edit().putBoolean(SpotifyTokens.IS_HOST, isHost).apply()
                            }
                            hideButtons(roomID)
                        }.addOnFailureListener { e ->
                            Log.w("Database Test", "Error adding document", e)
                            requireActivity().runOnUiThread {
                                Toast.makeText(activity, "There Was a Problem, Please Try again!", Toast.LENGTH_LONG).show()
                            }
                        }

                    }.addOnFailureListener { exception ->
                        Log.w("DatabaseTest", "Error getting documents.", exception)
                    }


                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(activity, "You need Spotify Premium to create a room", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun hideButtons(roomID: String){
        view?.findViewById<Button>(R.id.createBtn)?.visibility = View.GONE
        view?.findViewById<Button>(R.id.joinBtn)?.visibility = View.GONE
        view?.findViewById<EditText>(R.id.roomCode)?.setText(roomID, TextView.BufferType.EDITABLE)
        view?.findViewById<EditText>(R.id.roomCode)?.isEnabled = false
        view?.findViewById<EditText>(R.id.roomCode)?.layoutParams?.let {
            val params = it as ConstraintLayout.LayoutParams
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        //Set SpotifyTokens.ROOM_CODE shared preference to roomID
        this.requireActivity().getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE)
            .edit().putString(SpotifyTokens.ROOM_CODE, roomID).apply()
        updateFriendList(roomID)
    }

    private fun updateFriendList(roomID: String) {
        val docRef = database.collection("rooms").document(roomID)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            val source = if (snapshot != null && snapshot.metadata.hasPendingWrites())
                "Local"
            else
                "Server"

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "$source data: ${snapshot.data}")
                docRef.get().addOnSuccessListener { result ->
                    //Get the "user" data from result and convert it to an arraylist of users
                    val userList = FirebaseReader().resultsToUserList(result)
                    updateUserView(userList)
                }.addOnFailureListener { exception ->
                    Log.w("DatabaseTest", "Error getting documents.", exception)
                }
            } else {
                Log.d(TAG, "$source data: null")
            }
        }
    }

    fun isInRoom(): Boolean = !this.requireActivity().getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null).isNullOrEmpty()

    private fun updateUserView(userList: ArrayList<User>) {
        layoutManager = LinearLayoutManager(requireContext())
        val friendsRecycler = view?.findViewById<RecyclerView>(R.id.friendsRecycler)
        friendsRecycler?.layoutManager = layoutManager
        adapter = FriendRecyclerAdapter(userList)
        friendsRecycler?.adapter = adapter
    }

    //Function that gets the user data from spotify and returns it as a UserData object
    suspend fun getUserData(): User {
        val getUserProfileURL = "https://api.spotify.com/v1/me"
        val url = URL(getUserProfileURL)
        val httpsURLConnection = url.openConnection() as HttpsURLConnection
        httpsURLConnection.requestMethod = "GET"
        httpsURLConnection.setRequestProperty("Authorization", "Bearer ${
            this.requireActivity().getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, "")
        }")
        httpsURLConnection.doInput = true
        httpsURLConnection.doOutput = false
        val user: Deferred<User> = GlobalScope.async(Dispatchers.Default) {
            val responseCode = httpsURLConnection.responseCode
            if(responseCode == HttpsURLConnection.HTTP_OK){
                val response = httpsURLConnection.inputStream.bufferedReader().use { it.readText() }
                withContext(Dispatchers.Main) {
                    if (!response.contains("error")) {
                        val jsonObject = JSONObject(response)
                        val username = jsonObject.getString("display_name")
                        val profilePicture = jsonObject.getString("images")
                        val hasPremium = jsonObject.getString("product") == "premium"
                        val jsonArray = JSONArray(profilePicture)
                        val profilePictureURL: String = if (jsonArray.length() > 0) {
                            jsonArray.getJSONObject(0).getString("url")
                        } else {
                            "https://www.thebromie.com/CarpoolMusic/ProfilePic.jpg"
                        }
                        val user = User(username, profilePictureURL, hasPremium)
                        user
                    } else {
                        val jsonObject = JSONObject(response).getJSONObject("error")
                        val responseCode = jsonObject.getInt("status")
                        if (responseCode != 401) {
                            Toast.makeText(
                                requireContext(),
                                "$responseCode error: Please try again!", Toast.LENGTH_LONG
                            ).show()
                            Log.d("Error", "$responseCode error: Please try again!")
                            User()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Please Restart the App!", Toast.LENGTH_LONG
                            ).show()
                            Log.d("Error", "Please log in again!")
                            User()
                        }
                    }
                }
            }
            else{
                User()
            }
        }
        return user.await()
    }

    //Generate random Base36 string of length n.
    private fun randomBase36(n: Int): String {
        val random = java.util.Random()
        val sb = StringBuilder(n)
        for (i in 0 until n) {
            sb.append(Integer.toString(random.nextInt(36), 36).uppercase(Locale.ROOT))
        }
        return sb.toString()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if(isInRoom()) hideButtons(this.requireActivity().getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null).toString())
    }
}