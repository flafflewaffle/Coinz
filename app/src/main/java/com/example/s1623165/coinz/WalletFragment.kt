package com.example.s1623165.coinz

import android.app.ProgressDialog.show
import android.content.ClipData
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
import android.widget.EditText
import android.widget.Toast
import com.example.s1623165.coinz.R.id.exchangeRates
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
    private var currentAllowance = 25

    private lateinit var coins : MutableMap<String, Any>
    private lateinit var mContext: Context
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var walletReference: DocumentReference
    private lateinit var bankReference: DocumentReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WalletAdapter
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
        setCurrencyMap()
        setExchangeRates()
        getCoins()
        getAllowance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater?.inflate(R.layout.wallet_fragment, null)
        recyclerView = root.findViewById(R.id.recyclerView)
        return root
    }

    //---------------SETTER FUNCTIONS---------------//

    // set map of currencies to image resources
    private fun setCurrencyMap() {
        currencyImageMap.put("DOLR", R.drawable.ic_circle_blue_24dp)
        currencyImageMap.put("SHIL", R.drawable.ic_circle_red_24dp)
        currencyImageMap.put("QUID", R.drawable.ic_circle_green_24dp)
        currencyImageMap.put("PENY", R.drawable.ic_circle_yellow_24dp)
    }

    //retrieve coins from the database and add them as coin items to the wallet
    private fun getCoins() {
        walletReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        coins = documentSnapshot.data!!
                        if(coins.size == 1) {
                            Toast.makeText(mContext,
                                    "Wallet is empty, collect coins to view them here!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }
                        setWallet()
                        buildRecyclerView()
                        Log.d("WalletFragment", "Wallet retrieved from Firestore with wallet size: ${wallet.size}")
                    } else {
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
        exchangeRates.put("DOLR",settings.getString("DOLR",""))
        exchangeRates.put("PENY",settings.getString("PENY",""))
        exchangeRates.put("SHIL",settings.getString("SHIL",""))

    }

    // sets the wallet by retrieving lal the coins from the database
    private fun setWallet() {
        for (key: String in coins.keys) {
            if(key.equals("Exists")) {
                continue
            }
            val coinJson = coins[key] as String
            val gson = Gson()
            val coin = gson.fromJson(coinJson, Coin::class.java)
            val image = currencyImageMap[coin.currency]!!
            wallet.add(CoinItem(image, coin.currency, coin.value.toString(), coin.id))
        }
    }

    // set the allowance to 25 if not set, otherwise set it to 25 - the number of coins collected
    private fun setAllowance(coinsCollected : Int) {
        val allowance = HashMap<String, Any>()
        allowance["Allowance"] = 25-coinsCollected
        Log.d("Wallet Fragment", "Setting new allowance with $coinsCollected")
        bankReference.set(allowance, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("Login Activity", "Bank successfully setup for user")
                }
                .addOnFailureListener { e ->
                    Log.d("Login Activity", e.toString())
                }
    }

    //retrieves the current allowance from the database
    private fun getAllowance() {
        bankReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if(documentSnapshot.exists()) {
                        val allow = documentSnapshot.get("Allowance")
                        if(allow == null) {
                            Log.d("Wallet Fragment", "allow is null")
                            setAllowance(0)
                        }
                        else {
                            currentAllowance = (allow as Number).toInt()
                            Log.d("Wallet Fragment", "Get Allowance: $currentAllowance")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("Wallet Fragment", e.toString())
                }
    }

    // builds the recycler view and sets the on click listener
    private fun buildRecyclerView() {
        // initialise recycler view and adapter
        layoutManager = LinearLayoutManager(mContext)
        adapter = WalletAdapter(wallet)
        recyclerView.layoutManager = this.layoutManager
        recyclerView.adapter = this.adapter

        //set on click listeners for banking and sending coins to friends
        adapter.setOnItemClickListener { position: Int ->
            showDialogueBank(wallet[position], position)
        }

        adapter.setOnSendClickListener { position: Int ->
            showDialogueSendCoin(wallet[position], position)
        }
    }

    //---------------BANKING COIN---------------//

    // present an alert dialogue when you want to bank a coin
    // if the current allowance is 0, do not allow the user to bank a coin
    private fun showDialogueBank(coinItem: CoinItem, position : Int) {
        Log.d(tag, "dialogue for banking coin")
        getAllowance()

        if(currentAllowance == 0) {
            val builder = AlertDialog.Builder(mContext)
            builder.setTitle("Current Allowance Expended")
            builder.setMessage("You can no longer bank any more coins today as your allowance is 0. Please wait until tomorrow.")
            builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int -> }
            builder.show()
        } else {
            val builder = AlertDialog.Builder(mContext)
            val exchangeRate = exchangeRates[coinItem.title]!!
            val gold = (coinItem.description.toDouble() * exchangeRate.toDouble()).roundToInt()
            builder.setTitle("Would you like to bank this coin?")
            builder.setMessage("This coin is worth $gold in gold.\nYou can bank $currentAllowance more coins today.")
            builder.setPositiveButton("YES") { dialog: DialogInterface?, which: Int ->
                bankCoin(coinItem, position)
            }
            builder.setNegativeButton("NO", { dialog: DialogInterface?, which: Int -> })
            builder.show()
        }
    }

    //This banks the coin the user has chosen
    private fun bankCoin(coinItem: CoinItem, position : Int) {
        Log.d(tag, "Converting coin to gold and sending to bank")

        // current coin's value in gold
        val exchangeRate = exchangeRates[coinItem.title]!!
        var coinInGold = (coinItem.description.toDouble() * exchangeRate.toDouble()).roundToInt()

        //calculating total gold by adding the current coin's value in gold + current amount of gold in bank
        val settings = activity!!.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val currentGold = settings.getInt("Bank", 0)
        getAllowance()
        val totalGold = coinInGold + currentGold

        // gold to store in database
        val gold = HashMap<String, Any>()
        gold["Gold"] = totalGold
        Log.d(tag, "Total gold collected$totalGold")

        // store gold in bank, update current allowance in shared prefs
        val editor = settings.edit()
        editor.putInt("Bank", totalGold)
        editor.apply()

        setAllowance(25-(currentAllowance-1))

        // store gold in database bank
        bankReference.set(gold, SetOptions.merge())
                .addOnSuccessListener { _ ->
                    Toast.makeText(mContext,
                            "Coin cashed into bank",
                            Toast.LENGTH_SHORT)
                            .show()
                    //delete coin from database
                    deleteCoinDatabase(coinItem, position)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(mContext,
                            "Error storing coin, try again later",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d(tag, e.toString())
                }
    }

    // delete coin from the database and update the wallet
    private fun deleteCoinDatabase(coinItem: CoinItem, position : Int) {
        val deleteCoin = HashMap<String,Any>()
        deleteCoin[coinItem.id] = FieldValue.delete()
        walletReference.update(deleteCoin)
                .addOnSuccessListener { _ ->
                    wallet.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
    }

    //---------------SENDING COIN---------------//

    // present an alert dialogue when you want to send a coin to another user
    private fun showDialogueSendCoin(coinItem: CoinItem, position : Int) {
        Log.d(tag, "Dialogue for sending coin to a user")
            val builder = AlertDialog.Builder(mContext)
            val exchangeRate = exchangeRates.get(coinItem.title)!!
            val gold = (coinItem.description.toDouble() * exchangeRate.toDouble()).roundToInt()
            builder.setTitle("Would you like to send this coin to a friend?")
            builder.setMessage("This coin is worth $gold in gold.")
            builder.setPositiveButton("YES") { dialog: DialogInterface?, which: Int ->
                sendToFriend(coinItem, position)
            }
            builder.setNegativeButton("NO", { dialog: DialogInterface?, which: Int -> })
            builder.show()
    }

    //present an alert dialogue to enter email of user you want to send gold to
    private fun sendToFriend(coinItem: CoinItem, position: Int) {
        //edit text with email
        val builder = AlertDialog.Builder(mContext)
        val view = layoutInflater.inflate(R.layout.send_coin, null)
        val email = view.findViewById(R.id.edit_email) as EditText
        builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
            checkEmail(email.text.toString(), coinItem, position)
        }
        builder.setNegativeButton("Cancel", { dialog: DialogInterface?, which: Int -> })
        builder.setView(view)
        builder.show()
    }

    //checks if the entered email address exists within the database
    private fun checkEmail(email : String, coinItem: CoinItem, position: Int) {
        if(email.equals(mAuth.currentUser!!.email)) {
            Toast.makeText(mContext,
                    "Please enter a different user!",
                    Toast.LENGTH_SHORT)
                    .show()
        }
        else {
            db.collection("Users").document(email).collection("User Information")
                    .document("Bank").get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            sendCoin(email, coinItem, position)
                        } else {
                            Toast.makeText(mContext,
                                    "Please enter a valid email!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(mContext,
                                "Error viewing user information",
                                Toast.LENGTH_SHORT)
                                .show()
                        Log.d(tag, e.toString())
                    }
        }
    }

    //if the email address is valid, exchanges the coin into gold and sends the gold to the user
    private fun sendCoin(email : String, coinItem: CoinItem, position: Int) {
        // current coin's value in gold
        val exchangeRate = exchangeRates[coinItem.title]!!
        var coinInGold = (coinItem.description.toDouble() * exchangeRate.toDouble()).roundToInt()

        val userBankRef = db.collection("Users")
                .document(email).collection("User Information")
                .document("Bank")
        val userNoteRef = db.collection("Users")
                .document(email).collection("User Information")
                .document("Notifications")

        userBankRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if(documentSnapshot.exists()) {
                        val currentGold = documentSnapshot["Gold"] as Number
                        val totalGold = currentGold.toInt() + coinInGold

                        // gold to store in database
                        val gold = HashMap<String, Any>()
                        gold["Gold"] = totalGold

                        // notification to store in the database
                        val note = HashMap<String, Any>()
                        note[mAuth.currentUser!!.email!!] = "${mAuth.currentUser!!.email} sent you $coinInGold gold!"

                        userBankRef.set(gold, SetOptions.merge())
                                .addOnSuccessListener { _ ->
                                    Toast.makeText(mContext,
                                            "Gold successfully sent to $email!",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                    deleteCoinDatabase(coinItem, position)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(mContext,
                                            "Error sending gold, please try again later",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                    Log.d("Wallet Fragment", e.toString())
                                }
                        userNoteRef.set(note, SetOptions.merge())
                                .addOnSuccessListener { _ ->
                                    Log.d("Wallet Fragment", "Notified User")
                                }
                                .addOnFailureListener { e ->
                                    Log.d("Wallet Fragment", e.toString())
                                }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(mContext,
                            "Error viewing user's bank, please try again later",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d("Wallet Preference", e.toString())
                }
    }

}