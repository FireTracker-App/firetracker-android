package me.techchrism.firetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import me.techchrism.firetracker.firedata.CalFireData
import me.techchrism.firetracker.firedata.FireData
import me.techchrism.firetracker.firedata.ReportedFireData
import java.text.DateFormat

class MarkerInfoFragment(private val fireData: FireData) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_marker_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(fireData is ReportedFireData) {
            view.findViewById<TextView>(R.id.marker_title).text = "Reported Fire"
            view.findViewById<LinearLayout>(R.id.marker_location).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.marker_burned).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.marker_started).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.marker_contained).visibility = View.GONE
        }
        else if(fireData is CalFireData) {
            view.findViewById<TextView>(R.id.marker_title).text = fireData.name
            view.findViewById<TextView>(R.id.marker_location_text).text = fireData.location
            if(fireData.acresBurned == null) {
                view.findViewById<LinearLayout>(R.id.marker_burned).visibility = View.GONE
            }
            else {
                view.findViewById<TextView>(R.id.marker_burned_text).text = fireData.acresBurned.toString()
            }
            view.findViewById<TextView>(R.id.marker_started_text).text = DateFormat.getDateInstance().format(fireData.started)
            if(fireData.percentContained == null) {
                view.findViewById<LinearLayout>(R.id.marker_contained).visibility = View.GONE
            }
            else {
                view.findViewById<TextView>(R.id.marker_contained_text).text = fireData.percentContained.toString() + "%"
            }
        }

        if(fireData.description == null) {
            view.findViewById<LinearLayout>(R.id.marker_description).visibility = View.GONE
        }
        else {
            view.findViewById<TextView>(R.id.marker_description_text).text = fireData.description
        }
    }
}