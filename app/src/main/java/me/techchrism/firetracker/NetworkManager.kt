package me.techchrism.firetracker

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.koushikdutta.async.http.AsyncHttpClient
import me.techchrism.firetracker.firedata.CalFireData
import me.techchrism.firetracker.firedata.FireData
import me.techchrism.firetracker.firedata.ReportedFireData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class NetworkManager
    (context: Context) {
    private var requestQueue: RequestQueue = Volley.newRequestQueue(context)

    var incidentSet: HashSet<FireData> = HashSet()

    lateinit var onNewFire: (FireData) -> Unit
    lateinit var onFireRemoved: (FireData) -> Unit
    lateinit var onError: (String) -> Unit
    var mainHandler: Handler = Handler(Looper.getMainLooper())

    init {
        //loadCalFireData()
        loadReportedFireData()
        connectToWebsocket()
    }

    /**
     * Loads a ReportedFireData object from JSON data
     */
    private fun loadReportedFireData(report: JSONObject) : ReportedFireData {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val id = UUID.nameUUIDFromBytes(report.getString("_id").toByteArray())
        return ReportedFireData(
            id,
            report.getDouble("latitude"),
            report.getDouble("longitude"),
            format.parse(report.getString("reported"))!!
        )
    }

    /**
     * Parse network error and call the callback if initialized
     */
    private fun handleNetworkError(error: VolleyError, prefix: String) {
        if (!this::onError.isInitialized) {
            return
        }
        // Toot toot ""train wreck code""
        val response = error.networkResponse?.data?.let { JSONObject(String(it)) }
        if(response == null) {
            onError(prefix + "unknown network error")
        } else {
            onError(prefix + response.getString("message"))
        }
    }

    /**
     * Report a fire with the app id and coordinates
     */
    fun reportFire(id: UUID, latitude: Double, longitude: Double) {
        val data = JSONObject()
        data.put("reporter", id.toString())
        data.put("latitude", latitude)
        data.put("longitude", longitude)
        val reportRequest = JsonObjectRequest(
            Request.Method.POST,
            "https://firetracker.techchrism.me/markers",
            data,
            { response ->
                // Get the returned marker and add it to the map
                val report = response.getJSONObject("marker")
                val fireData = loadReportedFireData(report)
                addFire(fireData)
            },
            { error ->
                handleNetworkError(error, "Error while reporting: ")
            }
        )
        requestQueue.add(reportRequest)
    }

    /**
     * Add a fire if it doesn't already exist
     */
    private fun addFire(data: FireData) {
        for(check in incidentSet) {
            if(check.uniqueID == data.uniqueID) {
                return
            }
        }
        incidentSet.add(data)
        if (this::onNewFire.isInitialized) {
            onNewFire(data)
        }
    }

    /**
     * Loads reported fire data from the FireTracker server
     */
    private fun loadReportedFireData() {
        val reportedFireDataRequest = JsonArrayRequest(
            Request.Method.GET,
            "https://firetracker.techchrism.me/markers",
            null,
            { response ->
                // Iterate through the reports in the api
                for (i in 0 until response.length()) {
                    val report = response.getJSONObject(i)
                    val fireData = loadReportedFireData(report)
                    addFire(fireData)
                }
            },
            { error ->
                handleNetworkError(error, "Error while loading reported fire data: ")
            }
        )
        requestQueue.add(reportedFireDataRequest)
    }

    /**
     * Loads the fire data from the CalFire API
     */
    private fun loadCalFireData() {
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
                    val fireData = CalFireData(
                        UUID.fromString(incident.getString("UniqueId")),
                        incident.getDouble("Latitude"),
                        incident.getDouble("Longitude"),
                        incident.getString("Name"),
                        incident.getString("Location"),
                        incident.getBoolean("Active"),
                        format.parse(incident.getString("Started"))!!,
                        if (contained != -1) contained else null,
                        if (acres != -1) acres else null,
                        incident.getString("SearchDescription")
                    )
                    addFire(fireData)
                }
            },
            { error ->
                handleNetworkError(error, "Error while loading CalFire data: ")
            }
        )
        requestQueue.add(fireDataRequest)
    }

    private fun connectToWebsocket() {
        AsyncHttpClient.getDefaultInstance().websocket("wss://firetracker.techchrism.me/markers",
            "wss",
            AsyncHttpClient.WebSocketConnectCallback { ex, webSocket ->
                if (ex != null) {
                    ex.printStackTrace()
                    return@WebSocketConnectCallback
                }
                webSocket.setStringCallback { stringData ->
                    // Parse message body as json
                    val body = JSONObject(stringData)
                    // {action: 'created'} indicates a new marker
                    if (body.getString("action") == "created") {
                        // Load the marker data and render it on the main thread
                        val fireData = loadReportedFireData(body.getJSONObject("data"))
                        mainHandler.post {
                            addFire(fireData)
                        }
                    }
                }
            })
    }
}