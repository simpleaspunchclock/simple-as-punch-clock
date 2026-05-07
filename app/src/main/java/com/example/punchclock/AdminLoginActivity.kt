package com.simpleas.punchclock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
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
                val question = binding.editRecoveryQuestion.text.toString().trim()
                val answer = binding.editRecoveryAnswer.text.toString().trim()
                val confirm = binding.editRecoveryAnswerConfirm.text.toString().trim()

                if (pin.length != 4) {
                    UiUtils.toast(this, "Enter a 4-digit admin PIN")
                    return@setOnClickListener
                }

                if (question.isEmpty()) {
                    UiUtils.toast(this, "Enter a recovery question")
                    return@setOnClickListener
                }

                if (answer.isEmpty()) {
                    UiUtils.toast(this, "Enter a one-word recovery answer")
                    return@setOnClickListener
                }

                if (answer.contains(" ")) {
                    UiUtils.toast(this, "Recovery answer must be one word")
                    return@setOnClickListener
                }

                if (
                    SecurityUtils.normalizeRecoveryAnswer(answer) !=
                    SecurityUtils.normalizeRecoveryAnswer(confirm)
                ) {
                    UiUtils.toast(this, "Recovery answers do not match")
                    return@setOnClickListener
                }

                db.setAdminPin(pin)
                db.setAdminRecovery(
                    question = question,
                    answerHash = SecurityUtils.hashRecoveryAnswer(answer)
                )

                UiUtils.toast(this, "Admin PIN created")
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else if (db.verifyAdminPin(pin)) {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else {
                UiUtils.toast(this, "Incorrect admin PIN")
            }
        }

        binding.btnForgotPin.setOnClickListener {
            showRecoveryQuestionDialog()
        }
    }

    private fun refreshMode() {
        if (db.hasAdminPin()) {
            binding.txtTitle.text = "Admin Login"
            binding.txtHelp.text = "Enter your admin PIN to continue."
            binding.editAdminPin.hint = "Admin PIN"
            binding.btnEnter.text = "Enter"

            binding.editRecoveryQuestion.visibility = View.GONE
            binding.editRecoveryAnswer.visibility = View.GONE
            binding.editRecoveryAnswerConfirm.visibility = View.GONE
            binding.btnForgotPin.visibility = View.VISIBLE
        } else {
            binding.txtTitle.text = "Create Admin PIN"
            binding.txtHelp.text =
                "No admin PIN exists yet. Enter a new 4-digit PIN and set a recovery question."
            binding.editAdminPin.hint = "New Admin PIN"
            binding.btnEnter.text = "Create Admin PIN"

            binding.editRecoveryQuestion.visibility = View.VISIBLE
            binding.editRecoveryAnswer.visibility = View.VISIBLE
            binding.editRecoveryAnswerConfirm.visibility = View.VISIBLE
            binding.btnForgotPin.visibility = View.GONE
        }

        binding.editAdminPin.text?.clear()
        binding.editRecoveryQuestion.text?.clear()
        binding.editRecoveryAnswer.text?.clear()
        binding.editRecoveryAnswerConfirm.text?.clear()
    }

    private fun showRecoveryQuestionDialog() {
        val question = db.getAdminRecoveryQuestion()
        val storedHash = db.getAdminRecoveryAnswerHash()

        if (question.isBlank() || storedHash.isBlank()) {
            UiUtils.toast(this, "No recovery question has been set")
            return
        }

        val answerInput = EditText(this).apply {
            hint = "Recovery answer"
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(answerInput)
        }

        AlertDialog.Builder(this)
            .setTitle(question)
            .setView(container)
            .setPositiveButton("Verify") { _, _ ->
                val entered = answerInput.text.toString().trim()

                if (entered.isEmpty()) {
                    UiUtils.toast(this, "Enter your recovery answer")
                    return@setPositiveButton
                }

                val enteredHash = SecurityUtils.hashRecoveryAnswer(entered)
                if (enteredHash == storedHash) {
                    showResetPinDialog()
                } else {
                    UiUtils.toast(this, "Incorrect recovery answer")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetPinDialog() {
        val newPinInput = EditText(this).apply {
            hint = "New 4-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val confirmPinInput = EditText(this).apply {
            hint = "Confirm new PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(newPinInput)
            addView(confirmPinInput)
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Admin PIN")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newPin = newPinInput.text.toString().trim()
                val confirmPin = confirmPinInput.text.toString().trim()

                if (newPin.length != 4) {
                    UiUtils.toast(this, "Enter a 4-digit PIN")
                    return@setPositiveButton
                }

                if (newPin != confirmPin) {
                    UiUtils.toast(this, "PINs do not match")
                    return@setPositiveButton
                }

                db.updateAdminPin(newPin)
                UiUtils.toast(this, "Admin PIN reset")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}