package com.simpleas.punchclock

data class BackupData(
    val backupVersion: Int,
    val employees: List<Employee>,
    val punches: List<Punch>,
    val adminPin: String?,
    val recoveryQuestion: String?,
    val recoveryAnswerHash: String?,
    val showCurrentlyIn: Boolean,
    val kioskEnabled: Boolean
)