package com.simpleas.punchclock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Existing navigation buttons
        binding.btnEmployees.setOnClickListener {
            startActivity(Intent(this, EmployeeManagerActivity::class.java))
        }

        binding.btnLogs.setOnClickListener {
            startActivity(Intent(this, PunchLogActivity::class.java))
        }

        binding.btnReports.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.btnAudit.setOnClickListener {
            startActivity(Intent(this, AuditLogActivity::class.java))
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Enable kiosk mode
        binding.btnEnableKiosk.setOnClickListener {

            KioskManager.enabled = true

            UiUtils.toast(this, "Kiosk mode enabled")

            // Restart the app at the home screen so lock task activates
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Disable kiosk mode
        binding.btnDisableKiosk.setOnClickListener {

            KioskManager.enabled = false

            try {
                stopLockTask()
            } catch (_: Exception) {
            }

            UiUtils.toast(this, "Kiosk mode disabled")
        }
    }
}