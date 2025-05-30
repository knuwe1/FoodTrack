package com.foodtrack.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.foodtrack.app.data.local.SessionManager
import com.foodtrack.app.ui.auth.LoginActivity
import com.foodtrack.app.ui.lebensmittel.LebensmittelListActivity
import com.foodtrack.app.ui.welcome.WelcomeActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Check if user is already logged in
            val sessionManager = SessionManager(this)
            if (sessionManager.fetchAuthToken() != null) {
                // User is logged in, go directly to main app
                val intent = Intent(this, LebensmittelListActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return
            }

            // Try to start welcome screen, fallback to login if it fails
            try {
                val intent = Intent(this, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to login if welcome fails
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ultimate fallback - just finish
            finish()
        }
    }
}
