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
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import me.techchrism.firetracker.firedata.CalFireData
import me.techchrism.firetracker.firedata.FireData
import me.techchrism.firetracker.firedata.ReportedFireData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class NetworkManager
    (context: Context, private val userID: UUID) {
    private var requestQueue: RequestQueue = Volley.newRequestQueue(context)
    private var waitingForResponse: Boolean = false
    private var webSocket: WebSocket? = null

    var incidents: HashMap<UUID, FireData> = HashMap()

    lateinit var onNewFire: (FireData) -> Unit
    lateinit var onFireRemoved: (FireData) -> Unit
    lateinit var onError: (String) -> Unit
    var mainHandler: Handler = Handler(Looper.getMainLooper())

    init {
        loadCalFireData()
        loadReportedFireData()
        connectToWebsocket()
    }

    fun pause() {
        webSocket?.disconnect()
    }

    fun resume() {
        loadCalFireData()
        loadReportedFireData()
        connectToWebsocket()
    }

    /**
     * Loads a ReportedFireData object from JSON data
     */
    private fun loadReportedFireData(report: JSONObject) : ReportedFireData {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        format.timeZone = TimeZone.getTimeZone("UTC")
        val id = UUID.nameUUIDFromBytes(report.getString("_id").toByteArray())
        return ReportedFireData(
            id,
            report.getDouble("latitude"),
            report.getDouble("longitude"),
            (if (report.has("description")) report.getString("description") else null),
            format.parse(report.getString("reported"))!!,
            report.getBoolean("canRemove"),
            report.getString("_id")
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
    fun reportFire(id: UUID, latitude: Double, longitude: Double, description: String?) {
        val data = JSONObject()
        data.put("reporter", id.toString())
        data.put("latitude", latitude)
        data.put("longitude", longitude)
        description?.let { data.put("description", it) }
        val reportRequest = JsonObjectRequest(
            Request.Method.POST,
            "https://firetracker.techchrism.me/markers",
            data,
            { response ->
                // Get the returned marker and add it to the map
                val report = response.getJSONObject("marker")
                val fireData = loadReportedFireData(report)
                addFire(fireData)
                waitingForResponse = false
            },
            { error ->
                handleNetworkError(error, "Error while reporting: ")
            }
        )
        waitingForResponse = true
        requestQueue.add(reportRequest)
    }

    /**
     * Remove a fire by id
     */
    fun removeFire(id: String) {
        val removeRequest = JsonObjectRequest(
            Request.Method.DELETE,
            "https://firetracker.techchrism.me/markers/${id}?id=${userID}",
            null,
            { response ->
                // Upon successful removal, the websocket will broadcast
            },
            { error ->
                handleNetworkError(error, "Error while removing: ")
            }
        )
        requestQueue.add(removeRequest)
    }

    /**
     * Add a fire if it doesn't already exist
     */
    private fun addFire(data: FireData) {
        if(incidents.containsKey(data.uniqueID)) {
            return
        }
        incidents[data.uniqueID] = data
        if (this::onNewFire.isInitialized) {
            onNewFire(data)
        }
    }

    /**
     * Remove a fire
     */
    private fun removeFire(data: FireData) {
        if(!incidents.containsKey(data.uniqueID)) {
            return
        }
        incidents.remove(data.uniqueID)
        if (this::onFireRemoved.isInitialized) {
            onFireRemoved(data)
        }
    }

    /**
     * Loads reported fire data from the FireTracker server
     */
    private fun loadReportedFireData() {
        val reportedFireDataRequest = JsonArrayRequest(
            Request.Method.GET,
            "https://firetracker.techchrism.me/markers?id=${userID}",
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
                        (if (incident.isNull("SearchDescription")) null else incident.getString("SearchDescription")),
                        incident.getString("Name"),
                        incident.getString("Location"),
                        incident.getBoolean("Active"),
                        format.parse(incident.getString("Started"))!!,
                        if (contained != -1) contained else null,
                        if (acres != -1) acres else null
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
        this.webSocket = WebSocketFactory().createSocket("wss://firetracker.techchrism.me/markers")
        this.webSocket?.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                val body = JSONObject(text!!)
                // {action: 'created'} indicates a new marker
                if (body.getString("action") == "created") {
                    if (waitingForResponse) {
                        return
                    }
                    // Load the marker data and render it on the main thread
                    val fireData = loadReportedFireData(body.getJSONObject("data"))
                    mainHandler.post {
                        addFire(fireData)
                    }
                } else if (body.getString("action") == "removed") {
                    val fireData = loadReportedFireData(body.getJSONObject("data"))
                    mainHandler.post {
                        removeFire(fireData)
                    }
                }
            }
        })
        this.webSocket?.connectAsynchronously()
    }
}