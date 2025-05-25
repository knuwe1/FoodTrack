package com.foodtrack.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.foodtrack.app.MainActivity
import com.foodtrack.app.R

class LoginActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var etLoginEmail: EditText
    private lateinit var etLoginPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoToRegister: Button
    private lateinit var pbLoginLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)
        pbLoginLoading = findViewById(R.id.pbLoginLoading)

        btnLogin.setOnClickListener {
            val email = etLoginEmail.text.toString().trim()
            val password = etLoginPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        authViewModel.authenticationState.observe(this) { state ->
            when (state) {
                is AuthenticationState.Loading -> {
                    pbLoginLoading.visibility = View.VISIBLE
                    btnLogin.isEnabled = false
                    btnGoToRegister.isEnabled = false
                }
                is AuthenticationState.Authenticated -> {
                    pbLoginLoading.visibility = View.GONE
                    btnLogin.isEnabled = true
                    btnGoToRegister.isEnabled = true
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_LONG).show() // Token display can be removed for brevity
                    // Navigate to LebensmittelListActivity
                    val intent = Intent(this, com.foodtrack.app.ui.lebensmittel.LebensmittelListActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is AuthenticationState.Error -> {
                    pbLoginLoading.visibility = View.GONE
                    btnLogin.isEnabled = true
                    btnGoToRegister.isEnabled = true
                    Toast.makeText(this, "Login Failed: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is AuthenticationState.Idle -> {
                    pbLoginLoading.visibility = View.GONE
                    btnLogin.isEnabled = true
                    btnGoToRegister.isEnabled = true
                }
            }
        }
        // Observe error message from ViewModel (alternative to state.message if preferred)
        authViewModel.errorMessage.observe(this) { error ->
            error?.let {
                // Toast.makeText(this, it, Toast.LENGTH_LONG).show() // Already handled by AuthenticationState.Error
                authViewModel.resetErrorMesssage() // Clear error after showing
            }
        }
         authViewModel.isLoading.observe(this) { isLoading ->
            pbLoginLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
            btnGoToRegister.isEnabled = !isLoading
        }
    }
}
