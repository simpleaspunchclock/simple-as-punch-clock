package com.simpleas.punchclock

data class Employee(
    val id: Long,
    val name: String,
    val pin: String,
    val active: Boolean = true
)

data class Punch(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val type: String,
    val punchTime: Long,
    val createdAt: Long,
    val isManual: Boolean,
    val note: String?
)

data class HoursRow(
    val employeeName: String,
    val totalMillis: Long,
    val issues: Int
)
