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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToLong

class Map : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private val tag = "MapActivity"
    private val prefsFile = "MyPrefsFile"

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


    override fun onCreate(savedInstanceState: Bundle?) {
        //initialise mapview, firebase and current date
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fab.setOnClickListener { _ -> menu() }
        mapView = findViewById(R.id.mapview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        currentDate = getDate()
        setIcons()
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
                //clear map of previous coins and save the geojson
                //update the last download date and set coins, icons and exchange rates
                clearMapCoins()
                val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putString("lastDownloadDate", currentDate)
                editor.putString("geoJson", geoJsonString)
                editor.apply()
                setExchangeRates()
                setCoinz()
                showDownloadSuccessful()
            }
        }

        //always log and present exchange rates on start
        Log.d(tag, "lastDownloadDate: " + lastDownloadDate)
        Log.d(tag, "curentDate: " + currentDate)
        Log.d(tag, geoJsonString)
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
            R.id.action_settings -> {
                settings()
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
                val coinID = marker.snippet
                val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
                val coinJson = settings.getString(coinID,"")
                val gson = Gson()
                val coin = gson.fromJson(coinJson, Coin::class.java)
                if(inRange(coin)) {
                    showDialogueCoinInRange(coin, marker)
                    true
                }
                else {
                    showDialogueNotInRange(coin)
                    true
                }
            }
            // enable location/map settings/coin markers
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            initialiseCoinMarkers()
        }
    }

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

    // set icons in the hashmap for each currency
    private fun setIcons() {
        val icon = IconFactory.getInstance(this)
        //val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_map_marker_green_24dp)
        //coinMarkerIcons.put("DOLR", icon.fromBitmap(bitmap))
        coinMarkerIcons.put("DOLR", icon.fromResource(R.drawable.mapbox_marker_icon_default))
        coinMarkerIcons.put("SHIL", icon.fromResource(R.drawable.mapbox_marker_icon_default))
        coinMarkerIcons.put("QUID", icon.fromResource(R.drawable.mapbox_marker_icon_default))
        coinMarkerIcons.put("PENY", icon.fromResource(R.drawable.mapbox_marker_icon_default))
    }

    //present an alert dialogue when a new map is successfully downloaded
    private fun showDownloadSuccessful() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Coin Map Downloaded")
        builder.setMessage("Today's Exchange Rates\n\n"+getExchangeRates())
        builder.setPositiveButton("OK", {dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    // present an alert dialogue with the formatted exchange rates
    private fun showDialogueExchangeRates() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exchange Rates")
        builder.setMessage(getExchangeRates())
        builder.setPositiveButton("OK", {dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    // present an alert dialogue if the map was not downloaded due to bad network connection
    private fun showDialogueBadNetworkConnection() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unable to connect to network")
        builder.setMessage("Unable to load today's map, please check your network connection and refresh the app.")
        builder.setPositiveButton("OK", {dialog: DialogInterface?, which: Int -> })
        builder.show()                                                               
    }

    // present an alert dialogue if the coin is not within range of the user
    private fun showDialogueNotInRange(coin : Coin) {
        // the alert dialogue does not do anything bt present info about the coin out of range
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Coin Not In Range")
        builder.setMessage("Coin: ${coin.currency} \nValue: ${coin.value}")
        builder.setPositiveButton("OK", {dialog: DialogInterface?, which: Int -> })
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
            idCoinMap.put(coin.id, coin.toString())

            db.collection("Users")
                .document(mAuth.uid!!)
                .collection("User Information")
                .document("Wallet")
                .set(idCoinMap, SetOptions.merge())
                    .addOnSuccessListener { _ ->
                        Toast.makeText(this,
                                "Coin collected",
                                Toast.LENGTH_SHORT)
                                .show()

                        // remove coin from map
                        val settingsMap = getSharedPreferences("map", Context.MODE_PRIVATE)
                        val editorMap = settingsMap.edit()
                        editorMap.remove(coin.id)
                        editorMap.apply()
                        map?.removeMarker(marker)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                                "Error collecting coin",
                                Toast.LENGTH_SHORT)
                                .show()
                        Log.d(tag, e.toString())
                    }
        }
        builder.setNegativeButton("No", {dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    //present an alert dialogue when the user would like to logout
    private fun showDialogueLogout() {
        // alert dialogue does no action except present information on map start
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout confirmation")
        builder.setMessage("Are you sure you would like to logout?")
        builder.setPositiveButton("Yes")
        { dialog: DialogInterface?, which: Int ->
            mAuth.signOut()
            finish()
            login()
        }
        builder.setNegativeButton("No", {dialog: DialogInterface?, which: Int ->})
        builder.show()
    }

    // check if coin is within range to location
    @SuppressWarnings("MissingPermission")
    private fun inRange(coin : Coin) : Boolean {
        // Get current location (assert non null)
        val curLat = map?.locationComponent?.lastKnownLocation?.latitude!!
        val curLon = map?.locationComponent?.lastKnownLocation?.longitude!!
        val curLocation = LatLng(curLat, curLon)
        // calculate and return if other coin is within 50m
        val distance = coin.distanceTo(curLocation)
        Log.d(tag, "Distance between coin and current location: $distance")
        return (distance <= 50.0)
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

    // initialise all coin markers from those stored in shared preferences under 'map'
    private fun initialiseCoinMarkers() {
        Log.d(tag, "Inititalising Coin Markers")
        // Retrieve all coins as json from sharedpreferences
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val coinz = settings.all
        for (k in coinz.keys) {
            // for each coin, add  a marker to the map with the relevant information
            val coinJson = coinz.get(k) as String
            val gson = Gson()
            val coin = gson.fromJson(coinJson, Coin::class.java)
            map?.addMarker(MarkerOptions()
                .position(coin.location)
                .title(coin.currency)
                .snippet(coin.id)
                .icon(coinMarkerIcons[coin.currency]))
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

    fun menu() {
        val menuIntent = Intent(this, MenuActivity::class.java)
        startActivity(menuIntent)
    }

    fun settings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
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

