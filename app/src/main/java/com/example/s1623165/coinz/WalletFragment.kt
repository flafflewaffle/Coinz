package com.example.s1623165.coinz

import android.app.ProgressDialog.show
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlin.collections.Map

class WalletFragment : Fragment() {

    private var wallet = ArrayList<CoinItem>()
    private var currencyImageMap = HashMap<String, Int>()

    private lateinit var coins : MutableMap<String, Any>

    private lateinit var mContext: Context
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var documentReference: DocumentReference

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WalletAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.mContext = context!!

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        documentReference = db.collection("Users").document(mAuth.uid!!)
                .collection("User Information").document("Wallet")
        setCurrencyMap()
        getCoins()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater?.inflate(R.layout.wallet_fragment, null)
        recyclerView = root.findViewById(R.id.recyclerView)
        return root
    }

    // set map of currencies to image resources
    private fun setCurrencyMap() {
        currencyImageMap.put("DOLR", R.drawable.ic_circle_blue_24dp)
        currencyImageMap.put("SHIL", R.drawable.ic_circle_red_24dp)
        currencyImageMap.put("QUID", R.drawable.ic_circle_green_24dp)
        currencyImageMap.put("PENY", R.drawable.ic_circle_yellow_24dp)
    }

    //retrieve coins from the database and add them as coin items to the wallet
    private fun getCoins() {
        documentReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        coins = documentSnapshot.data!!

                        if(coins.isEmpty()) {
                            Toast.makeText(mContext,
                                    "Wallet is empty, collect coins to view them here!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }

                        for (key: String in coins.keys) {
                            val coinJson = coins[key] as String
                            val gson = Gson()
                            val coin = gson.fromJson(coinJson, Coin::class.java)
                            val image = currencyImageMap[coin.currency]!!
                            wallet.add(CoinItem(image, coin.currency, coin.value.toString(), coin.id))
                        }

                        layoutManager = LinearLayoutManager(mContext)
                        adapter = WalletAdapter(wallet)

                        recyclerView.layoutManager = this.layoutManager
                        recyclerView.adapter = this.adapter

                        Log.d("WalletFragment", "Wallet retrieved from Firestore with wallet size: ${wallet.size}")
                    } else {
                        Toast.makeText(mContext,
                                "Wallet does not exist",
                                Toast.LENGTH_SHORT)
                                .show()
                        Log.d("WalletFragment", "Wallet does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(mContext,
                            "Error accessing wallet",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d("WalletFragment", e.toString())
                }
    }
}