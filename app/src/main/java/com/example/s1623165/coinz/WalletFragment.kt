package com.example.s1623165.coinz

import android.app.ProgressDialog.show
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlin.math.roundToInt

class WalletFragment : Fragment() {

    private var wallet = ArrayList<CoinItem>()
    private var currencyImageMap = HashMap<String, Int>()
    private var exchangeRates = HashMap<String, String>()
    private val prefsFile = "MyPrefsFile"


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
        setExchangeRates()
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
                        setWallet()
                        buildRecyclerView()
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

    // retrieve the current exchange rates from shared preferences
    private fun setExchangeRates() {
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)

        exchangeRates.put("QUID",settings.getString("QUID",""))
        exchangeRates.put("DOLR",settings.getString("DOLR","") )
        exchangeRates.put("PENY",settings.getString("PENY",""))
        exchangeRates.put("SHIL",settings.getString("SHIL",""))

    }

    private fun setWallet() {
        for (key: String in coins.keys) {
            val coinJson = coins[key] as String
            val gson = Gson()
            val coin = gson.fromJson(coinJson, Coin::class.java)
            val image = currencyImageMap[coin.currency]!!
            wallet.add(CoinItem(image, coin.currency, coin.value.toString(), coin.id))
        }
    }

    private fun buildRecyclerView() {
        layoutManager = LinearLayoutManager(mContext)
        adapter = WalletAdapter(wallet)

        recyclerView.layoutManager = this.layoutManager
        recyclerView.adapter = this.adapter
        adapter.setOnItemClickListener { position: Int ->
            showDialogueBank(wallet[position])
        }
    }

    //present an alert dialogue when you want to bank a coin
    private fun showDialogueBank(coinItem: CoinItem) {
        Log.d(tag, "dialogue for banking coin")
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val currentAllowance = settings.getInt("allowance",25)

        val builder = AlertDialog.Builder(mContext)
        val exchangeRate = exchangeRates.get(coinItem.title)!!
        val gold = (coinItem.description.toDouble() * exchangeRate.toDouble()).roundToInt()
        builder.setTitle("Would you like to bank this coin?")
        builder.setMessage("This coin is worth $gold in gold.\nYou can bank $currentAllowance more coins today.")
        builder.setPositiveButton("YES") { dialog: DialogInterface?, which: Int ->
            bankCoin(coinItem)
        }
        builder.setNegativeButton("NO",{ dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    //This banks the coin the user has chosen
    private fun bankCoin(coinItem: CoinItem) {
        Log.d(tag, "Converting coin to gold and sending to bank")

        // current coin's value in gold
        val exchangeRate = exchangeRates.get(coinItem.title)!!
        var coinInGold = (coinItem.description.toDouble() * exchangeRate.toDouble()).roundToInt()

        //calculating total gold by adding the current coin's value in gold + current amount of gold in bank
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val currentGold = settings.getInt("Bank", 0)
        val currentAllowance = settings.getInt("allowance",25)

        val totalGold = coinInGold + currentGold

        // gold to store in database
        val gold = HashMap<String, Any>()
        gold.put("Gold", totalGold)
        Log.d(tag, "Total gold collected$totalGold")

        // store gold in bank, update current allowance in shared prefs
        val editor = settings.edit()
        editor.putInt("Bank", totalGold)
        editor.putInt("allowance", (currentAllowance-1))
        editor.apply()

        // store gold in database bank
        db.collection("Users").document(mAuth.uid!!)
                .collection("User Information").document("Bank")
                .set(gold)
                .addOnSuccessListener { _ ->
                    Toast.makeText(mContext,
                            "Coin cashed into bank",
                            Toast.LENGTH_SHORT)
                            .show()
                    //delete coin from database
                    deleteCoinDatabase(coinItem)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(mContext,
                            "Error storing coin, try again later",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d(tag, e.toString())
                }
    }

    private fun deleteCoinDatabase(coinItem: CoinItem) {
        val deleteCoin = HashMap<String,Any>()
        deleteCoin.put(coinItem.id, FieldValue.delete())
        db.collection("Users").document(mAuth.uid!!)
                .collection("User Information").document("Wallet")
                .update(deleteCoin)
                .addOnSuccessListener { _ ->
                    wallet.remove(coinItem)
                    adapter.notifyItemRemoved(wallet.indexOf(coinItem))
                }
    }


}