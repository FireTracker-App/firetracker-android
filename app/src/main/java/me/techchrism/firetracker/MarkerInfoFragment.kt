package me.techchrism.firetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import me.techchrism.firetracker.firedata.CalFireData
import me.techchrism.firetracker.firedata.FireData
import me.techchrism.firetracker.firedata.ReportedFireData
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MarkerInfoFragment(private val fireData: FireData, private val networkManager: NetworkManager) : Fragment() {

    private var updateTimer: Timer? = null

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

            if(fireData.canRemove) {
                val button = view.findViewById<Button>(R.id.button_remove_marker)
                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    networkManager.removeFire(fireData.internalID)
                    button.text = "Removing..."
                }
            }

            view.findViewById<LinearLayout>(R.id.marker_location).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.marker_burned).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.marker_started).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.marker_contained).visibility = View.GONE
            updateReportedTimeText()
            updateTimer = Timer()
            updateTimer?.schedule(object : TimerTask() {
                override fun run() {
                    activity?.runOnUiThread {
                        updateReportedTimeText()
                    }
                }
            }, 0, 1000)
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
            view.findViewById<TextView>(R.id.marker_started_text).text = DateFormat.getDateInstance().format(
                fireData.started
            )
            if(fireData.percentContained == null) {
                view.findViewById<LinearLayout>(R.id.marker_contained).visibility = View.GONE
            }
            else {
                view.findViewById<TextView>(R.id.marker_contained_text).text = fireData.percentContained.toString() + "%"
            }

            view.findViewById<LinearLayout>(R.id.marker_reported).visibility = View.GONE
        }

        if(fireData.description == null) {
            view.findViewById<LinearLayout>(R.id.marker_description).visibility = View.GONE
        }
        else {
            view.findViewById<TextView>(R.id.marker_description_text).text = fireData.description
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateTimer?.cancel();
    }

    private fun updateReportedTimeText() {
        if(fireData !is ReportedFireData) {
            return
        }

        // Get difference between time reported and current time in hours, minutes, and seconds
        val difference = System.currentTimeMillis() - fireData.reported.time
        val hours = TimeUnit.MILLISECONDS.toHours(difference)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(difference) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(difference) % 60

        // Construct a string for the snippet text
        val timeString = StringBuilder()
        if(difference < 0) {
            timeString.append("now")
        } else {
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
        view?.findViewById<TextView>(R.id.marker_reported_text)?.text = timeString.toString()
    }
}