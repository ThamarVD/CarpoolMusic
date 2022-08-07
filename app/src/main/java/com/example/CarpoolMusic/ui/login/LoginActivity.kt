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
import com.spotify.sdk.android.auth.AccountsQueryParameters.CLIENT_ID
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE
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
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit().putString(SpotifyTokens.ROOM_CODE, null).apply()
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
                            switchToMain()
                        }
                    }
                }
            }
            else {
                switchToMain()
            }
        }
    }

    //TODO: Remove function from all places where it is used
    private fun switchToMain() {
        val switchMainIntent = Intent(this, MainActivity::class.java).apply {  }
        startActivity(switchMainIntent)
        finish()
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

        return gScope.await()
    }

    /*ToDo
        If action fails, do not continue to login.
     */


    private fun spotifyLoginAuth() {
        val builder =
            AuthorizationRequest.Builder(
                SpotifyConstants.CLIENT_ID,
                AuthorizationResponse.Type.TOKEN,
                SpotifyConstants.REDIRECT_URI
            )

        builder.setScopes(arrayOf("user-read-private user-read-playback-state user-modify-playback-state"))
        val request = builder.build()

        AuthorizationClient.openLoginInBrowser(this, request)
    }

    fun spotifyLogout(){
        val builder =
            AuthorizationRequest.Builder(SpotifyConstants.CLIENT_ID, AuthorizationResponse.Type.TOKEN, SpotifyConstants.REDIRECT_URI)
        builder.setScopes(arrayOf("streaming"))
        builder.setShowDialog(true)
        val request = builder.build()
        AuthorizationClient.openLoginInBrowser(this, request)
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit()
            .putString(SpotifyTokens.ACCESS_TOKEN, null).apply()
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit()
            .putString(SpotifyTokens.ACCESS_EXPIRE, null).apply()
        val switchLoginIntent = Intent(this, LoginActivity::class.java).apply {  }
        startActivity(switchLoginIntent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri: Uri? = intent!!.data
        if (uri != null) {
            val response = AuthorizationResponse.fromUri(uri)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    saveSpotifyToken(response)
                    switchToMain()
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

    override fun onBackPressed() {
    }
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