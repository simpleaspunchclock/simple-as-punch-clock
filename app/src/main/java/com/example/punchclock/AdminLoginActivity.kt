package com.simpleas.punchclock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityAdminLoginBinding

class AdminLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminLoginBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        binding.btnBack.setOnClickListener { finish() }
        refreshMode()
        binding.btnEnter.setOnClickListener {
            val pin = binding.editAdminPin.text.toString().trim()
            if (!db.hasAdminPin()) {
                if (pin.length == 4) {
                    db.setAdminPin(pin)
                    UiUtils.toast(this, "Admin PIN created")
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                } else {
                    UiUtils.toast(this, "Enter a 4-digit admin PIN")
                }
            } else if (db.verifyAdminPin(pin)) {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else {
                UiUtils.toast(this, "Incorrect admin PIN")
            }
        }
    }

    private fun refreshMode() {
        if (db.hasAdminPin()) {
            binding.txtTitle.text = "Admin Login"
            binding.txtHelp.text = "Enter your admin PIN to continue."
            binding.editAdminPin.hint = "Admin PIN"
            binding.btnEnter.text = "Enter"
        } else {
            binding.txtTitle.text = "Create Admin PIN"
            binding.txtHelp.text = "No admin PIN exists yet. Enter a new 4-digit PIN to create the first admin login."
            binding.editAdminPin.hint = "New Admin PIN"
            binding.btnEnter.text = "Create Admin PIN"
        }
        binding.editAdminPin.text?.clear()
    }
}
