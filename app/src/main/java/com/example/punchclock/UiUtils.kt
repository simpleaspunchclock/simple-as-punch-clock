package com.simpleas.punchclock

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import java.util.Calendar

object UiUtils {

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun pickDateTime(
        context: Context,
        startMillis: Long,
        onSelected: (Long) -> Unit
    ) {

        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
        }

        DatePickerDialog(
            context,
            android.R.style.Theme_DeviceDefault_Light_Dialog,
            DatePickerDialog.OnDateSetListener { _, y, m, d ->

                TimePickerDialog(
                    context,
                    android.R.style.Theme_DeviceDefault_Light_Dialog,
                    TimePickerDialog.OnTimeSetListener { _, hour, minute ->

                        val chosen = Calendar.getInstance().apply {
                            set(Calendar.YEAR, y)
                            set(Calendar.MONTH, m)
                            set(Calendar.DAY_OF_MONTH, d)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        onSelected(chosen.timeInMillis)

                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()

            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()

    }

}