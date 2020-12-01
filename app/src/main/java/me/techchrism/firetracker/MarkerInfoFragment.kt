package me.techchrism.firetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import me.techchrism.firetracker.firedata.CalFireData
import me.techchrism.firetracker.firedata.FireData
import me.techchrism.firetracker.firedata.ReportedFireData

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
        }
        else if(fireData is CalFireData) {
            view.findViewById<TextView>(R.id.marker_title).text = fireData.name
        }

        if(fireData.description == null) {
            view.findViewById<TextView>(R.id.marker_description).visibility = View.GONE
        }
        else {
            view.findViewById<TextView>(R.id.marker_description_text).text = fireData.description
        }
    }
}