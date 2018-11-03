package com.example.s1623165.coinz

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.Style
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.*

class MapFragment : Fragment(), OnMapReadyCallback {

    lateinit var mView: View
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var latlng = LatLng(55.944, -3.188396)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(context!!, getString(R.string.access_token))
        mapView = mView.findViewById(R.id.mapview)
        mapView!!.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var root = inflater.inflate(R.layout.map_fragment, container, false)
        //Mapbox.getInstance(context!!, getString(R.string.access_token))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var mapFragment: SupportMapFragment
        mapFragment = fragmentManager!!.findFragmentById(R.id.mapview) as SupportMapFragment
        mapFragment.getMapAsync(this)
//
//        var transaction =
//               childFragmentManager.beginTransaction()
//
//        var options = MapboxMapOptions()
//        options.styleUrl(Style.MAPBOX_STREETS)
//        options.camera(CameraPosition.Builder()
//                .target(latlng)
//                .zoom(12.0)
//                .build())
//        var mapFragment = SupportMapFragment.newInstance(options)
//        transaction.add(R.id.container, mapFragment, "com.mapbox.map")
//        transaction.commit()
//
//        mapFragment.getMapAsync(this)

        mapView = mView.findViewById(R.id.mapview)
        if(mapView != null) {
            mapView!!.onCreate(savedInstanceState)
            mapView!!.onResume()
            mapView!!.getMapAsync(this)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null){
            Log.d(tag, "[onMapReady] mapboxMap is null")
        }
        else {
            map = mapboxMap
            var markerOptions = MarkerOptions().apply {
                title("Hello Mapbox")
                position(latlng)
            }
            map!!.addMarker(markerOptions)
            map!!.moveCamera(CameraUpdateFactory.newLatLng(latlng))
            //map?.uiSettings?.isCompassEnabled = true
            //map?.uiSettings?.isZoomControlsEnabled = true
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

    }

}