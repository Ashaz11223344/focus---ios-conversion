package com.focus.data.local

import app.cash.sqldelight.db.SqlDriver
import com.focus.database.FocusDatabase

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): FocusDatabase {
    val driver = driverFactory.createDriver()
    return FocusDatabase(driver)
}
