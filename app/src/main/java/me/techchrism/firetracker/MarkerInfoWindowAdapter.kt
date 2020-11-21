package me.techchrism.firetracker

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class MarkerInfoWindowAdapter(activity: Activity) : GoogleMap.InfoWindowAdapter {
    private val view: View = activity.layoutInflater.inflate(R.layout.marker_info, null)

    override fun getInfoWindow(marker: Marker): View? {
        //TODO get fire data as marker tag
        /*if(marker.tag !is FireData) {
            return null
        }
        val data: FireData = marker.tag as FireData*/
        view.findViewById<TextView>(R.id.fire_title).text = marker.title
        view.findViewById<TextView>(R.id.fire_description).text = marker.snippet

        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}