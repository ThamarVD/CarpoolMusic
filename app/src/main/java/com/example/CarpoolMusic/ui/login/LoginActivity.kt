package com.example.CarpoolMusic.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.CarpoolMusic.MainActivity
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.SpotifyConstants
import com.example.CarpoolMusic.SpotifyTokens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.spotify_login_btn)
        loginBtn.visibility = Button.VISIBLE
        GlobalScope.launch(Dispatchers.Main) {
            val tokenStatus : Deferred<Boolean> = GlobalScope.async(Dispatchers.Default) {
                hasSpotifyToken()
            }
            if(!tokenStatus.await()) {
                loginBtn.setOnClickListener {
                    GlobalScope.launch(Dispatchers.Main) {
                        val tokenStatus : Deferred<Boolean> = GlobalScope.async(Dispatchers.Default) {
                            hasSpotifyToken()
                        }
                        if(!tokenStatus.await()) {
                            getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit()
                                .putString(SpotifyTokens.ACCESS_TOKEN, null).apply()
                            getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit()
                                .putString(SpotifyTokens.ACCESS_EXPIRE, null).apply()
                            spotifyLoginAuth()
                        }
                        else {
                            firebaseLoginAuth()
                        }
                    }
                }
            }
            else {
                firebaseLoginAuth()
            }
        }
    }

    private fun firebaseLoginAuth() {
        //Anonymous Auth
        val switchMainIntent = Intent(this, MainActivity::class.java).apply {  }
        firebaseAuth = Firebase.auth
        firebaseAuth.signInAnonymously()
            .addOnSuccessListener { startActivity(switchMainIntent) }
            .addOnFailureListener { Log.d("Firebase Failure", "anonymousAuth: $it")
                Toast.makeText(applicationContext, "Problem Connecting to Firebase", Toast.LENGTH_LONG).show()}
    }

    private suspend fun hasSpotifyToken(): Boolean{
        val token = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, "")
        Log.d("Status", "Please Wait...")
        if (token == "") {
            Log.d("Status", "No Access Token found")
            return false
        }
        Log.d("Status", "Token Found: Checking if token is active")

        val gScope = GlobalScope.async(Dispatchers.Default) {
            val getUserProfileURL = "https://api.spotify.com/v1/me"
            val url = URL(getUserProfileURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false

            try{
                val response = httpsURLConnection.errorStream.bufferedReader().use { it.readText() }
                withContext(Dispatchers.Main) {
                    val jsonObject = JSONObject(JSONObject(response).getString("error"))
                    val responseCode = jsonObject.getInt("status")
                    Toast.makeText(applicationContext,
                        "$responseCode error: Please try again!", Toast.LENGTH_LONG).show()
                    responseCode <= 400
                }
            }catch (e: java.lang.NullPointerException){
                true
            }
        }

        return gScope.await();
    }

    private fun spotifyLoginAuth() {
        val builder =
            AuthorizationRequest.Builder(
                SpotifyConstants.CLIENT_ID,
                AuthorizationResponse.Type.TOKEN,
                SpotifyConstants.REDIRECT_URI
            )

        builder.setScopes(arrayOf("user-read-private user-modify-playback-state"))
        val request = builder.build()

        AuthorizationClient.openLoginInBrowser(this, request)
    }

    fun spotifyLogout(){
        val builder =
            AuthorizationRequest.Builder(SpotifyConstants.CLIENT_ID, AuthorizationResponse.Type.TOKEN, SpotifyConstants.REDIRECT_URI)
        builder.setScopes(arrayOf("streaming"))
        builder.setShowDialog(true)
        val request = builder.build()
        AuthorizationClient.openLoginInBrowser(this, request);
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit()
            .putString(SpotifyTokens.ACCESS_TOKEN, null).apply()
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit()
            .putString(SpotifyTokens.ACCESS_EXPIRE, null).apply()
        val switchLoginIntent = Intent(this, LoginActivity::class.java).apply {  }
        startActivity(switchLoginIntent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri: Uri? = intent!!.data
        if (uri != null) {
            val response = AuthorizationResponse.fromUri(uri)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    saveSpotifyToken(response)
                    firebaseLoginAuth()
                }
                AuthorizationResponse.Type.ERROR -> {Log.e("Error", response.error)}
                else -> {}
            }
        }
    }

    private fun saveSpotifyToken(response: AuthorizationResponse) {
        val sharedPreferences = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE)
        val sharedEditor = sharedPreferences.edit()
        sharedEditor.putString(SpotifyTokens.ACCESS_TOKEN, response.accessToken)
        sharedEditor.putInt(SpotifyTokens.ACCESS_EXPIRE, response.expiresIn)
        sharedEditor.apply()
    }

//    private fun firebaseLoginAuth(){
//        //Anonymous Auth
//        firebaseAuth.signInAnonymously()
//            .addOnSuccessListener {  }
//            .addOnFailureListener { Log.d("Firebase Failure", "anonymousAuth: $it") }
//    }

    /*private fun pingSpotify(){
        val token = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, "")
        Log.d("Status: ", "Please Wait...")
        if (token == "") {
            Log.i("Status: ", "Something went wrong - No Access Token found")
            return
        }
        val getUserProfileURL = "https://api.spotify.com/v1/me"
        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getUserProfileURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false
            val response = httpsURLConnection.errorStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8

            withContext(Dispatchers.Main){
                val jsonObject = JSONObject(response)
                val responseCode = jsonObject.getString("status")
            }
            Log.v("Error type", response)
        }
    }*/

/*    private fun fetchSpotifyUserProfile(token: String?) {
        Log.d("Status: ", "Please Wait...")
        if (token == null) {
            Log.i("Status: ", "Something went wrong - No Access Token found")
            return
        }
        val getUserProfileURL = "https://api.spotify.com/v1/me"
        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(getUserProfileURL)
            val httpsURLConnection = withContext(Dispatchers.IO) {url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false
            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonObject = JSONObject(response)
                // Spotify Id
                val spotifyId = jsonObject.getString("id")
                Log.d("Spotify Id :", spotifyId)
                // Spotify Display Name
                val spotifyDisplayName = jsonObject.getString("display_name")
                Log.d("Spotify Display Name :", spotifyDisplayName)
                // Spotify Email
                val spotifyEmail = jsonObject.getString("email")
                Log.d("Spotify Email :", spotifyEmail)
                Log.d("Spotify AccessToken :", token)
            }
        }
    }*/
}

/*ToDo
    SPOTIFY RESPONSE STATUS CODES
    200 - OK
    201 - Created
    202 - Accepted
    204 - No Content
    304 - Not Modified
    400 - Bad Request
    401 - Unauthorized
    403 - Forbidden
    404 - Not Found
    429 - Too Many Requests
    500 - Internal Server Error
    502 - Bad Gateway
    503 - Service Unavailable
*/