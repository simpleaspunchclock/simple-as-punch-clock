package com.simpleas.punchclock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityPinEntryBinding

class PinEntryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinEntryBinding
    private lateinit var db: DatabaseHelper
    private var selectedTime = TimeUtils.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        val employeeId = intent.getLongExtra(MainActivity.EXTRA_EMPLOYEE_ID, -1L)
        val employeeName = intent.getStringExtra(MainActivity.EXTRA_EMPLOYEE_NAME).orEmpty()
        val type = intent.getStringExtra(MainActivity.EXTRA_TYPE).orEmpty()
        binding.txtHeader.text = "$employeeName - Punch $type"
        binding.txtManualTime.text = TimeUtils.formatDisplay(selectedTime)
        binding.btnBack.setOnClickListener { finish() }

        val check = db.canPerform(employeeId, type)
        if (!check.first) {
            binding.txtRule.text = check.second
        }

        binding.chkManual.setOnCheckedChangeListener { _, checked ->
            binding.btnPickTime.isEnabled = checked
            if (!checked) {
                selectedTime = TimeUtils.now()
                binding.txtManualTime.text = TimeUtils.formatDisplay(selectedTime)
            }
        }

        binding.btnPickTime.setOnClickListener {
            UiUtils.pickDateTime(this, selectedTime) {
                selectedTime = it
                binding.txtManualTime.text = TimeUtils.formatDisplay(selectedTime)
            }
        }

        binding.btnConfirm.setOnClickListener {
            val pin = binding.editPin.text.toString().trim()
            if (pin.length != 4) {
                UiUtils.toast(this, "Enter a 4-digit PIN")
                return@setOnClickListener
            }
            if (!db.verifyEmployeePin(employeeId, pin)) {
                UiUtils.toast(this, "Incorrect PIN")
                return@setOnClickListener
            }
            val isManual = binding.chkManual.isChecked
            val result = db.addPunch(
                employeeId = employeeId,
                type = type,
                whenMillis = if (isManual) selectedTime else TimeUtils.now(),
                isManual = isManual,
                note = if (isManual) "Employee self-corrected" else null
            )
            if (result.first) {
                UiUtils.toast(this, result.second)
                val home = android.content.Intent(this, MainActivity::class.java)
                home.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(home)
                finish()
            } else {
                binding.txtRule.text = result.second
            }
        }
    }
}
