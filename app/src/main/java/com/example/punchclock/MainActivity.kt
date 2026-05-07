package com.simpleas.punchclock

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simpleas.punchclock.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val clockHandler = Handler(Looper.getMainLooper())
    private var showColon = true
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: DatabaseHelper

    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            val base = SimpleDateFormat("h:mm a", Locale.US).format(now)
            val spannable = SpannableString(base)
            val colonIndex = base.indexOf(':')

            if (colonIndex != -1) {
                val color = if (showColon) Color.WHITE else Color.DKGRAY
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

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
        refreshCurrentlyInCard()
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
            }
        }
    }

    private fun refreshCurrentlyInCard() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val showCurrentlyIn = prefs.getBoolean("show_currently_in", false)

        if (!showCurrentlyIn) {
            binding.currentlyInCard.visibility = View.GONE
            return
        }

        val allEmployees = db.getEmployees(includeInactive = false)
        val punchedInEmployees = allEmployees.filter { employee ->
            val lastPunch = db.getLastPunch(employee.id)
            lastPunch != null && lastPunch.type == DatabaseHelper.TYPE_IN
        }

        if (punchedInEmployees.isEmpty()) {
            binding.currentlyInCard.visibility = View.GONE
            binding.currentlyInList.removeAllViews()
            return
        }

        binding.currentlyInList.removeAllViews()

        punchedInEmployees.forEach { employee ->
            val nameView = TextView(this).apply {
                text = employee.name
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(0, 8, 0, 8)
            }
            binding.currentlyInList.addView(nameView)
        }
        binding.currentlyInScroll.post {
            val maxHeightDp = 180
            val scale = resources.displayMetrics.density
            val maxHeightPx = (maxHeightDp * scale).toInt()

            val contentHeight = binding.currentlyInList.height
            val params = binding.currentlyInScroll.layoutParams

            params.height = if (contentHeight > maxHeightPx) {
                maxHeightPx
            } else {
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            }

            binding.currentlyInScroll.layoutParams = params
        }
        binding.currentlyInCard.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_EMPLOYEE_ID = "extra_employee_id"
        const val EXTRA_EMPLOYEE_NAME = "extra_employee_name"
    }
}