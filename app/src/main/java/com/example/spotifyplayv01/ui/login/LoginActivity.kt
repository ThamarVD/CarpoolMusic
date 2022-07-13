package com.example.spotifyplayv01.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.spotifyplayv01.MainActivity
import com.example.spotifyplayv01.R
import com.example.spotifyplayv01.SpotifyConstants
import com.example.spotifyplayv01.SpotifyTokens
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).edit().putString(SpotifyTokens.ACCESS_TOKEN, "").apply()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.spotify_login_btn)
        val switchMainIntent = Intent(this, MainActivity::class.java).apply {  }
        loginBtn.setOnClickListener {
            if (!hasToken())
                loginAuth()
            if (hasToken())
                startActivity(switchMainIntent)
        }
        loginBtn.visibility = Button.VISIBLE
    }

    private fun hasToken(): Boolean{
        return !getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE).getString(SpotifyTokens.ACCESS_TOKEN, "").equals("");
    }

    private fun loginAuth() {
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

    private fun logout(){

        val builder =
            AuthorizationRequest.Builder(SpotifyConstants.CLIENT_ID, AuthorizationResponse.Type.TOKEN, SpotifyConstants.REDIRECT_URI)
        builder.setScopes(arrayOf("streaming"))
        builder.setShowDialog(true)
        val request = builder.build()
        AuthorizationClient.openLoginInBrowser(this, request);
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri: Uri? = intent!!.data
        if (uri != null) {
            val response = AuthorizationResponse.fromUri(uri)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    saveToken(response)
                }
                AuthorizationResponse.Type.ERROR -> {Log.e("Error", response.error)}
                else -> {}
            }
        }
    }

    private fun saveToken(response: AuthorizationResponse) {
        val sharedPreferences = getSharedPreferences(SpotifyTokens.SHARED_PREFS, MODE_PRIVATE)
        val sharedEditor = sharedPreferences.edit()
        sharedEditor.putString(SpotifyTokens.ACCESS_TOKEN, response.accessToken)
        sharedEditor.putInt(SpotifyTokens.ACCESS_EXPIRE, response.expiresIn)
        sharedEditor.apply()
    }

    private fun fetchSpotifyUserProfile(token: String?) {
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
    }
}
