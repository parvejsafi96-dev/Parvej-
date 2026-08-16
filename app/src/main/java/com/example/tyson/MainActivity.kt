package com.example.tyson

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val requestAudio = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // no-op
    }
    private val requestContacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> }
    private val requestCall = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudio.launch(Manifest.permission.RECORD_AUDIO)
        requestContacts.launch(Manifest.permission.READ_CONTACTS)
        requestCall.launch(Manifest.permission.CALL_PHONE)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            startService(Intent(this, WakeService::class.java))
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, WakeService::class.java))
        }
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
