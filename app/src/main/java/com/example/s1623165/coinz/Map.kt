package com.example.s1623165.coinz

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView

import kotlinx.android.synthetic.main.activity_map.*

class Map : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        private lateinit var mapView: MapView

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { _ -> menu() }

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapview = findViewById(R.id.mapview)
    }

    fun menu() {
        val menuIntent = Intent(this, MenuActivity::class.java)
        startActivity(menuIntent)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
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
}
