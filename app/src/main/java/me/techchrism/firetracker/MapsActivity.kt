package me.techchrism.firetracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import kotlin.collections.HashSet

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var requestQueue: RequestQueue
    private var incidentSet: HashSet<FireData> = HashSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Load the fire data from the API
        requestQueue = Volley.newRequestQueue(this)
        loadFireData()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Move the camera to California
        val california = LatLng(36.7783, -119.4179)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(california, 5.5f))
        if(incidentSet.size != 0) {
            displayFireData()
        }
    }

    /**
     * Loads the fire data from the API and, if the map is ready, displays it
     */
    private fun loadFireData() {
        val fireDataRequest = JsonObjectRequest(Request.Method.GET, "https://www.fire.ca.gov/umbraco/Api/IncidentApi/GetIncidents", null,
                { response ->
                    val incidents = response.getJSONArray("Incidents")
                    // Iterate through the incidents in the api
                    for(i in 0 until incidents.length()) {
                        val incident = incidents.getJSONObject(i)
                        incidentSet.add(FireData(
                            UUID.fromString(incident.getString("UniqueId")),
                            incident.getString("Name"),
                            incident.getString("Location"),
                            incident.getDouble("Latitude"),
                            incident.getDouble("Longitude"),
                            incident.getBoolean("Active"),
                        ))
                    }
                    // Display the data if the map is ready
                    if(this::mMap.isInitialized) {
                        displayFireData()
                    }
                },
                { error ->
                    // TODO: Handle error
                }
        )
        requestQueue.add(fireDataRequest)
    }

    /**
     * Displays the fire data as markers on the map
     */
    private fun displayFireData() {
        if(!this::mMap.isInitialized) {
            return;
        }

        for(fireData in incidentSet) {
            mMap.addMarker(MarkerOptions()
                    .position(LatLng(fireData.latitude, fireData.longitude))
                    .draggable(false)
                    .title(fireData.name)
                    .visible(fireData.active)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fire_icon))
            )
        }
    }
}