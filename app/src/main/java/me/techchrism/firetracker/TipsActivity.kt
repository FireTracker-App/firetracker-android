package me.techchrism.firetracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.github.barteksc.pdfviewer.PDFView

class TipsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips)

        val newPDFView = findViewById<PDFView>(R.id.pdfView)
        newPDFView.fromAsset("CalFire_Ready_Set_Go_Plan.pdf").load()
        // Set up button functionality to return to main fire map.
        val returnButton = findViewById<Button>(R.id.returnBtnAbout)

        returnButton?.setOnClickListener() {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}