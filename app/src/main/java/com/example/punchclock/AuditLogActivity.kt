package com.simpleas.punchclock

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityListOnlyBinding

class AuditLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListOnlyBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOnlyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)
        binding.titleText.text = "Audit Log"
        binding.btnPrimary.text = "Change Admin PIN"
        binding.btnPrimary.setOnClickListener { showPinDialog() }
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val items = db.getAuditLog()
        binding.listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }

    private fun showPinDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "New 4-digit admin PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Change Admin PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.length == 4) {
                    db.setAdminPin(pin)
                    UiUtils.toast(this, "Admin PIN updated")
                    recreate()
                } else {
                    UiUtils.toast(this, "PIN must be 4 digits")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
