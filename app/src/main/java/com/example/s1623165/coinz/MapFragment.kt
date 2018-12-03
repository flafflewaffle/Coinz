package com.example.s1623165.coinz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import timber.log.Timber
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.*
import kotlinx.android.synthetic.main.activity_map.*

class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener {

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var mContext: Context
    private lateinit var permissionsManager : PermissionsManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mContext = context!!
        Mapbox.getInstance(mContext, getString(R.string.access_token))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = mapView?.findViewById(R.id.mapview)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null) {
            Timber.e("[onMapReady] mapboxMap is null")
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
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if(PermissionsManager.areLocationPermissionsGranted(mContext)) {
            Timber.e("Permissions are granted")
            locationComponent = map?.locationComponent
            locationComponent?.activateLocationComponent(activity!!.applicationContext)

            //Set location engine interval times
            locationComponent?.locationEngine?.interval = 5000
            locationComponent?.locationEngine?.fastestInterval = 1000

            // Set visibility after activation
            locationComponent?.isLocationComponentEnabled = true

            // Customises the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.NORMAL

        } else {
            Timber.e("Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(mContext as Activity)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Timber.e( "Permissions: $permissionsToExplain")
        // Present popup message or dialogue
        val userDialogue = Toast.makeText(mContext,
                permissionsToExplain!!.joinToString(", ", "", "", -1, "..."),
                Toast.LENGTH_LONG)
        userDialogue.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        Timber.e( "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
            val userDialogue = Toast.makeText(mContext,
                    "Please enable your location",
                    Toast.LENGTH_LONG)
            userDialogue.show()
        }
    }

    @SuppressWarnings("MissingPermission")
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

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

}