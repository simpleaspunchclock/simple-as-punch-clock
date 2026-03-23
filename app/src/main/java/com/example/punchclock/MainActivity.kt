package com.simpleas.punchclock
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityMainBinding
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.graphics.Color

class MainActivity : AppCompatActivity() {

    private val clockHandler = Handler(Looper.getMainLooper())

    private var showColon = true

    private val clockRunnable = object : Runnable {
        override fun run() {

            val now = Date()

            val base = SimpleDateFormat("h:mm a", Locale.US).format(now)

            // Toggle colon visibility
            val spannable = SpannableString(base)

            val colonIndex = base.indexOf(':')

            if (colonIndex != -1) {
                val color = if (showColon) {
                    Color.WHITE
                } else {
                    Color.DKGRAY
                }

                spannable.setSpan(
                    ForegroundColorSpan(color),
                    colonIndex,
                    colonIndex + 1,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.txtClock.text = spannable

            showColon = !showColon

            clockHandler.postDelayed(this, 1000)
        }
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIn.setOnClickListener { openEmployees(DatabaseHelper.TYPE_IN) }
        binding.btnOut.setOnClickListener { openEmployees(DatabaseHelper.TYPE_OUT) }
        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
    }

override fun onResume() {
    super.onResume()
    applyKioskModeIfNeeded()
    clockHandler.post(clockRunnable)
}

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun openEmployees(type: String) {
        startActivity(Intent(this, EmployeeSelectActivity::class.java).putExtra(EXTRA_TYPE, type))
    }

private fun applyKioskModeIfNeeded() {
    if (KioskManager.enabled) {
        try {
            startLockTask()
        } catch (_: Exception) {
            // On unmanaged devices this may fall back to screen pinning
        }
    }
}

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_EMPLOYEE_ID = "extra_employee_id"
        const val EXTRA_EMPLOYEE_NAME = "extra_employee_name"
    }
}
