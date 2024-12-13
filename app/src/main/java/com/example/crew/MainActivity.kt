package com.example.crew

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var kioskModeRef: DatabaseReference
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    companion object {
        private const val REQUEST_CODE_ADMIN = 1
        private const val TAG = "CrewApp" // Use a constant tag for logging
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Database reference
        kioskModeRef = FirebaseDatabase.getInstance().getReference("kiosk_mode")

        // Initialize DevicePolicyManager and admin component
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Check and request admin permissions if needed
        ensureDeviceAdmin()

        // Monitor Firebase for kiosk mode updates
        monitorScreenPinning()
    }

    private fun ensureDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Admin permissions are required for enabling kiosk mode."
                )
            }
            startActivityForResult(intent, REQUEST_CODE_ADMIN)
        } else {
            Log.d(TAG, "Admin permissions already granted.")
            Toast.makeText(this, "Admin permissions already granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADMIN) {
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                Log.d(TAG, "Admin permissions granted.")
                Toast.makeText(this, "Admin permissions granted.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Admin permissions not granted.")
                Toast.makeText(this, "Admin permissions not granted. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScreenPinning() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                // Set the app as the lock task package
                devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
                startLockTask()
                Toast.makeText(this, "Screen pinning enabled.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "App is not the device owner.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to enable screen pinning: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception while enabling screen pinning: ${e.message}")
        }
    }

    private fun stopScreenPinning() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                // Clear the lock task package
                devicePolicyManager.setLockTaskPackages(adminComponent, arrayOfNulls(0))
                stopLockTask()
                Toast.makeText(this, "Screen pinning disabled.", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "App is not the device owner.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to disable screen pinning: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception while disabling screen pinning: ${e.message}")
        }
    }

    private fun monitorScreenPinning() {
        kioskModeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isScreenPinned = snapshot.getValue(Boolean::class.java)
                Log.d(TAG, "Screen pinning mode: $isScreenPinned")
                when (isScreenPinned) {
                    true -> startScreenPinning()
                    false -> stopScreenPinning()
                    else -> {
                        Log.e(TAG, "Invalid screen pinning mode value.")
                        Toast.makeText(this@MainActivity, "Invalid kiosk mode value.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read kiosk mode: ${error.message}")
                Toast.makeText(this@MainActivity, "Failed to read kiosk mode: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
