package com.example.s1623165.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.media.AsyncPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.example.s1623165.coinz.DownloadCompleteRunner.result
import com.example.s1623165.coinz.R.id.*
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { _ -> menu() }
        mapView = findViewById(R.id.mapview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        currentDate = getDate()
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView?.onStart()

        val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        lastDownloadDate = settings.getString("lastDownloadDate", "")
        if(lastDownloadDate.equals(currentDate)) {
            geoJsonString = settings.getString("geoJson","")
            Log.d(tag, "Coinz for " + currentDate +" downloaded previously.")
        }
        else {
            val url = "http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson"
            Log.d(tag, "Download from " + url)

            geoJsonString = DownloadFileTask(DownloadCompleteRunner).execute(url).get()
            if(geoJsonString.equals("Unable to load content. Check your network connection")) {
                Log.d(tag, "Check network connection, new map not downloaded")
            }
            else {
                clearMapCoins()
                val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putString("lastDownloadDate", currentDate)
                editor.putString("geoJson", geoJsonString)
                editor.apply()
                setExchangeRates()
                setCoinz()
                setIcons()
            }
        }
        
        showDialogueExchangeRates()
        Log.d(tag, "lastDownloadDate: " + lastDownloadDate)
        Log.d(tag, "curentDate: " + currentDate)
        Log.d(tag, geoJsonString)
    }

    fun menu() {
        val menuIntent = Intent(this, MenuActivity::class.java)
        startActivity(menuIntent)
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            //set the map
            map = mapboxMap
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
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            initialiseCoinMarkers()
        }
    }

    @SuppressLint("MissingPermission")
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
        val userDialogue = Snackbar.make(
                findViewById(R.id.mapCoordinatorLayout),
                permissionsToExplain!!.joinToString(", ", "", "", -1, "..."),
                Snackbar.LENGTH_INDEFINITE)
        userDialogue.show()
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

    private fun setCoinz() {
        Log.d(tag, "Creating Coins from GeoJson")
        val fc = FeatureCollection.fromJson(geoJsonString)
        val featureList = fc.features()?.iterator()
        if (featureList != null) {
            for(f in featureList) createCoin(f)
        }
    }

    private fun setIcons() {
        val icon = IconFactory.getInstance(this)
        coinMarkerIcons.put("DOLR", icon.fromResource(R.drawable.mapbox_marker_icon_default))
        coinMarkerIcons.put("SHIL", icon.fromResource(R.drawable.mapbox_marker_icon_default))
        coinMarkerIcons.put("QUID", icon.fromResource(R.drawable.mapbox_marker_icon_default))
        coinMarkerIcons.put("PENY", icon.fromResource(R.drawable.mapbox_marker_icon_default))
    }

    private fun showDialogueExchangeRates() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exchange Rates")
        builder.setMessage(getExchangeRates())
        builder.setPositiveButton("OK", {dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    private fun showDialogueNotInRange(coin : Coin) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Coin Not In Range")
        builder.setMessage("Coin: ${coin.currency} \nValue: ${coin.value}")
        builder.setPositiveButton("OK", {dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    private fun showDialogueCoinInRange(coin : Coin, marker : Marker) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Would you like to pick up this coin")
        builder.setMessage("Coin: ${coin.currency} \nValue: ${coin.value}")
        builder.setPositiveButton("Yes") { dialog: DialogInterface?, which: Int ->
            val settingsWallet = getSharedPreferences("wallet", Context.MODE_PRIVATE)
            val editorWallet = settingsWallet.edit()
            editorWallet.putString(coin.id, coin.toString())
            editorWallet.apply()

            val settingsMap = getSharedPreferences("map", Context.MODE_PRIVATE)
            val editorMap = settingsMap.edit()
            editorMap.remove(coin.id)
            editorMap.apply()
            map?.removeMarker(marker)
        }
        builder.setNegativeButton("No", {dialog: DialogInterface?, which: Int -> })
        builder.show()
    }

    @SuppressWarnings("MissingPermission")
    private fun inRange(coin : Coin) : Boolean {
        val curLat = map?.locationComponent?.lastKnownLocation?.latitude!!
        val curLon = map?.locationComponent?.lastKnownLocation?.longitude!!
        val curLocation = LatLng(curLat, curLon)
        val distance = coin.distanceTo(curLocation)
        Log.d(tag, "Distance between coin and current location: $distance")
        return (distance <= 50.0)
    }

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

    private fun initialiseCoinMarkers() {
        Log.d(tag, "Inititalising Coin Markers")
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val coinz = settings.all
        for (k in coinz.keys) {
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

    private fun clearMapCoins() {
        val settings = getSharedPreferences("map", Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.clear()
        editor.apply()
    }

    private fun getDate() : String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd")
        return dateFormat.format(Date())
    }

    private fun getExchangeRates() : String {
        val settings = getSharedPreferences(prefsFile, Context.MODE_PRIVATE)

        val quid = settings.getString("QUID","")
        val dolr = settings.getString("DOLR","")
        val peny = settings.getString("PENY","")
        val shil = settings.getString("SHIL","")

        return "QUID: $quid \nDOLR: $dolr \nPENY: $peny \nSHIL: $shil"
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

