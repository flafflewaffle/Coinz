package com.example.s1623165.coinz

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class SendReceiveFragment : Fragment() {

    private var notifications = ArrayList<com.example.s1623165.coinz.Notification>()

    private lateinit var noteMap : MutableMap<String, Any>
    private lateinit var mContext: Context
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var noteReference: DocumentReference

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    //---------------SENDING COIN---------------//

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.mContext = context!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        noteReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Notifications")
        getNotifications()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater?.inflate(R.layout.send_receive_fragment, null)
        recyclerView = root.findViewById(R.id.sendRecyclerView)
        return root
    }

    //---------------SETTER FUNCTIONS---------------//

    //retrieve notifications from the database and add them as notification items to the wallet
    private fun getNotifications() {
        noteReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        noteMap = documentSnapshot.data!!
                        if(noteMap.isEmpty()) {
                            Toast.makeText(mContext,
                                    "No notifications to retrieve!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }
                        setNotifications()
                        buildRecyclerView()
                        Log.d("Receive Fragment", "Notifications retrieved from Firestore with wallet size: ${noteMap.size}")
                    } else {
                        Toast.makeText(mContext,
                                "Notifications does not exist",
                                Toast.LENGTH_SHORT)
                                .show()
                        Log.d("Receive Fragment", "Notifications does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(mContext,
                            "Error accessing notifications",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d("Receive Fragment", e.toString())
                }
    }

    //add new Notification items to the list of notifications
    private fun setNotifications() {
        for (key: String in noteMap.keys) {
            val note = noteMap[key] as String
            notifications.add(com.example.s1623165.coinz.Notification(R.drawable.ic_account_circle_black_24dp, key, note))
        }
    }

    // builds the recycler view
    private fun buildRecyclerView() {
        layoutManager = LinearLayoutManager(mContext)
        adapter = NotificationsAdapter(notifications)
        recyclerView.layoutManager = this.layoutManager
        recyclerView.adapter = this.adapter
    }
}