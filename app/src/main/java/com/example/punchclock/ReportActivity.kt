package com.simpleas.punchclock

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityReportBinding
import java.io.File

class ReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportBinding
    private lateinit var db: DatabaseHelper
    private var startMillis = TimeUtils.startOfDay(TimeUtils.now())
    private var endMillis = TimeUtils.endOfDay(TimeUtils.now())
    private var lastExportedCsvFile: File? = null
    private lateinit var pendingCsvText: String
    private lateinit var pendingCsvFileName: String

    private val createCsvDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(pendingCsvText.toByteArray())
                }
                UiUtils.toast(this, "CSV saved")
            } else {
                UiUtils.toast(this, "Save cancelled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnStart.setOnClickListener {
            UiUtils.pickDateTime(this, startMillis) {
                startMillis = TimeUtils.startOfDay(it)
                renderDates()
                refresh()
            }
        }

        binding.btnEnd.setOnClickListener {
            UiUtils.pickDateTime(this, endMillis) {
                endMillis = TimeUtils.endOfDay(it)
                renderDates()
                refresh()
            }
        }

        binding.btnExportCsv.setOnClickListener {
            exportCsv()
        }

        binding.btnShareCsv.setOnClickListener {
            shareCsv()
        }

        renderDates()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun renderDates() {
        binding.txtDates.text = "${TimeUtils.formatDay(startMillis)} to ${TimeUtils.formatDay(endMillis)}"
    }

    private fun refresh() {
        val rows = db.getHoursByEmployee(startMillis, endMillis)
        val items = rows.map {
            val issues = if (it.issues > 0) " | issues: ${it.issues}" else ""
            "${it.employeeName} - ${TimeUtils.formatDuration(it.totalMillis)}$issues"
        }
        binding.listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun exportCsv() {
        val rows = db.getHoursByEmployee(startMillis, endMillis)

        val csvRows = rows.map {
            HourReportRow(
                employeeName = it.employeeName,
                startDate = TimeUtils.formatDay(startMillis),
                endDate = TimeUtils.formatDay(endMillis),
                totalHours = it.totalMillis / 3600000.0
            )
        }

        pendingCsvText = CsvExporter.buildHoursReportCsv(csvRows)
        pendingCsvFileName = CsvExporter.makeExportFileName(
            startDateLabel = TimeUtils.formatDay(startMillis),
            endDateLabel = TimeUtils.formatDay(endMillis)
        )

        createCsvDocument.launch(pendingCsvFileName)
    }

    private fun shareCsv() {
        val rows = db.getHoursByEmployee(startMillis, endMillis)

        val csvRows = rows.map {
            HourReportRow(
                employeeName = it.employeeName,
                startDate = TimeUtils.formatDay(startMillis),
                endDate = TimeUtils.formatDay(endMillis),
                totalHours = it.totalMillis / 3600000.0
            )
        }

        val file = CsvExporter.exportHoursReportToInternalFile(
            context = this,
            rows = csvRows,
            startDateLabel = TimeUtils.formatDay(startMillis),
            endDateLabel = TimeUtils.formatDay(endMillis)
        )

        lastExportedCsvFile = file
        CsvShareHelper.shareCsv(this, file)
    }
}