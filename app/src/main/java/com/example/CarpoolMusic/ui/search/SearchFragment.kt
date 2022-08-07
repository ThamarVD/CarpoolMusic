package com.example.CarpoolMusic.ui.search

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.CarpoolMusic.R
import com.example.CarpoolMusic.SpotifyTokens
import com.example.CarpoolMusic.databinding.FragmentSearchBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<SearchRecyclerAdapter.ViewHolder>? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(SearchViewModel::class.java)

        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        var notifFragment = view.findViewById<ConstraintLayout>(R.id.notifFragment)

        //Searches for object when user presses enter
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchItems()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
    }

    private fun addSongToQueue(){
        TODO("Not implemented yet")
    }

    private fun searchItems() {
        val token =
            this.activity?.getSharedPreferences(SpotifyTokens.SHARED_PREFS, AppCompatActivity.MODE_PRIVATE)?.getString(SpotifyTokens.ACCESS_TOKEN, "")
        val encodedQuery = URLEncoder.encode(view?.findViewById<SearchView>(R.id.searchView)?.query.toString(), "UTF-8")
        this.hideKeyboard()
        if(encodedQuery.isNotEmpty()){
            val searchURL = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track"
            GlobalScope.launch(Dispatchers.Default) {
                val url = URL(searchURL)
                val httpsURLConnection =
                    withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
                httpsURLConnection.requestMethod = "GET"
                httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = false

                try {
                    val response =
                        httpsURLConnection.inputStream.bufferedReader().use { it.readText() }
                    withContext(Dispatchers.Main) {
                        //Log.d("Input Stream", response)
                        if (!response.contains("error")) {
                            //Log.d("Input Stream", response)
                            layoutManager = LinearLayoutManager(requireContext())
                            val notifRecyclerView =
                                view?.findViewById<RecyclerView>(R.id.searchRecycler)
                            notifRecyclerView?.layoutManager = layoutManager
                            adapter = SearchRecyclerAdapter(
                                JSONObject(response).getJSONObject("tracks").getJSONArray("items")
                            )
                            notifRecyclerView?.adapter = adapter
                        } else {
                            Toast.makeText(requireContext(), "Error " + JSONObject(response).getJSONObject("error").getString("status"), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("Test", "Failed $e")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}