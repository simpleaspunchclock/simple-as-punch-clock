package com.simpleas.punchclock

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityListOnlyBinding

class EmployeeManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListOnlyBinding
    private lateinit var db: DatabaseHelper
    private var employees: List<Employee> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOnlyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)
        binding.titleText.text = "Employees"
        binding.btnPrimary.text = "Add Employee"
        binding.btnPrimary.setOnClickListener { showEditor(null) }
        binding.btnBack.setOnClickListener { finish() }
        binding.listView.setOnItemClickListener { _, _, position, _ -> showEditor(employees[position]) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        employees = db.getEmployees(includeInactive = true)
        val items = employees.map { "${it.name} - PIN ${it.pin} - ${if (it.active) "Active" else "Inactive"}" }
        binding.listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun showEditor(employee: Employee?) {
        val view = layoutInflater.inflate(R.layout.dialog_employee_editor, null)
        val name = view.findViewById<EditText>(R.id.editName)
        val pin = view.findViewById<EditText>(R.id.editPin)
        val active = view.findViewById<CheckBox>(R.id.checkActive)

        name.setText(employee?.name.orEmpty())
        pin.setText(employee?.pin.orEmpty())
        active.isChecked = employee?.active ?: true

        AlertDialog.Builder(this)
            .setTitle(if (employee == null) "Add Employee" else "Edit Employee")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val n = name.text.toString().trim()
                val p = pin.text.toString().trim()
                if (n.isNotEmpty() && p.length == 4) {
                    db.upsertEmployee(employee?.id, n, p, active.isChecked)
                    refresh()
                } else {
                    UiUtils.toast(this, "Enter a name and 4-digit PIN")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
