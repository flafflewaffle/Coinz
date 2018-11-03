package com.example.s1623165.coinz

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.Mapbox
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = mView.findViewById(R.id.mapview)

        if(mapView != null) {
            mapView!!.onCreate(savedInstanceState)
            mapView!!.onResume()
            mapView!!.getMapAsync(this)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        Mapbox.getInstance(context!!,getString(R.string.access_token))
        map = mapboxMap
        val latlng = LatLng(55.944, -3.188396)
        mapboxMap?.setStyleUrl(Style.MAPBOX_STREETS)
        val camera = CameraPosition.Builder()
        camera.target(latlng)
        camera.zoom(12.0)
        mapboxMap?.moveCamera(CameraUpdateFactory.newCameraPosition(camera.build()))
    }
}