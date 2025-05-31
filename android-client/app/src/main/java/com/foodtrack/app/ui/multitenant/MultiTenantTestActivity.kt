package com.foodtrack.app.ui.multitenant

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foodtrack.app.R
import com.foodtrack.app.data.local.SessionManager
import com.foodtrack.app.data.model.*
import com.foodtrack.app.data.network.RetrofitClient
import com.foodtrack.app.databinding.ActivityMultiTenantTestBinding
import kotlinx.coroutines.launch

class MultiTenantTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMultiTenantTestBinding
    private lateinit var sessionManager: SessionManager
    private val apiService by lazy { RetrofitClient.getApiService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiTenantTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()

        // Auto-test if user is logged in
        if (sessionManager.fetchAuthToken() != null) {
            testAllApis()
        } else {
            testLogin()
        }
    }

    private fun setupUI() {
        binding.apply {
            btnTestLogin.setOnClickListener { testLogin() }
            btnTestHouseholds.setOnClickListener { testHouseholds() }
            btnTestStorageLocations.setOnClickListener { testStorageLocations() }
            btnTestPackages.setOnClickListener { testPackages() }
            btnTestLebensmittel.setOnClickListener { testLebensmittel() }
            btnTestCreateItem.setOnClickListener { testCreateLebensmittel() }
            btnTestAll.setOnClickListener { testAllApis() }
        }
    }

    private fun testLogin() {
        lifecycleScope.launch {
            try {
                appendLog("🔐 Testing Login...")

                val loginRequest = UserLogin(
                    username = "admin@foodtrack.com",
                    password = "admin"
                )

                val response = apiService.loginUser(loginRequest)

                if (response.isSuccessful) {
                    val token = response.body()
                    if (token != null) {
                        sessionManager.saveAuthToken(token.access_token)
                        appendLog("✅ Login successful!")
                        appendLog("Token: ${token.access_token.take(20)}...")

                        // Test user info
                        testCurrentUser()
                    } else {
                        appendLog("❌ Login failed: No token received")
                    }
                } else {
                    appendLog("❌ Login failed: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Login error: ${e.message}")
                Log.e("MultiTenantTest", "Login error", e)
            }
        }
    }

    private fun testCurrentUser() {
        lifecycleScope.launch {
            try {
                appendLog("\n👤 Testing Current User...")

                val response = apiService.getCurrentUser()

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        appendLog("✅ User: ${user.email}")
                        appendLog("   Household ID: ${user.householdId}")
                        appendLog("   Role: ${user.role}")
                    }
                } else {
                    appendLog("❌ Get user failed: ${response.code()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Get user error: ${e.message}")
            }
        }
    }

    private fun testHouseholds() {
        lifecycleScope.launch {
            try {
                appendLog("\n🏠 Testing Households API...")

                val response = apiService.getMyHouseholds()

                if (response.isSuccessful) {
                    val households = response.body()
                    if (households != null) {
                        appendLog("✅ Found ${households.size} household(s)")
                        households.forEach { household ->
                            appendLog("   - ${household.name} (ID: ${household.id})")
                        }
                    } else {
                        appendLog("❌ Households response body is null")
                    }
                } else {
                    appendLog("❌ Households failed: ${response.code()} - ${response.message()}")
                    appendLog("   Error body: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Households error: ${e.message}")
                Log.e("MultiTenantTest", "Households error", e)
            }
        }
    }

    private fun testStorageLocations() {
        lifecycleScope.launch {
            try {
                appendLog("\n📦 Testing Storage Locations API...")

                val response = apiService.getStorageLocations()

                if (response.isSuccessful) {
                    val locations = response.body()
                    if (locations != null) {
                        appendLog("✅ Found ${locations.size} storage location(s)")
                        locations.forEach { location ->
                            appendLog("   - ${location.name} (${location.locationType})")
                        }
                    }
                } else {
                    appendLog("❌ Storage locations failed: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Storage locations error: ${e.message}")
                Log.e("MultiTenantTest", "Storage locations error", e)
            }
        }
    }

    private fun testPackages() {
        lifecycleScope.launch {
            try {
                appendLog("\n📋 Testing Packages API...")

                val response = apiService.getPackages()

                if (response.isSuccessful) {
                    val packages = response.body()
                    if (packages != null) {
                        appendLog("✅ Found ${packages.size} package(s)")
                        packages.take(3).forEach { pkg ->
                            appendLog("   - ${pkg.name} (${pkg.fillAmount} ${pkg.fillUnit})")
                        }
                    }
                } else {
                    appendLog("❌ Packages failed: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Packages error: ${e.message}")
                Log.e("MultiTenantTest", "Packages error", e)
            }
        }
    }

    private fun testLebensmittel() {
        lifecycleScope.launch {
            try {
                appendLog("\n🥫 Testing Lebensmittel API...")

                val response = apiService.getAllLebensmittel()

                if (response.isSuccessful) {
                    val items = response.body()
                    if (items != null) {
                        appendLog("✅ Found ${items.size} lebensmittel item(s)")
                        items.take(3).forEach { item ->
                            appendLog("   - ${item.name} (${item.menge} ${item.einheit})")
                        }
                    }
                } else {
                    appendLog("❌ Lebensmittel failed: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Lebensmittel error: ${e.message}")
                Log.e("MultiTenantTest", "Lebensmittel error", e)
            }
        }
    }

    private fun testCreateLebensmittel() {
        lifecycleScope.launch {
            try {
                appendLog("\n📝 Testing Create Lebensmittel...")

                val newItem = LebensmittelCreate(
                    name = "Android Test Item ${System.currentTimeMillis()}",
                    quantity = 5,
                    einheit = "Stück",
                    kategorie = "Test",
                    ablaufdatum = null,
                    eanCode = null,
                    mindestmenge = 2
                )

                val response = apiService.createLebensmittel(newItem)

                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        appendLog("✅ Created: ${item.name} (ID: ${item.id})")
                    }
                } else {
                    appendLog("❌ Create failed: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                appendLog("❌ Create error: ${e.message}")
                Log.e("MultiTenantTest", "Create error", e)
            }
        }
    }

    private fun testAllApis() {
        lifecycleScope.launch {
            appendLog("🚀 Testing All Multi-Tenant APIs...\n")

            if (sessionManager.fetchAuthToken() == null) {
                testLogin()
                kotlinx.coroutines.delay(2000) // Wait for login
            }

            testHouseholds()
            kotlinx.coroutines.delay(1000)

            testStorageLocations()
            kotlinx.coroutines.delay(1000)

            testPackages()
            kotlinx.coroutines.delay(1000)

            testLebensmittel()
            kotlinx.coroutines.delay(1000)

            testCreateLebensmittel()

            appendLog("\n🎉 All tests completed!")
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            val currentText = binding.tvLog.text.toString()
            binding.tvLog.text = "$currentText\n$message"
            binding.scrollView.post {
                binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }
}
