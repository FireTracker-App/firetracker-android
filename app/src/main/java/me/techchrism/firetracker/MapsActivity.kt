package me.techchrism.firetracker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import me.techchrism.firetracker.firedata.CalFireData
import me.techchrism.firetracker.firedata.FireData
import me.techchrism.firetracker.firedata.ReportedFireData
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var networkManager: NetworkManager
    private val california = LatLng(36.7783, -119.4179)
    private val fireMarkers: HashMap<UUID, Marker> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Set up the network manager
        networkManager = NetworkManager(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Obtain main report button for use in methods below.
        val reportButton = findViewById<Button>(R.id.report)
        // Obtain marker placed button for use in methods below.
        val markerPlacedButton = findViewById<Button>(R.id.markerPlaced)
        markerPlacedButton.visibility = View.GONE;
        // The new marker to be initialized in reportFire()
        lateinit var newMarker: Marker

        reportButton?.setOnClickListener() { // Whenever this button is clicked...
            newMarker = reportFire();
            // Set the button to gone while the user sets the location of the marker.
            reportButton.visibility = View.GONE;
            // Return the placedButton button.
            markerPlacedButton.visibility = View.VISIBLE;
        }

        markerPlacedButton?.setOnClickListener() { // Whenever this button is clicked...
            enterFireInfo(newMarker);
            // Set the button to gone while the user sets the location of the marker.
            markerPlacedButton.visibility = View.GONE;
            // Return the report button.
            reportButton.visibility = View.VISIBLE;
        }
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

        // Turn on Google's built in zoom buttons
        val uiSettings: UiSettings = googleMap.uiSettings
        uiSettings.isZoomControlsEnabled = true

        // Move the camera to California
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

        // Set up new marker callback
        networkManager.onNewFire = this::addFireMarker
        // If fires have already been loaded, add them to the map
        if(networkManager.incidentSet.size > 0) {
            for(fireData: FireData in networkManager.incidentSet) {
                addFireMarker(fireData)
            }
        }
    }

    private fun addFireMarker(fireData: FireData) {
        if (!this::mMap.isInitialized || fireMarkers.containsKey(fireData.uniqueID)) {
            return
        }
        val dateFormat = DateFormat.getDateFormat(this)
        val numberFormat = NumberFormat.getInstance()

        val markerOptions = MarkerOptions()
            .position(LatLng(fireData.latitude, fireData.longitude))
            .draggable(false)

        // Customize marker depending on fire type
        if(fireData is CalFireData) {
            markerOptions.title(fireData.name)
                .visible(fireData.active)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.calfire_fire_icon))
                .snippet("""
                        Location: ${fireData.location}
                        Started: ${dateFormat.format(fireData.started)}
                        Acres Burned: ${fireData.acresBurned?.let { numberFormat.format(it) } ?: "unknown"}
                        Contained: ${fireData.percentContained?.toString()?.plus("%") ?: "unknown"}
                        Description: ${fireData.searchDescription}
                    """.trimIndent())
        } else if(fireData is ReportedFireData) {
            markerOptions.title("Reported Fire")
                .visible(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.reported_fire_icon))
                .snippet("Reported: ${dateFormat.format(fireData.reported)}")
        }
        val marker = mMap.addMarker(markerOptions)
        fireMarkers[fireData.uniqueID] = marker;
    }

    /**
     * Places a pin on user location; allows user to place pin
     * Opens a dialog for the user to report a local fire
     */
    private fun reportFire(): Marker {
        Toast.makeText(this, "Hold down new orange and yellow striped fire icon to move it.", Toast.LENGTH_SHORT).show()
        return mMap.addMarker(
                MarkerOptions()
                        .position(california)
                        .draggable(true)
                        .title("New Fire Report")
                        .visible(true)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.report_fire_icon))
        )
    }

    /**
     * User enters fire information after placing down the pin on the map
     */
    private fun enterFireInfo(placedMarker: Marker) {
        //TODO
        placedMarker.isDraggable = false
        placedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.reported_fire_icon))
    }
}