package me.techchrism.firetracker

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.text.SimpleDateFormat
import java.util.*

class NetworkManager
    (context: Context) {
    private var requestQueue: RequestQueue = Volley.newRequestQueue(context)

    var incidentSet: HashSet<FireData> = HashSet()

    lateinit var onNewFire: (FireData) -> Unit
    lateinit var onFireRemoved: (FireData) -> Unit

    init {
        loadFireData()
    }

    /**
     * Loads the fire data from the API and, if the map is ready, displays it
     */
    private fun loadFireData() {
        val fireDataRequest = JsonObjectRequest(
            Request.Method.GET,
            "https://www.fire.ca.gov/umbraco/Api/IncidentApi/GetIncidents",
            null,
            { response ->
                val incidents = response.getJSONArray("Incidents")
                // Iterate through the incidents in the api
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                for (i in 0 until incidents.length()) {
                    val incident = incidents.getJSONObject(i)
                    val contained = incident.optInt("PercentContained", -1)
                    val acres = incident.optInt("AcresBurned", -1)
                    val fireData = FireData(
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
                    )
                    incidentSet.add(fireData)
                    if(this::onNewFire.isInitialized) {
                        onNewFire(fireData)
                    }
                }
            },
            { error ->
                // TODO: Handle error
            }
        )
        requestQueue.add(fireDataRequest)
    }
}