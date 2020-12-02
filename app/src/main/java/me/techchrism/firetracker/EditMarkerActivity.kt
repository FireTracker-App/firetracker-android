package me.techchrism.firetracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class EditMarkerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_marker)

        // Set up button functionality to return to main fire map.
        findViewById<Button>(R.id.returnBtnAbout).setOnClickListener() {
            saveDetails()
        }
    }

    private fun saveDetails() {
        val userDescription = findViewById<EditText>(R.id.editTextTextMultiLine).text.toString()

        val result = Intent()
        result.putExtra("descriptionKey", userDescription)
        result.putExtra("requestCode", 1)
        result.putExtra("resultCode", RESULT_OK)

        setResult(RESULT_OK, result)
        finish()
    }
}

