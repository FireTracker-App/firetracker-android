package me.techchrism.firetracker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
        // From https://stackoverflow.com/a/31629308
        mMap.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val info = LinearLayout(this@MapsActivity)
                info.orientation = LinearLayout.VERTICAL
                val title = TextView(this@MapsActivity)
                title.setTextColor(Color.BLACK)
                title.gravity = Gravity.CENTER
                title.setTypeface(null, Typeface.BOLD)
                title.text = marker.title
                val snippet = TextView(this@MapsActivity)
                snippet.setTextColor(Color.GRAY)
                snippet.text = marker.snippet
                info.addView(title)
                info.addView(snippet)
                return info
            }
        })

        // Display fire data if already retrieved
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
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    for (i in 0 until incidents.length()) {
                        val incident = incidents.getJSONObject(i)
                        val contained = incident.optInt("PercentContained", -1)
                        val acres = incident.optInt("AcresBurned", -1)
                        incidentSet.add(FireData(
                                UUID.fromString(incident.getString("UniqueId")),
                                incident.getString("Name"),
                                incident.getString("Location"),
                                incident.getDouble("Latitude"),
                                incident.getDouble("Longitude"),
                                incident.getBoolean("Active"),
                                format.parse(incident.getString("Started"))!!,
                                if (contained != -1) contained else null,
                                if (acres != -1) acres else null,
                                incident.getString("SearchDescription")
                        ))
                    }
                    // Display the data if the map is ready
                    if (this::mMap.isInitialized) {
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
            return
        }

        val dateFormat = DateFormat.getDateFormat(this)
        val numberFormat = NumberFormat.getInstance()

        for(fireData in incidentSet) {
            mMap.addMarker(MarkerOptions()
                    .position(LatLng(fireData.latitude, fireData.longitude))
                    .draggable(false)
                    .title(fireData.name)
                    .visible(fireData.active)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fire_icon))
                    .snippet("""
                        Location: ${fireData.location}
                        Started: ${dateFormat.format(fireData.started)}
                        Acres Burned: ${fireData.acresBurned?.let { numberFormat.format(it) } ?: "unknown"}
                        Contained: ${fireData.percentContained?.toString()?.plus("%") ?: "unknown"}
                        Description: ${fireData.searchDescription}
                    """.trimIndent())
            )
        }
    }
}