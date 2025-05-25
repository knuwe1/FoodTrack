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
import com.foodtrack.app.R

class RegisterActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var etRegisterEmail: EditText
    private lateinit var etRegisterPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: Button // This button was in the XML, so I'll add its ID if not already used
    private lateinit var pbRegisterLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etRegisterEmail = findViewById(R.id.etRegisterEmail)
        etRegisterPassword = findViewById(R.id.etRegisterPassword)
        btnRegister = findViewById(R.id.btnRegister)
        pbRegisterLoading = findViewById(R.id.pbRegisterLoading)
        btnGoToLogin = findViewById(R.id.btnGoToLogin) // Make sure this ID exists in activity_register.xml

        btnRegister.setOnClickListener {
            val email = etRegisterEmail.text.toString().trim()
            val password = etRegisterPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.register(email, password)
            } else {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoToLogin.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            // Optional: Clear previous activities from stack if RegisterActivity was launched from LoginActivity
            // intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish() // Finish RegisterActivity
        }

        authViewModel.authenticationState.observe(this) { state ->
            when (state) {
                is AuthenticationState.Loading -> {
                    pbRegisterLoading.visibility = View.VISIBLE
                    btnRegister.isEnabled = false
                    btnGoToLogin.isEnabled = false
                }
                is AuthenticationState.Authenticated -> { // For registration, this means success
                    pbRegisterLoading.visibility = View.GONE
                    btnRegister.isEnabled = true
                    btnGoToLogin.isEnabled = true
                    Toast.makeText(this, "Registration Successful! User: ${state.user?.email}. Please login.", Toast.LENGTH_LONG).show()
                    // Navigate to LoginActivity
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is AuthenticationState.Error -> {
                    pbRegisterLoading.visibility = View.GONE
                    btnRegister.isEnabled = true
                    btnGoToLogin.isEnabled = true
                    Toast.makeText(this, "Registration Failed: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is AuthenticationState.Idle -> {
                    pbRegisterLoading.visibility = View.GONE
                    btnRegister.isEnabled = true
                    btnGoToLogin.isEnabled = true
                }
            }
        }
        authViewModel.isLoading.observe(this) { isLoading ->
            pbRegisterLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !isLoading
            btnGoToLogin.isEnabled = !isLoading
        }

        // Observe error message from ViewModel
        authViewModel.errorMessage.observe(this) { error ->
            error?.let {
                // Toast.makeText(this, it, Toast.LENGTH_LONG).show() // Already handled by AuthenticationState.Error
                authViewModel.resetErrorMesssage() // Clear error after showing
            }
        }
    }
}
