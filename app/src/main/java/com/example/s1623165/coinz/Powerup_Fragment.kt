package com.example.s1623165.coinz

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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import kotlin.math.roundToInt

class Powerup_Fragment : Fragment() {

    private var powerups = ArrayList<PowerItem>()
    private val prefsFile = "MyPrefsFile"
    private var exchangeRates = ArrayList<String>()
    private var currencies = ArrayList<String>()

    private lateinit var mContext: Context
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var walletReference: DocumentReference
    private lateinit var bankReference: DocumentReference

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PowerupAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager

    //---------------INITIALISATION---------------//

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.mContext = context!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        walletReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Wallet")
        bankReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Bank")
        setCurrencies()
        setPowerups()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater?.inflate(R.layout.wallet_fragment, null)
        recyclerView = root.findViewById(R.id.recyclerView)
        buildRecyclerView()
        return root
    }

    private fun setPowerups() {
        powerups.add(PowerItem(R.drawable.ic_stars_blue_24dp, "Randomise Exchange Rates!", 250))
        powerups.add(PowerItem(R.drawable.ic_stars_green_24dp, "Randomise Currencies!", 500))
        powerups.add(PowerItem(R.drawable.ic_stars_red_24dp, "Double Coins!", 1000))

    }

    // builds the recycler view and sets the on click listener
    private fun buildRecyclerView() {
        // initialise recycler view and adapter
        layoutManager = LinearLayoutManager(mContext)
        adapter = PowerupAdapter(powerups)
        recyclerView.layoutManager = this.layoutManager
        recyclerView.adapter = this.adapter
        recyclerView.setHasFixedSize(true)

        //set on click listeners for activating a power up
        adapter.setOnItemClickListener { position: Int ->
            showDialoguePowerup(powerups[position])
        }
    }

    // present an alert dialogue for the relevant powerup
    private fun showDialoguePowerup(powerItem: PowerItem) {
        Log.d(tag, "dialogue for banking coin")
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle(powerItem.title)
        when(powerItem.title) {
            "Randomise Currencies!" -> {
                builder.setMessage("This randomises the currencies for all the coins on the map. It costs ${powerItem.price}. Would you like to activate?")
                builder.setPositiveButton("YES") { dialog: DialogInterface?, which: Int ->
                    if(check(powerItem)) { randomiseCurrencies() }
                }
                builder.setNegativeButton("NO") { dialog: DialogInterface?, which: Int -> }
                builder.show()
            }
            "Randomise Exchange Rates!" ->{
                builder.setMessage("This randomises the exchange rates for all the currencies. It costs ${powerItem.price}. Would you like to activate?")
                builder.setPositiveButton("YES") { dialog: DialogInterface?, which: Int ->
                    if(check(powerItem)) { randomiseExchangeRates() }
                }
                builder.setNegativeButton("NO") { dialog: DialogInterface?, which: Int -> }
                builder.show()
            }
            "Double Coins!" -> {
                builder.setMessage("This doubles the values of all coins on the map. It costs ${powerItem.price}. Would you like to activate?")
                builder.setPositiveButton("YES") { dialog: DialogInterface?, which: Int ->
                    if(check(powerItem)) { doubleCoins() }
                }
                builder.setNegativeButton("NO") { dialog: DialogInterface?, which: Int -> }
                builder.show()
            }
            else -> {
                Log.d("Powerup Fragment", "Enter valid String for powerup item")
            }
        }
    }

    private fun setCurrencies() {
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        exchangeRates.add(settings.getString("QUID",""))
        exchangeRates.add(settings.getString("DOLR",""))
        exchangeRates.add(settings.getString("SHIL",""))
        exchangeRates.add(settings.getString("PENY",""))
        currencies.add("QUID")
        currencies.add("DOLR")
        currencies.add("SHIL")
        currencies.add("PENY")
    }

    // checks if you have enough gold to purchase the powerup
    // checks if the power hasn't already been activated
    private fun check(powerItem: PowerItem) : Boolean {
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val activation = settings.getBoolean(powerItem.title, false)
        val currentGold = settings.getInt("Bank", 0)
        if(currentGold < powerItem.price) {
            Toast.makeText(mContext,
                    "You do not have enough money to buy this powerup!",
                    Toast.LENGTH_SHORT)
                    .show()
            return false
        } else if(activation) {
            Toast.makeText(mContext,
                    "You cannot purchase this item twice in one day!",
                    Toast.LENGTH_SHORT)
                    .show()
            return false
        }
        else {
            spendGold(powerItem)
            return true
        }
    }

    // spends the gold if you have confirmed, and you have enough gold
    private fun spendGold(powerItem: PowerItem){

        // get the gold from the bank and set new value, by taking away the price of the powerup
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val currentGold = settings.getInt("Bank", 0)
        val totalGold = currentGold - powerItem.price

        // gold to store in database
        val gold = HashMap<String, Any>()
        gold["Gold"] = totalGold
        Log.d(tag, "Updated total gold: $totalGold")

        // store gold in bank, update current allowance in shared prefs
        val editor = settings.edit()
        editor.putInt("Bank", totalGold)
        editor.putBoolean(powerItem.title, true)
        editor.apply()

        bankReference.set(gold, SetOptions.merge())
                .addOnSuccessListener { _ ->
                    Toast.makeText(mContext,
                            "Powerup successfully bought",
                            Toast.LENGTH_SHORT)
                            .show()
                }
                .addOnFailureListener { e ->
                    Log.d(tag, e.toString())
                }
    }

    // randomises the exchange rates set in shared preferences
    private fun randomiseExchangeRates() {
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val editor = settings.edit()

        val quid = (Math.random()*exchangeRates.size).toInt()
        editor.putString("QUID", exchangeRates.get(quid))
        exchangeRates.removeAt(quid)

        val dolr = (Math.random()*exchangeRates.size).toInt()
        editor.putString("DOLR", exchangeRates.get((Math.random()*exchangeRates.size).toInt()))
        exchangeRates.removeAt(dolr)

        val shil = (Math.random()*exchangeRates.size).toInt()
        editor.putString("SHIL", exchangeRates.get((Math.random()*exchangeRates.size).toInt()))
        exchangeRates.removeAt(shil)

        editor.putString("PENY", exchangeRates.get((Math.random()*exchangeRates.size).toInt()))
        editor.apply()
    }

    // doubles the values of all the coins on the map
    private fun doubleCoins() {
        val settings = activity!!.getSharedPreferences("map", Context.MODE_PRIVATE)
        val editor = settings.edit()
        val coinzMap = settings.all

        for (k in coinzMap.keys) {
            // for each coin, add  a marker to the map with the relevant information
            val coinJson = coinzMap[k] as String
            val gson = Gson()
            val coin = gson.fromJson(coinJson, Coin::class.java)
            val newvalue = coin.value*2
            coin.value = newvalue
            editor.putString(k, coin.toString())
        }
        editor.apply()
    }

    // randomises the currencies of all coins on the map
    private fun randomiseCurrencies() {
        val settings = activity!!.getSharedPreferences("map", Context.MODE_PRIVATE)
        val editor = settings.edit()
        val coinzMap = settings.all

        for (k in coinzMap.keys) {
            // for each coin, add  a marker to the map with the relevant information
            val coinJson = coinzMap[k] as String
            val gson = Gson()
            val coin = gson.fromJson(coinJson, Coin::class.java)
            coin.currency = currencies.get((Math.random()*4).toInt())
            editor.putString(k, coin.toString())
        }

        editor.apply()
    }
}