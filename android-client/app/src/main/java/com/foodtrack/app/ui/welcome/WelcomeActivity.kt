package com.foodtrack.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.foodtrack.app.R
import com.foodtrack.app.data.local.SessionManager
import com.foodtrack.app.ui.auth.LoginActivity
import com.foodtrack.app.ui.lebensmittel.LebensmittelListActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnGetStarted: Button
    private lateinit var btnSkip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Check if user is already logged in
            val sessionManager = SessionManager(this)
            if (sessionManager.fetchAuthToken() != null) {
                // User is logged in, go directly to main app
                startActivity(Intent(this, LebensmittelListActivity::class.java))
                finish()
                return
            }

            setContentView(R.layout.activity_welcome_simple)

            initViews()
            setupClickListeners()

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to login if welcome page fails
            navigateToLogin()
        }
    }

    private fun initViews() {
        btnGetStarted = findViewById(R.id.btnGetStarted)
        btnSkip = findViewById(R.id.btnSkip)
    }

    private fun setupClickListeners() {
        btnGetStarted.setOnClickListener {
            navigateToLogin()
        }

        btnSkip.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
