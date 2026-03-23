package com.simpleas.punchclock

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                pin TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE punches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                punch_time INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                is_manual INTEGER NOT NULL DEFAULT 0,
                note TEXT,
                FOREIGN KEY(employee_id) REFERENCES employees(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE settings (
                key_name TEXT PRIMARY KEY,
                value_text TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_time INTEGER NOT NULL,
                message TEXT NOT NULL
            )
            """.trimIndent()
        )
        seedDefaults(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, event_time INTEGER NOT NULL, message TEXT NOT NULL)")
        }
        if (oldVersion < 3) {
            removeLegacyDefaultData(db)
        }
    }

    private fun seedDefaults(db: SQLiteDatabase) {
        addAudit(db, "Database created with no default employees or admin PIN")
    }

    private fun removeLegacyDefaultData(db: SQLiteDatabase) {
        val defaultEmployees = listOf(
            "Sarah" to "1234",
            "Mike" to "2345",
            "Emma" to "3456",
            "Luis" to "4567"
        )

        val employeeCount = android.database.DatabaseUtils.longForQuery(
            db,
            "SELECT COUNT(*) FROM employees",
            null
        )

        val settingsCount = android.database.DatabaseUtils.longForQuery(
            db,
            "SELECT COUNT(*) FROM settings",
            null
        )

        val currentAdminPin = db.rawQuery(
            "SELECT value_text FROM settings WHERE key_name = ?",
            arrayOf(KEY_ADMIN_PIN)
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }

        val hasExactDefaultEmployees = defaultEmployees.all { (name, pin) ->
            db.rawQuery(
                "SELECT COUNT(*) FROM employees WHERE name = ? AND pin = ? AND active = 1",
                arrayOf(name, pin)
            ).use { c -> c.moveToFirst() && c.getLong(0) == 1L }
        }

        val onlyLegacySeedData = employeeCount == 4L && settingsCount <= 1L && currentAdminPin == "9999" && hasExactDefaultEmployees

        if (onlyLegacySeedData) {
            db.delete("punches", null, null)
            db.delete("employees", null, null)
            db.delete("settings", "key_name = ?", arrayOf(KEY_ADMIN_PIN))
            addAudit(db, "Removed legacy default employees and admin PIN during upgrade")
        }
    }

    fun getEmployees(includeInactive: Boolean = true): List<Employee> {
        val result = mutableListOf<Employee>()
        val where = if (includeInactive) null else "active = 1"
        readableDatabase.query("employees", null, where, null, null, null, "name ASC").use { c ->
            while (c.moveToNext()) {
                result.add(
                    Employee(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        pin = c.getString(c.getColumnIndexOrThrow("pin")),
                        active = c.getInt(c.getColumnIndexOrThrow("active")) == 1
                    )
                )
            }
        }
        return result
    }

    fun getEmployee(employeeId: Long): Employee? {
        readableDatabase.query("employees", null, "id = ?", arrayOf(employeeId.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) {
                return Employee(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    pin = c.getString(c.getColumnIndexOrThrow("pin")),
                    active = c.getInt(c.getColumnIndexOrThrow("active")) == 1
                )
            }
        }
        return null
    }

    fun verifyEmployeePin(employeeId: Long, pin: String): Boolean {
        val employee = getEmployee(employeeId) ?: return false
        return employee.pin == pin
    }

    fun hasAdminPin(): Boolean = !getSetting(KEY_ADMIN_PIN).isNullOrBlank()

    fun verifyAdminPin(pin: String): Boolean = getSetting(KEY_ADMIN_PIN) == pin

    fun setAdminPin(pin: String) {
        val hadPin = hasAdminPin()
        setSetting(KEY_ADMIN_PIN, pin)
        addAudit(if (hadPin) "Admin PIN changed" else "Admin PIN created")
    }

    fun upsertEmployee(id: Long?, name: String, pin: String, active: Boolean) {
        val values = ContentValues().apply {
            put("name", name.trim())
            put("pin", pin)
            put("active", if (active) 1 else 0)
        }
        if (id == null) {
            writableDatabase.insert("employees", null, values)
            addAudit("Employee added: $name")
        } else {
            writableDatabase.update("employees", values, "id = ?", arrayOf(id.toString()))
            addAudit("Employee updated: $name")
        }
    }

    fun getLastPunch(employeeId: Long): Punch? {
        val sql = """
            SELECT p.id, p.employee_id, e.name, p.type, p.punch_time, p.created_at, p.is_manual, p.note
            FROM punches p
            JOIN employees e ON e.id = p.employee_id
            WHERE p.employee_id = ?
            ORDER BY p.punch_time DESC, p.id DESC
            LIMIT 1
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(employeeId.toString())).use { c ->
            if (c.moveToFirst()) return readPunch(c)
        }
        return null
    }

    fun getEmployeeStatus(employeeId: Long): String {
        val last = getLastPunch(employeeId) ?: return "OUT"
        return if (last.type == TYPE_IN) "IN since ${TimeUtils.formatShort(last.punchTime)}" else "OUT"
    }

    fun canPerform(employeeId: Long, desiredType: String): Pair<Boolean, String> {
        val last = getLastPunch(employeeId)
        return when {
            last == null && desiredType == TYPE_OUT -> false to "Employee has not punched in yet."
            last == null && desiredType == TYPE_IN -> true to ""
            last?.type == desiredType -> false to "Employee must punch ${if (desiredType == TYPE_IN) "OUT" else "IN"} next."
            else -> true to ""
        }
    }

  fun addPunch(
    employeeId: Long,
    type: String,
    whenMillis: Long,
    isManual: Boolean,
    note: String?
): Pair<Boolean, String> {

    val now = TimeUtils.now()

    // Prevent future punches
    if (whenMillis > now) {
        return false to "Future punches are not allowed."
    }

    val last = getLastPunch(employeeId)

    // Cannot punch OUT before first IN
    if (last == null && type == TYPE_OUT) {
        return false to "Cannot punch OUT before first IN."
    }

    if (last != null) {

        // Must alternate IN / OUT
        if (last.type == type) {
            return false to "Invalid sequence. Employee must alternate IN and OUT."
        }

        // Cannot punch earlier than previous punch
        if (whenMillis < last.punchTime) {
            return false to "Punch time cannot be earlier than the previous punch."
        }
    }

    val values = ContentValues().apply {
        put("employee_id", employeeId)
        put("type", type)
        put("punch_time", whenMillis)
        put("created_at", now)
        put("is_manual", if (isManual) 1 else 0)
        put("note", note)
    }

    writableDatabase.insert("punches", null, values)

    val employee = getEmployee(employeeId)
    val tag = if (isManual) "manual" else "normal"

    addAudit(
        "${employee?.name ?: "Employee $employeeId"} added $type at ${
            TimeUtils.formatDisplay(whenMillis)
        } ($tag)"
    )

    return true to "Saved ${employee?.name ?: "employee"} $type at ${
        TimeUtils.formatDisplay(whenMillis)
    }"
}

    fun updatePunch(punchId: Long, whenMillis: Long, type: String, note: String?) {
        val values = ContentValues().apply {
            put("punch_time", whenMillis)
            put("type", type)
            put("note", note)
        }
        writableDatabase.update("punches", values, "id = ?", arrayOf(punchId.toString()))
        addAudit("Punch $punchId updated to $type at ${TimeUtils.formatDisplay(whenMillis)}")
    }

    fun getPunches(startMillis: Long, endMillis: Long): List<Punch> {
        val result = mutableListOf<Punch>()
        val sql = """
            SELECT p.id, p.employee_id, e.name, p.type, p.punch_time, p.created_at, p.is_manual, p.note
            FROM punches p
            JOIN employees e ON e.id = p.employee_id
            WHERE p.punch_time BETWEEN ? AND ?
            ORDER BY p.punch_time DESC, p.id DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(startMillis.toString(), endMillis.toString())).use { c ->
            while (c.moveToNext()) result.add(readPunch(c))
        }
        return result
    }

    fun getHoursByEmployee(startMillis: Long, endMillis: Long): List<HoursRow> {
        val employees = getEmployees(includeInactive = false)
        val punchesByEmployee = getPunches(startMillis, endMillis).groupBy { it.employeeId }
        val rows = mutableListOf<HoursRow>()
        for (employee in employees) {
            val list = punchesByEmployee[employee.id].orEmpty().sortedBy { it.punchTime }
            var total = 0L
            var openIn: Punch? = null
            var issues = 0
            for (p in list) {
                if (p.type == TYPE_IN) {
                    if (openIn != null) issues++
                    openIn = p
                } else {
                    if (openIn == null) {
                        issues++
                    } else {
                        total += (p.punchTime - openIn.punchTime).coerceAtLeast(0L)
                        openIn = null
                    }
                }
            }
            if (openIn != null) issues++
            rows.add(HoursRow(employee.name, total, issues))
        }
        return rows.sortedBy { it.employeeName }
    }

    fun getAuditLog(limit: Int = 200): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.query("audit_log", null, null, null, null, null, "event_time DESC", limit.toString()).use { c ->
            while (c.moveToNext()) {
                val time = c.getLong(c.getColumnIndexOrThrow("event_time"))
                val message = c.getString(c.getColumnIndexOrThrow("message"))
                result.add("${TimeUtils.formatDisplay(time)} - $message")
            }
        }
        return result
    }

    fun getSetting(key: String): String? {
        readableDatabase.query("settings", arrayOf("value_text"), "key_name = ?", arrayOf(key), null, null, null).use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }

    private fun setSetting(key: String, value: String) {
        val values = ContentValues().apply {
            put("key_name", key)
            put("value_text", value)
        }
        writableDatabase.insertWithOnConflict("settings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun addAudit(message: String) {
        addAudit(writableDatabase, message)
    }

    private fun addAudit(db: SQLiteDatabase, message: String) {
        val values = ContentValues().apply {
            put("event_time", TimeUtils.now())
            put("message", message)
        }
        db.insert("audit_log", null, values)
    }

    private fun readPunch(c: android.database.Cursor): Punch {
        return Punch(
            id = c.getLong(0),
            employeeId = c.getLong(1),
            employeeName = c.getString(2),
            type = c.getString(3),
            punchTime = c.getLong(4),
            createdAt = c.getLong(5),
            isManual = c.getInt(6) == 1,
            note = c.getString(7)
        )
    }

    companion object {
        private const val DB_NAME = "punch_clock.db"
        private const val DB_VERSION = 3
        const val TYPE_IN = "IN"
        const val TYPE_OUT = "OUT"
        const val KEY_ADMIN_PIN = "admin_pin"
        const val MANUAL_WINDOW_MS = 24L * 60L * 60L * 1000L
    }
}
