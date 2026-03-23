package com.simpleas.punchclock

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityListOnlyBinding

class PunchLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListOnlyBinding
    private lateinit var db: DatabaseHelper
    private var punches: List<Punch> = emptyList()
    private var startMillis = TimeUtils.startOfDay(TimeUtils.now())
    private var endMillis = TimeUtils.endOfDay(TimeUtils.now())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOnlyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)
        binding.titleText.text = "Punch Logs (Today)"
        binding.btnPrimary.text = "Add Correction"
        binding.btnPrimary.setOnClickListener { showAddCorrection() }
        binding.btnBack.setOnClickListener { finish() }
        binding.listView.setOnItemClickListener { _, _, position, _ -> showEditPunch(punches[position]) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        punches = db.getPunches(startMillis, endMillis)
        val items = punches.map {
            val manual = if (it.isManual) " [manual]" else ""
            "${it.employeeName} - ${it.type} - ${TimeUtils.formatDisplay(it.punchTime)}$manual"
        }
        binding.listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun showAddCorrection() {
        val employees = db.getEmployees(includeInactive = false)
        val names = employees.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose employee")
            .setItems(names) { _, which ->
                val employee = employees[which]
                val types = arrayOf(DatabaseHelper.TYPE_IN, DatabaseHelper.TYPE_OUT)
                AlertDialog.Builder(this)
                    .setTitle("Choose punch type")
                    .setItems(types) { _, t ->
                        UiUtils.pickDateTime(this, TimeUtils.now()) { chosen ->
                            val ok = db.addPunch(employee.id, types[t], chosen, true, "Admin correction")
                            UiUtils.toast(this, ok.second)
                            refresh()
                        }
                    }
                    .show()
            }
            .show()
    }

    private fun showEditPunch(punch: Punch) {
        val types = arrayOf(DatabaseHelper.TYPE_IN, DatabaseHelper.TYPE_OUT)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }
       val note = EditText(this).apply {
    hint = "Note"
    inputType = InputType.TYPE_CLASS_TEXT
    setText(punch.note.orEmpty())

    setTextColor(android.graphics.Color.BLACK)
    setHintTextColor(android.graphics.Color.DKGRAY)
}
        container.addView(note)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle("Edit ${punch.employeeName} ${punch.type}")
            .setView(container)
            .setPositiveButton("Pick Time") { _, _ ->
                UiUtils.pickDateTime(this, punch.punchTime) { chosen ->
                    AlertDialog.Builder(this)
                        .setTitle("Select type")
                        .setItems(types) { _, which ->
                            db.updatePunch(punch.id, chosen, types[which], note.text.toString().trim())
                            refresh()
                        }
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
