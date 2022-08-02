package com.example.CarpoolMusic.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.CarpoolMusic.databinding.FragmentJoinBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class JoinFragment : Fragment() {

    private var _binding: FragmentJoinBinding? = null
    val database = Firebase.firestore
    // This property is only valid between onCreateView and
    // onDestroyView.
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
        database.collection("test/Vxbnq6yLIGwp0X8bvwm6/Users").get().addOnSuccessListener { result ->for (document in result) {
            Log.d("Database Test", "${document.id} => ${document.data}")
        } }.addOnFailureListener { exception ->
            Log.w("Database Test", "Error getting documents.", exception)
        }
        val user = hashMapOf(
            "first" to "Alan",
            "middle" to "Mathison",
            "last" to "Turing",
            "born" to 1912
        )

        database.collection("test/Vxbnq6yLIGwp0X8bvwm6/Users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("Database Test", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Database Test", "Error adding document", e)
            }
        database.collection("test/Vxbnq6yLIGwp0X8bvwm6/Users").get().addOnSuccessListener { result ->for (document in result) {
            Log.d("Database Test", "${document.id} => ${document.data}")
        } }.addOnFailureListener { exception ->
            Log.w("Database Test", "Error getting documents.", exception)
        }
        return root
    }

    private fun joinRoom(){
        TODO("Have not implemented")
    }

    private fun createRoom(){
        TODO("Have not implemented")
    }

    private fun updateFriendList(){
        TODO("Have not implemented")
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}