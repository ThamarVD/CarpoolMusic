package com.example.CarpoolMusic

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.CarpoolMusic.data.model.Song
import com.example.CarpoolMusic.databinding.ActivityMainBinding
import com.example.CarpoolMusic.ui.login.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val database = Firebase.firestore
    lateinit var mainHandler: Handler

    private val updateTextTask = object : Runnable {
        override fun run() {
            changeSong()
            mainHandler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViewById<ImageButton>(R.id.logOutButton).setOnClickListener{
            logout()
        }
        findViewById<ProgressBar>(R.id.progressBar).max = 100

        mainHandler = Handler(Looper.getMainLooper())

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateTextTask)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateTextTask)
    }

    //TODO
    private fun changeSong() {
        val token = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, null)
        val isHost = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getBoolean(SpotifyTokens.IS_HOST, false)
        val roomID = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null)
        if(roomID != null && isHost) {
            val databasePath = database.collection("rooms").document(roomID)
            databasePath.get().addOnSuccessListener { result ->
                if(result.data?.contains("queue") == true){
                    val songs = FirebaseReader().resultsToSongList(result)
                    if(songs.size > 0){
                        val getPlaybackState = "https://api.spotify.com/v1/me/player"
                        val url = URL(getPlaybackState)
                        GlobalScope.launch (Dispatchers.Default) {
                            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection }
                            httpsURLConnection.requestMethod = "GET"
                            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
                            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
                            httpsURLConnection.doInput = true
                            httpsURLConnection.doOutput = false
                            val responseCode = httpsURLConnection.responseCode
                            val responseMessage = httpsURLConnection.responseMessage
                            if(withContext(Dispatchers.Default){responseCode == HttpsURLConnection.HTTP_OK}){
                                val response = httpsURLConnection.inputStream.bufferedReader()
                                    .use { it.readText() }
                                withContext(Dispatchers.Default) {
                                    val responseJson = JSONObject(response)
                                    val isDevicePlaying = responseJson.getJSONObject("device")
                                        .getBoolean("is_active")
                                    if (isDevicePlaying) {
                                        val isSongPlaying =
                                            responseJson.getBoolean("is_playing")
                                        GlobalScope.launch(Dispatchers.Main) {
                                            if (isSongPlaying) {
                                                val progress_ms = responseJson.getInt("progress_ms")
                                                val duration_ms = responseJson.getJSONObject("item").getInt("duration_ms")
                                                val progress = ((progress_ms.toDouble()/duration_ms.toDouble())*100).roundToInt()
                                                findViewById<ProgressBar>(R.id.progressBar).progress = progress
                                                updateSongStatus(progress)
                                                if(duration_ms - progress_ms < 10000){
                                                    Log.d("ResponseError", "Adding Song")
                                                    addSongToQueue(songs[0].uri)
                                                    removeSongFromQueue(songs)
                                                }
                                            } else {
                                                Log.d("ResponseError", "Adding Song")
                                                addSongToQueue(songs[0].uri)
                                                removeSongFromQueue(songs)
                                                playSong()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            applicationContext,
                                            "Please start playing on a device",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else{
                                Log.d("ResponseError", "$responseCode: $responseMessage")
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                Log.d("MainActivity", "Error getting documents: ", it)
            }
        }else if(roomID != null && !isHost){
            setProgressStatus()
        }
    }

    private suspend fun addSongToQueue(songURI: String){
        GlobalScope.launch(Dispatchers.IO){
            var songURL = URLEncoder.encode(songURI, "UTF-8")
            val token = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, null)
            val url = URL("https://api.spotify.com/v1/me/player/queue?uri=$songURL")
            val httpsURLConnection =
                withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "POST"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true

            val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
            outputStreamWriter.flush()

            val responseCode = httpsURLConnection.responseCode
            if (!(responseCode == HttpsURLConnection.HTTP_NO_CONTENT || responseCode == HttpsURLConnection.HTTP_OK)) {
                Log.e("HTTPURLCONNECTION_ERROR", responseCode.toString())
            }else{
                Log.d("QueueStatus", "Song added to queue")
            }
        }
    }

    private fun removeSongFromQueue(songs: ArrayList<Song>){
        val roomID = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null)
        val databasePath = roomID?.let { database.collection("rooms").document(it) }
        songs.removeAt(0)
        databasePath?.update("queue", songs)?.addOnFailureListener { Log.d("FirestoreError", "An Error has occurred") }
    }

    private fun updateSongStatus(progress: Int){
        val roomID = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null)
        val databasePath = roomID?.let { database.collection("rooms").document(it) }
        databasePath?.update("progress", progress)?.addOnFailureListener {
            databasePath.set(mapOf("progress" to progress))
                .addOnFailureListener {Log.d("FirestoreError", "An Error has occurred") }
        }
    }

    private fun setProgressStatus(){
        val roomID = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null)
        val databasePath = roomID?.let { database.collection("rooms").document(it) }
        databasePath?.get()?.addOnSuccessListener { results -> findViewById<ProgressBar>(R.id.progressBar).progress = FirebaseReader().resultsToProgress(results) }
    }

    private suspend fun playSong(){
        GlobalScope.launch(Dispatchers.IO){
            val token = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, null)
            val url = URL("https://api.spotify.com/v1/me/player/play")
            val httpsURLConnection =
                withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "PUT"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.setRequestProperty("Content-Type", "application/json")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true

            val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
            outputStreamWriter.flush()

            val responseCode = httpsURLConnection.responseCode
            if (!(responseCode == HttpsURLConnection.HTTP_NO_CONTENT || responseCode == HttpsURLConnection.HTTP_OK)) {
                Log.e("HTTPURLCONNECTION_ERROR", responseCode.toString())
            }else{
                Log.d("PlayerStatus", "Songs playing")
            }
        }
    }

    fun logout() {
        if(!getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null).isNullOrEmpty()){
            val roomID = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ROOM_CODE, null)
            if (roomID != null) {
                database.collection("rooms").document(roomID).get().addOnSuccessListener { results ->
                    val users = FirebaseReader().resultsToUserList(results)
                    val currUser = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.USERNAME, null)
                    for (user in users) {
                        //TODO: Error on this line
                        if(currUser == user.userName){
                            users.remove(user)
                        }
                    }
                    database.collection("rooms").document(roomID).update("users", users).addOnFailureListener { e ->
                        Toast.makeText(this, "There was a problem leaving the room", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { Toast.makeText(this, "There was a problem leaving the room", Toast.LENGTH_LONG).show() }
            }
            getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit().putString(SpotifyTokens.ROOM_CODE, null).apply()
        }
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit().putString(SpotifyTokens.ACCESS_TOKEN, null).apply()
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit().putString(SpotifyTokens.ACCESS_EXPIRE, null).apply()
        val switchLoginIntent = Intent(this, LoginActivity::class.java).apply {  }
        startActivity(switchLoginIntent)
        finish()
    }

    override fun onBackPressed() {}
}