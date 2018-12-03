package com.example.s1623165.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AsyncPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.example.s1623165.coinz.DownloadCompleteRunner.result
import com.example.s1623165.coinz.R.id.fab
import com.example.s1623165.coinz.R.id.toolbar
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
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class Map : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private val tag = "MapActivity"

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var locationComponent: LocationComponent? = null

    private lateinit var geoJsonString: String
    private lateinit var lastDownloadDate : String
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
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        val settings = getSharedPreferences("wallet", Context.MODE_PRIVATE)
        lastDownloadDate = settings.getString("lastDownloadDate", "")
        mapView?.onStart()
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
            //Test marker
            map?.addMarker(MarkerOptions()
                    .position(LatLng(55.944, -3.188396))
                    .title("University of Edinburgh: George Square"))
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            enableLocation()
            if(lastDownloadDate.equals(getDate())) {
                Log.d(tag, "Coinz for " + getDate() +" downloaded previously.")
            }
            else {
                val url = "http://homepages.inf.ed.ac.uk/stg/coinz/2018/12/02/coinzmap.geojson"
                //val url = "http://homepages.inf.ed.ac.uk/stg/coinz/ + getDate() + /coinzmap.geojson"
                geoJsonString = DownloadFileTask(DownloadCompleteRunner).execute(url).get()
                val settings = getSharedPreferences("wallet", Context.MODE_PRIVATE)
                val editor = settings.edit()
                editor.putString("lastDownloadDate", getDate())
                editor.apply()
                //getCoinz()
            }

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

    private fun getCoinz() {
        val fc = FeatureCollection.fromJson(geoJsonString)
        val featureList = fc.features()?.iterator()
        if (featureList != null) {
            for(f in featureList) createCoin(f)
        }
    }

    private fun createCoin(feature : Feature) {
        // Get location of the coin
        val point = feature.geometry() as Point
        val latLng = LatLng(point.latitude(),point.longitude())

        // Get relevant properties of the coin
        val coinJson = feature.properties()
        val currency = coinJson!!["currency"].toString()
        val value = coinJson!!["value"].toString()
        val id = coinJson!!["id"].toString()

        // Create new instance of coin
        val coin = Coin.Builder()
                .setID(id)
                .setCurrency(currency)
                .setValue(value.toDouble())
                .setLocation(latLng)
                .build()

        // Save coin in wallet
        val settings = getSharedPreferences("wallet", Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(id, coin.toString())
        editor.apply()

        // Add marker to map
        map?.addMarker(MarkerOptions()
                .position(latLng)
                .title(currency)
                .snippet(value))
    }

    private fun getDate() : String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd")
        return dateFormat.format(getDate())
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

interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner : DownloadCompleteListener {
    var result : String? = null
    override fun downloadComplete(result: String) {
        this.result = result
    }
}

class DownloadFileTask(private val caller : DownloadCompleteListener) :
        AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg urls: String?): String = try {
        loadFileFromNetwork(urls[0]!!)
    } catch (e : IOException) {
        "Unable to load content. Check your network connection"
    }

    private fun loadFileFromNetwork(urlString : String) : String {
        val stream : InputStream = downloadUrl(urlString)
        return stream.reader().use { it.readText() }
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect() // Starts the query
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }
}
