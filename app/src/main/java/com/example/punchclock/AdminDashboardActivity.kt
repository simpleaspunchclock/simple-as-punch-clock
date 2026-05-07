package com.simpleas.punchclock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityAdminDashboardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var db: DatabaseHelper
    private lateinit var pendingBackupJson: String

    private val createBackupDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(pendingBackupJson.toByteArray())
                    }
                    UiUtils.toast(this, "Backup exported")
                } catch (e: Exception) {
                    UiUtils.toast(this, "Backup export failed")
                }
            } else {
                UiUtils.toast(this, "Export cancelled")
            }
        }

    private val openBackupDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                confirmImportBackup(uri)
            } else {
                UiUtils.toast(this, "Import cancelled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        binding.checkShowCurrentlyIn.isChecked =
            prefs.getBoolean("show_currently_in", false)

        binding.checkShowCurrentlyIn.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_currently_in", isChecked).apply()
        }

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

        binding.btnExportBackup.setOnClickListener {
            exportBackup()
        }

        binding.btnImportBackup.setOnClickListener {
            openBackupDocument.launch(
                arrayOf("application/json", "text/*", "application/octet-stream", "*/*")
            )
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            val url = "https://docs.google.com/document/d/e/2PACX-1vS6BT2nqf-U8eKVQ2lrBcFwdXtb48kMYd4xjIrLex09322DGVPVE9AC7LjeYYHdzGyoBGql-xPXmezK/pub"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.android.chrome")
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    UiUtils.toast(this, "No browser available")
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEnableKiosk.setOnClickListener {
            KioskManager.enabled = true
            UiUtils.toast(this, "Kiosk mode enabled")
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.btnDisableKiosk.setOnClickListener {
            KioskManager.enabled = false

            try {
                stopLockTask()
            } catch (_: Exception) {
            }

            UiUtils.toast(this, "Kiosk mode disabled")
        }
    }

    private fun exportBackup() {
        pendingBackupJson = BackupManager.createBackupJson(db)

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
        val fileName = "simple_as_punch_clock_backup_$timestamp.json"

        createBackupDocument.launch(fileName)
    }

    private fun confirmImportBackup(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Import Backup?")
            .setMessage(
                "Importing a backup will replace all current employees, punches, admin settings, and app settings on this device."
            )
            .setPositiveButton("Import") { _, _ ->
                importBackup(uri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importBackup(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            }

            if (json.isNullOrBlank()) {
                UiUtils.toast(this, "Backup file is empty")
                return
            }

            val backup = BackupManager.parseBackupJson(json)

            if (backup.backupVersion != 1) {
                UiUtils.toast(this, "Unsupported backup version")
                return
            }

            db.restoreBackupData(backup)

            UiUtils.toast(this, "Backup imported")

            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        } catch (e: Exception) {
            UiUtils.toast(this, "Backup import failed")
        }
    }
}