package me.techchrism.firetracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
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
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var networkManager: NetworkManager
    private lateinit var appID: UUID
    private val california = LatLng(36.7783, -119.4179)
    private val fireMarkers: HashMap<UUID, Marker> = HashMap()
    private lateinit var lastMarkerPos: LatLng
    private lateinit var reportToast: Toast
    private lateinit var cancelToast: Toast
    private lateinit var placedToast: Toast
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Get a unique id for the app
        val prefs = getPreferences(Context.MODE_PRIVATE)
        if(prefs.contains("id")) {
            appID = UUID.fromString(prefs.getString("id", ""))
        } else {
            appID = UUID.randomUUID()
            prefs.edit().putString("id", appID.toString()).apply()
        }

        // Set up the network manager
        networkManager = NetworkManager(this)
        networkManager.onError = { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Obtain main report button for use in methods below.
        val reportButton = findViewById<Button>(R.id.report)
        // Obtain marker placed button for use in methods below.
        val markerPlacedButton = findViewById<Button>(R.id.markerPlaced)
        markerPlacedButton.visibility = View.GONE
        // Obtain cancel placement button for use in methods below.
        val cancelPlacementButton = findViewById<Button>(R.id.cancel_button)
        cancelPlacementButton.visibility = View.GONE
        // The new marker to be initialized in reportFire()
        lateinit var newMarker: Marker

        // Initialize toasts
        reportToast = Toast.makeText(
            this,
            "Hold down new created orange and yellow striped fire icon to move it",
            Toast.LENGTH_LONG
        )
        cancelToast = Toast.makeText(this, "Canceled placement.", Toast.LENGTH_LONG)
        placedToast = Toast.makeText(this, "Fire Successfully placed on map.", Toast.LENGTH_LONG)

        reportButton?.setOnClickListener() {
            newMarker = reportFire()
            // Set the button to gone while the user sets the location of the marker.
            reportButton.visibility = View.INVISIBLE
            // Give the user the ability to cancel placing a fire on the map.
            cancelPlacementButton.visibility = View.VISIBLE
            // Return the placedButton button.
            markerPlacedButton.visibility = View.VISIBLE
        }

        markerPlacedButton?.setOnClickListener() {
            // If the report or cancel toast is still showing, cancel it
            reportToast.cancel()
            cancelToast.cancel()

            // Set up & show placed toast
            placedToast.setGravity(Gravity.BOTTOM, 0, 200)
            placedToast.show()

            // Report a fire to the server
            networkManager.reportFire(appID, lastMarkerPos.latitude, lastMarkerPos.longitude)
            newMarker.remove()

            // Set the button to gone while the user sets the location of the marker
            markerPlacedButton.visibility = View.GONE
            // Hide the cancel placement button
            cancelPlacementButton.visibility = View.GONE
            // Return the report button
            reportButton.visibility = View.VISIBLE
        }

        cancelPlacementButton?.setOnClickListener() {
            // If the report or placed toast is still showing, cancel it
            reportToast.cancel()
            placedToast.cancel()
            // Set up & show cancel toast
            cancelToast.setGravity(Gravity.BOTTOM, 0, 200)
            cancelToast.show()
            // Remove the temp marker
            newMarker.remove()
            // Return the report button
            reportButton.visibility = View.VISIBLE
            // Hide the marker placed button
            markerPlacedButton.visibility = View.GONE
            // Hide the cancel button
            cancelPlacementButton.visibility = View.GONE
        }

        // Update the marker data for reported fires
        val markerDataTimer = Timer()
        markerDataTimer.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    for (marker in fireMarkers.values) {
                        val data = marker.tag
                        if (data is ReportedFireData) {
                            updateReportedFireMarker(data, marker)
                        }
                    }
                }
            }
        }, 0, 1000)
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

        // Set the coords to starting coords in case user doesn't move the icon
        lastMarkerPos = california

        // Add listener for drag event to get coordinates of marker
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker?) {}
            override fun onMarkerDrag(p0: Marker?) {}

            override fun onMarkerDragEnd(marker: Marker) {
                lastMarkerPos = marker.position
            }
        })

        // Set marker window to support new lines
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
        if(networkManager.incidents.size > 0) {
            for(fireData: FireData in networkManager.incidents.values) {
                addFireMarker(fireData)
            }
        }
    }

    private fun updateReportedFireMarker(data: ReportedFireData, marker: Marker) {
        // Get difference between time reported and current time in hours, minutes, and seconds
        val difference = System.currentTimeMillis() - data.reported.time
        val hours = TimeUnit.MILLISECONDS.toHours(difference)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(difference) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(difference) % 60

        // Construct a string for the snippet text
        val timeString = StringBuilder("Reported ")
        if(difference < 0) {
            timeString.append("now")
        }
        else {
            if(hours > 0) {
                timeString.append(hours).append(" hour")
                if(hours != 1L) timeString.append("s")
                timeString.append(" ").append(minutes).append(" minute")
                if(minutes != 1L) timeString.append("s")
            } else {
                // Only show seconds if hours is 0
                if(minutes != 0L) {
                    timeString.append(minutes).append(" minute")
                    if(minutes != 1L) timeString.append("s")
                    timeString.append(" ")
                }
                timeString.append(seconds).append(" second")
                if(seconds != 1L) timeString.append("s")
            }
            timeString.append(" ago")
        }

        marker.snippet = timeString.toString()
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
        }
        val marker = mMap.addMarker(markerOptions)
        marker.tag = fireData
        fireMarkers[fireData.uniqueID] = marker
    }

    /**
     * Places a pin on user location; allows user to place pin
     * Opens a dialog for the user to report a local fire
     */
    private fun reportFire(): Marker {
        // If the cancel or placed toast is still showing, cancel it
        cancelToast.cancel()
        placedToast.cancel()
        // Set up & show report toast
        reportToast.setGravity(Gravity.BOTTOM, 0, 200)
        reportToast.show()
        // Add report marker
        return mMap.addMarker(
            MarkerOptions()
                .position(california)
                .draggable(true)
                .title("New Fire Report")
                .visible(true)
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.report_fire_icon))
                .icon(BitmapDescriptorFactory.fromBitmap(generateLargeIcon(this)))
        )
    }

    /**
     * Function only used for creating a user-generated temporary marker.
     * We want bigger markers for this to make it clearer to the user where the marker is.
     */
    fun generateLargeIcon(context: Context): Bitmap {
        val height = 150
        val width = 150
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.report_fire_icon)
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    /**
     * Function to create the menu in the top right for various functions.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.about_this_project -> {
                val intent = Intent(this, CreditsActivity::class.java)
                startActivity(intent)
            }
            R.id.fire_safety_tips -> {
                val intent = Intent(this, TipsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}