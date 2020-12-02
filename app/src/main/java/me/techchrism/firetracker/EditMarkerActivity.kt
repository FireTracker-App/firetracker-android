package me.techchrism.firetracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class EditMarkerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_marker)

        // Set up button functionality to return to main fire map.
        val returnButton = findViewById<Button>(R.id.returnBtnAbout)

        returnButton?.setOnClickListener() {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}