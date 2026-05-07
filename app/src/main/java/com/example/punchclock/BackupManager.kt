package com.simpleas.punchclock

import com.google.gson.Gson

object BackupManager {

    private val gson = Gson()

    fun createBackupJson(db: DatabaseHelper): String {
        return gson.toJson(db.buildBackupData())
    }

    fun parseBackupJson(json: String): BackupData {
        return gson.fromJson(json, BackupData::class.java)
    }
}