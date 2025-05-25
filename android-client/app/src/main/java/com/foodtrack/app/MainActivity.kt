package com.foodtrack.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.foodtrack.app.data.local.SessionManager
import com.foodtrack.app.R // Import R class if not automatically resolved
import com.foodtrack.app.ui.auth.LoginActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        if (sessionManager.fetchAuthToken() == null) {
            // No token, redirect to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish MainActivity so user cannot navigate back to it without login
            return // Stop further execution of onCreate
        }

        // Token exists, proceed with normal app flow
        setContentView(R.layout.activity_main) // Assuming you'll create a layout file activity_main.xml
        // Your main activity logic here (e.g., setup UI, load data)
    }
}
