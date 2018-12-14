package com.example.s1623165.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.media.AsyncPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.example.s1623165.coinz.DownloadCompleteRunner.result
import com.example.s1623165.coinz.R.id.*
import com.google.common.base.CharMatcher.inRange
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_map.*
import java.text.SimpleDateFormat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Time
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToLong

class Map : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    //---------------VARIABLES---------------//

    private val tag = "MapActivity"
    private val prefsFile = "MyPrefsFile"
    private val rainbowCoin = "Rainbow Coin!"
    private val latmax = 55.946233
    private val latmin = 55.942617
    private val lonmax = -3.192473
    private val lonmin = -3.184319

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private var coinMarkerIcons : HashMap<String, Icon> = HashMap()

    private lateinit var geoJsonString: String
    private lateinit var lastDownloadDate : String
    private lateinit var currentDate : String
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var mAuth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var walletReference: DocumentReference
    private lateinit var bankReference: DocumentReference
    private lateinit var rainbowReference: DocumentReference
    private lateinit var collectedReference: DocumentReference
    private lateinit var magicCoin : LatLng

    //---------------INITIALISATION---------------//

    override fun onCreate(savedInstanceState: Bundle?) {
        //initialise mapview, firebase and current date
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        //database document references for easy access
        walletReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Wallet")
        bankReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Bank")
        rainbowReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Rainbow Coin")
        collectedReference = db.collection("Users").document(mAuth.currentUser!!.email!!)
                .collection("User Information").document("Collected")

        // set rainbow coin
        val lat = Random().nextDouble()*(latmax - latmin) + latmin
        val lon = Random().nextDouble()*(lonmax - lonmin) + lonmin
        magicCoin = LatLng(lat, lon)

        fab.setOnClickListener { _ -> menu() }

        mapView = findViewById(R.id.mapview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        currentDate = getDate()
        setIcons()
        setWallet()
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView?.onStart()

        //if the coinz map has already been downloaded, do not attempt again
        val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        lastDownloadDate = settings.getString("lastDownloadDate", "")
        if(lastDownloadDate.equals(currentDate)) {
            geoJsonString = settings.getString("geoJson","")
            Log.d(tag, "Coinz for " + currentDate +" downloaded previously.")
        }
        else {
            val url = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
            Log.d(tag, "Download from " + url)

            // if the download file is valid, save it, otherwise present a dialogue to check network connection
            geoJsonString = DownloadFileTask(DownloadCompleteRunner).execute(url).get()
            if(geoJsonString.equals("Unable to load content. Check your network connection")) {
                showDialogueBadNetworkConnection()
            }
            else {
                // a new day!
                // clear map of previous coins and save the geojson
                // update the last download date and set coins, icons and exchange rates
                // update the powerups to be purchaseable once again
                clearMapCoins()
                // update shared preference
                val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putString("lastDownloadDate", currentDate)
                editor.putString("geoJson", geoJsonString)
                editor.putBoolean("Rainbow Coin", false)
                editor.putBoolean("Randomise Currencies!", false)
                editor.putBoolean("Randomise Exchange Rates!", false)
                editor.putBoolean("Double Coins!", false)
                editor.apply()
                // call setters
                setAllowance()
                setExchangeRates()
                setCoinz()
                setCollected()
                setRainbowCoin()
                showDownloadSuccessful()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.map_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_exchange -> {
                showDialogueExchangeRates()
                return true
            }
            R.id.action_tutorial -> {
                showDialogueTutorial()
                return true
            }
            R.id.action_logout -> {
                showDialogueLogout()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            //set the map
            map = mapboxMap
            // present relevant dialogues if the coin in question is in range or not
            map?.setOnMarkerClickListener { marker ->
                //different functionality for clicking on a rianbow coin
                if(marker.title.equals(rainbowCoin)) {
                    if(inRange(marker))
                    {
                        showDialogueRainbowCoinInRange(marker)
                        true
                    }
                    else {
                        showDialogueRainbowCoinNotInRange()
                        true
                    }
                }
                else {
                    val coinID = marker.snippet
                    val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
                    val coinJson = settings.getString(coinID, "")
                    val gson = Gson()
                    val coin = gson.fromJson(coinJson, Coin::class.java)
                    if (inRange(marker)) {
                        showDialogueCoinInRange(coin, marker)
                        true
                    } else {
                        showDialogueNotInRange(coin)
                        true
                    }
                }
            }
            // enable location/map settings/coin markers
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            initialiseCoinMarkers()
            initialiseRainbowCoin()
        }
    }


    //---------------LOCATIONS AND PERMISSION---------------//


    @SuppressLint("MissingPermission")
    // enables the location using location component
    private fun enableLocation() {
        if(PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            locationComponent = map?.locationComponent
            locationComponent?.activateLocationComponent(this)

            //Set location engine interval times
            locationComponent?.locationEngine?.interval = 5000
            locationComponent?.locationEngine?.fastestInterval = 1000

            // Set visibility after activation
            locationComponent?.isLocationComponentEnabled = true

            // Customises the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.NORMAL

        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag, "Permissions: $permissionsToExplain")
        // Present popup message or dialogue
        Toast.makeText(this,
                permissionsToExplain!!.joinToString(", ", "", "", -1, "..."),
                Toast.LENGTH_LONG)
                .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
            val userDialogue = Toast.makeText(this, "Please enable your location", Toast.LENGTH_LONG)
            userDialogue.show()
        }
    }


    //---------------SETTERS FOR MAP AND DATABASE VALUES---------------//


    // read and store exchange rates from the geojson string
    private fun setExchangeRates() {
        val json = JSONObject(geoJsonString)
        val rates = json.getJSONObject("rates")
        val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("SHIL", rates["SHIL"].toString())
        editor.putString("DOLR", rates["DOLR"].toString())
        editor.putString("QUID", rates["QUID"].toString())
        editor.putString("PENY", rates["PENY"].toString())
        editor.apply()
    }

    // create a new coin for each coin listed in the geo json
    private fun setCoinz() {
        Log.d(tag, "Creating Coins from GeoJson")
        val fc = FeatureCollection.fromJson(geoJsonString)
        val featureList = fc.features()?.iterator()
        if (featureList != null) {
            for(f in featureList) createCoin(f)
        }
    }

    // create coin instance from json and store in shared preference (for map)
    private fun createCoin(feature : Feature) {
        // Get location of the coin
        val point = feature.geometry() as Point
        val latLng = LatLng(point.latitude(),point.longitude())

        // Get relevant properties of the coin
        val coinJson = feature.properties()
        val currency = coinJson!!["currency"].asString
        val value = coinJson!!["value"].asDouble
        val id = coinJson!!["id"].asString

        // Create new instance of coin
        val coin = Coin.Builder()
                .setID(id)
                .setCurrency(currency)
                .setValue(value)
                .setLocation(latLng)
                .build()

        // Save coin in wallet
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(id, coin.toString())
        editor.apply()
    }

    // set icons in the hashmap for each currency
    private fun setIcons() {
        val icon = IconFactory.getInstance(this)
        coinMarkerIcons.put("DOLR", icon.fromResource(R.drawable.map_marker_blue))
        coinMarkerIcons.put("SHIL", icon.fromResource(R.drawable.map_marker_red))
        coinMarkerIcons.put("QUID", icon.fromResource(R.drawable.map_marker_green))
        coinMarkerIcons.put("PENY", icon.fromResource(R.drawable.map_marker_yellow))
    }

    // initialises the wallet in the database with exists = "exists"
    private fun setWallet() {
        walletReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if(documentSnapshot.exists()) {
                        Log.d(tag, "Wallet already exists for user")
                    } else {
                        // if the wallet does not exist, initialise in the database with exists = true
                        // and initialise all map markers onto the map
                        val wallet = HashMap<String, Any>()
                        wallet["Exists"] = "EXISTS"
                        walletReference.set(wallet)
                                .addOnSuccessListener { _ ->
                                    Toast.makeText(this,
                                            "Wallet successfully setup for user",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this,
                                            "Error registering wallet to user",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                    Log.d(tag, e.toString())
                                }
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(tag, e.toString())
                }
    }

    // initialises the collection status of the rainbow coin in the database to false
    private fun setRainbowCoin() {

        val rainbow = HashMap<String, Any>()
        rainbow["Collected"] = false

        rainbowReference.set(rainbow)
                .addOnSuccessListener { _ ->
                    Log.d(tag, "Rainbow coin successfully set")
                }
                .addOnFailureListener { e ->
                    Log.d(tag, "Error setting rainbow coin")
                    Log.d("WalletFragment", e.toString())
                }
    }

    // remove collection status of coins from previous day and set all collection statuses
    // for the new day to false
    private fun setCollected() {
        collectedReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    // if coins form the previous day are in collected, remove all from the document
                    if (documentSnapshot.exists()) {
                        val collection = documentSnapshot.data!!
                        for (k in collection.keys) {
                            val deleteCollected = HashMap<String,Any>()
                            deleteCollected[k] = FieldValue.delete()
                            collectedReference.update(deleteCollected)
                                    .addOnSuccessListener { _ ->
                                        Log.d(tag, "Collection of coin: $k successfully deleted")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(tag, e.toString())
                                    }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(tag, e.toString())
                }

        // set all coins (for today's map) collection status to false
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val coinzMap = settings.all

        for(k in coinzMap.keys) {
            val collect = HashMap<String, Any>()
            collect[k] = false
            collectedReference.set(collect, SetOptions.merge())
                    .addOnSuccessListener { _ ->
                        Log.d(tag, "Collection status of coin: $k successfully set")
                    }
                    .addOnFailureListener { e ->
                        Log.d(tag, e.toString())
                    }
        }
    }

    // reset the allowance to 25 on a new day
    private fun setAllowance() {
        val allowance = HashMap<String, Any>()
        allowance["Allowance"] = 25

        bankReference.set(allowance, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(tag, "Bank successfully setup for user")
                }
                .addOnFailureListener { e ->
                    Log.d(tag, e.toString())
                }
    }


    //---------------ALERT DIALOGUES---------------//


    //present an alert dialogue when a new map is successfully downloaded
    private fun showDownloadSuccessful() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Coin Map Downloaded")
        builder.setMessage("Today's Exchange Rates\n\n"+getExchangeRates())
        builder.setPositiveButton("OK"){dialog: DialogInterface?, which: Int -> }
        builder.show()
    }

    // present an alert dialogue with the formatted exchange rates
    private fun showDialogueExchangeRates() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exchange Rates")
        builder.setMessage(getExchangeRates())
        builder.setPositiveButton("OK"){dialog: DialogInterface?, which: Int -> }
        builder.show()
    }

    // present an alert dialogue with the explanation of the game
    private fun showDialogueTutorial() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tutorial")
        builder.setMessage(R.string.tutorial)
        builder.setPositiveButton("OK"){dialog: DialogInterface?, which: Int -> }
        builder.show()
    }

    // present an alert dialogue if the map was not downloaded due to bad network connection
    private fun showDialogueBadNetworkConnection() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unable to connect to network")
        builder.setMessage("Unable to load today's map, please check your network connection and refresh the app.")
        builder.setPositiveButton("OK"){dialog: DialogInterface?, which: Int -> }
        builder.show()                                                               
    }

    // present an alert dialogue if the coin is not within range of the user
    private fun showDialogueNotInRange(coin : Coin) {
        // the alert dialogue does not do anything bt present info about the coin out of range
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Coin Not In Range")
        builder.setMessage("Coin: ${coin.currency} \nValue: ${coin.value}")
        builder.setPositiveButton("OK"){dialog: DialogInterface?, which: Int -> }
        builder.show()
    }

    // present an alert dialogue if a coin is within range to the user
    private fun showDialogueCoinInRange(coin : Coin, marker : Marker) {
        // build alert dialogue with relevant messages
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Would you like to pick up this coin")
        builder.setMessage("Coin: ${coin.currency} \nValue: ${coin.value}")
        builder.setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
            // add coin to wallet
            Log.d(tag, "Adding coin to wallet")
            val idCoinMap = HashMap<String, Any>()
            idCoinMap[coin.id] = coin.toString()

            walletReference.set(idCoinMap, SetOptions.merge())
                    .addOnSuccessListener { _ ->
                        Toast.makeText(this,
                                "Coin collected",
                                Toast.LENGTH_SHORT)
                                .show()
                        val collect = HashMap<String, Any>()
                        collect[coin.id] = true
                        collectedReference.set(collect, SetOptions.merge())
                                .addOnSuccessListener {

                                    Log.d(tag, "Coin ${coin.id} successfully collected")
                                }
                                .addOnFailureListener {e ->
                                    Log.d(tag, e.toString())
                                }
                        map?.removeMarker(marker)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                                "Error collecting coin, try again later",
                                Toast.LENGTH_SHORT)
                                .show()
                        Log.d(tag, e.toString())
                    }
        }
        builder.setNegativeButton("No") { dialog: DialogInterface?, which: Int -> }
        builder.show()
    }

    // present an alert dialogue if the rainbow coin is not within range of the user
    private fun showDialogueRainbowCoinNotInRange() {
        // the alert dialogue does not do anything bt present info about the coin out of range
        val builder = AlertDialog.Builder(this)
        builder.setTitle("A Mysterious Coin in Sight!")
        builder.setMessage("This coin is not like the others... However you are not in range to pick it up! Let's get a bit closer.")
        builder.setPositiveButton("OK"){dialog: DialogInterface?, which: Int -> }
        builder.show()
    }

    // present an alert dialogue if the rainbow coin is within range of the user
    private fun showDialogueRainbowCoinInRange(marker : Marker) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("You've Found the Rainbow Coin!")
        builder.setMessage("Congratulations! Collecting this coin gives you a 500 gold! The Rainbow Coin appears once a day, so best wait until tomorrow! Who knows where it will be.")
        builder.setPositiveButton("OK") {dialog: DialogInterface?, which: Int ->
            val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
            val currentGold = settings.getInt("Bank", 0)
            val totalGold = currentGold+500

            val gold = HashMap<String, Any>()
            gold["Gold"] = totalGold

            val rainbow = HashMap<String, Any>()
            rainbow["Collected"] = true

            // store gold in bank
            val editor = settings.edit()
            editor.putInt("Bank", totalGold)
            editor.apply()

            bankReference.set(gold, SetOptions.merge())
                    .addOnSuccessListener { _ ->
                        Toast.makeText(this,
                                "500 gold has been added to your bank!",
                                Toast.LENGTH_SHORT)
                                .show()
                        rainbowReference.set(rainbow)
                                .addOnSuccessListener { _ ->
                                    Toast.makeText(this,
                                            "Rainbow coin has successfully been collected!",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this,
                                            "Error collecting rainbow coin, try again later",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                    Log.d(tag, e.toString())
                                }
                        //delete coin from map
                        map?.removeMarker(marker)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                                "Error collecting rainbow coin, try again later",
                                Toast.LENGTH_SHORT)
                                .show()
                        Log.d(tag, e.toString())
                    }
        }
        builder.show()
    }

    //present an alert dialogue when the user would like to logout
    private fun showDialogueLogout() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout confirmation")
        builder.setMessage("Are you sure you would like to logout?")
        builder.setPositiveButton("Yes")
        { dialog: DialogInterface?, which: Int ->
            // clear the current amount of gold in sharedprefs
            val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
            val editor = settings.edit()
            editor.remove("Bank")
            editor.apply()

            mAuth.signOut()
            finish()
            login()
        }
        builder.setNegativeButton("No"){dialog: DialogInterface?, which: Int ->}
        builder.show()
    }


    //---------------HELPER FUNCTIONS---------------//


    // check if coin is within range to location
    @SuppressWarnings("MissingPermission")
    private fun inRange(coin : Marker) : Boolean {
        // Get current location (assert non null)
        val curLat = map?.locationComponent?.lastKnownLocation?.latitude!!
        val curLon = map?.locationComponent?.lastKnownLocation?.longitude!!
        val curLocation = LatLng(curLat, curLon)
        // calculate and return if other coin is within 50m
        val distance = distanceTo(coin.position, curLocation)
        Log.d(tag, "Distance between coin and current location: $distance")
        return (distance <= 25.0)
    }

    //calculates the distance between to LatLng
    private fun distanceTo(location : LatLng, locOther: LatLng): Float {
        val result = FloatArray(1)
        Location.distanceBetween(location.latitude,
                location.longitude,
                locOther.latitude,
                locOther.longitude,
                result)
        return result[0]
    }

    // initialise all coin markers from those stored in shared preferences under 'map'
    // if the coin is collected and stored in your wallet, do not display the marker
    // if the wallet has not been initialised for a user, initialise the wallet with the simple value
    // of exists = "exists"
    private fun initialiseCoinMarkers() {
        Log.d(tag, "Inititalising Coin Markers")
        // Retrieve all coins as json from sharedpreferences
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val coinzMap = settings.all

        //  if the collection status is false, ignore coins collected
        // when displaying markers on the map
        collectedReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if(documentSnapshot.exists()) {
                        val collectionStatus = documentSnapshot.data!!
                        for (k in coinzMap.keys) {
                            // for each coin, add  a marker to the map with the relevant information
                            val collected = collectionStatus[k]
                            if(collected == null || !(collected as Boolean)) {
                                val coinJson = coinzMap[k] as String
                                val gson = Gson()
                                val coin = gson.fromJson(coinJson, Coin::class.java)
                                map?.addMarker(MarkerOptions()
                                        .position(coin.location)
                                        .title(coin.currency)
                                        .snippet(coin.id)
                                        .icon(coinMarkerIcons[coin.currency]))
                            }
                        }
                    } else {
                        // if the wallet does not exist, initialise in the database with exists = true
                        // and initialise all map markers onto the map
                        setCollected()
                        for (k in coinzMap.keys) {
                            // for each coin, add  a marker to the map with the relevant information
                            val coinJson = coinzMap[k] as String
                            val gson = Gson()
                            val coin = gson.fromJson(coinJson, Coin::class.java)
                            map?.addMarker(MarkerOptions()
                                    .position(coin.location)
                                    .title(coin.currency)
                                    .snippet(coin.id)
                                    .icon(coinMarkerIcons[coin.currency]))
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                            "Error accessing coins collected",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.d(tag, e.toString())
                }
    }

    // initialise rainbow coin marker
    // if the rainbow coin collection status has not been initialised in the database, set it
    private fun initialiseRainbowCoin() {
        rainbowReference.get()
                .addOnSuccessListener { documentSnapshot ->
                    if(documentSnapshot.exists()) {
                        val collected = documentSnapshot["Collected"] as Boolean
                        if(collected) {
                            Log.d(tag, "Rainbow Coin has already been collected")
                        }
                        else {
                            map?.addMarker(MarkerOptions()
                                    .position(magicCoin)
                                    .title(rainbowCoin)
                                    .icon(IconFactory.getInstance(this).fromResource(R.drawable.rainbow_star)))
                        }
                    }
                    else {
                        setRainbowCoin()
                        map?.addMarker(MarkerOptions()
                                .position(magicCoin)
                                .title(rainbowCoin)
                                .icon(IconFactory.getInstance(this).fromResource(R.drawable.rainbow_star)))
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                            "Error initialising rainbow coin",
                            Toast.LENGTH_SHORT)
                    Log.d(tag, e.toString())
                }
    }

    // clear the map of coins when download a new map
    private fun clearMapCoins() {
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.clear()
        editor.apply()
    }

    // get the date in the correct format
    private fun getDate() : String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd")
        return dateFormat.format(Date())
    }

    // retrieve a formatted version of all exchange rates
    private fun getExchangeRates() : String {
        val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)

        val quid = settings.getString("QUID","")
        val dolr = settings.getString("DOLR","")
        val peny = settings.getString("PENY","")
        val shil = settings.getString("SHIL","")

        return "QUID: $quid \nDOLR: $dolr \nPENY: $peny \nSHIL: $shil"
    }

    //navigation functions
    fun menu() {
        val menuIntent = Intent(this, MenuActivity::class.java)
        startActivity(menuIntent)
    }

    fun login() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

}

