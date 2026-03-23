package com.simpleas.punchclock

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityEmployeeSelectBinding

class EmployeeSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmployeeSelectBinding
    private lateinit var db: DatabaseHelper
    private var employees: List<Employee> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)
        binding.actionTitle.text = "${intent.getStringExtra(MainActivity.EXTRA_TYPE)} - Select Employee"
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()

        val type = intent.getStringExtra(MainActivity.EXTRA_TYPE).orEmpty()
        val allEmployees = db.getEmployees(includeInactive = false)

        employees = allEmployees.filter { employee ->
            val lastPunch = db.getLastPunch(employee.id)
            when (type) {
                DatabaseHelper.TYPE_IN -> lastPunch == null || lastPunch.type == DatabaseHelper.TYPE_OUT
                DatabaseHelper.TYPE_OUT -> lastPunch != null && lastPunch.type == DatabaseHelper.TYPE_IN
                else -> true
            }
        }

        val items = employees.map { "${it.name}  (${db.getEmployeeStatus(it.id)})" }
        binding.listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)

        val hasEligibleEmployees = employees.isNotEmpty()
        binding.emptyText.text = when {
            hasEligibleEmployees -> ""
            allEmployees.isEmpty() -> "No employees have been created yet. Ask an admin to add the first employee."
            type == DatabaseHelper.TYPE_IN -> "No employees are currently available to punch IN."
            type == DatabaseHelper.TYPE_OUT -> "No employees are currently punched IN."
            else -> "No employees available."
        }
        binding.emptyText.visibility = if (hasEligibleEmployees) android.view.View.GONE else android.view.View.VISIBLE

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            val employee = employees[position]
            startActivity(
                Intent(this, PinEntryActivity::class.java)
                    .putExtra(MainActivity.EXTRA_TYPE, type)
                    .putExtra(MainActivity.EXTRA_EMPLOYEE_ID, employee.id)
                    .putExtra(MainActivity.EXTRA_EMPLOYEE_NAME, employee.name)
            )
        }
    }
}
