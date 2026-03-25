package com.simpleas.punchclock

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourReportRow(
    val employeeName: String,
    val startDate: String,
    val endDate: String,
    val totalHours: Double
)

object CsvExporter {

    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)

    fun buildHoursReportCsv(rows: List<HourReportRow>): String {
        return buildString {
            appendLine("Employee,Start Date,End Date,Total Hours")
            for (row in rows) {
                appendLine(
                    "${csvEscape(row.employeeName)},${row.startDate},${row.endDate},${row.totalHours}"
                )
            }
        }
    }

    fun makeExportFileName(startDateLabel: String, endDateLabel: String): String {
        val timestamp = fileNameDateFormat.format(Date())
        val safeStart = startDateLabel.replace("/", "-")
        val safeEnd = endDateLabel.replace("/", "-")
        return "hours_${safeStart}_to_${safeEnd}_$timestamp.csv"
    }

    fun exportHoursReportToInternalFile(
        context: Context,
        rows: List<HourReportRow>,
        startDateLabel: String,
        endDateLabel: String
    ): File {
        val exportsDir = File(context.filesDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }

        val outFile = File(exportsDir, makeExportFileName(startDateLabel, endDateLabel))
        outFile.writeText(buildHoursReportCsv(rows))
        return outFile
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}

object CsvShareHelper {
    fun shareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share CSV")
        )
    }
}