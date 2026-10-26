package com.ancata.prima_focus

import androidx.sqlite.db.SupportSQLiteDatabase
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Unit tests verifying database schema migrations in PrimaFocusDatabase.
 */
class DatabaseMigrationTest {

    private fun createRecordingDatabase(executedSql: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase
    }

    @Test
    fun migration5_6_dropsDeadEventsTable() {
        val executedSql = mutableListOf<String>()
        val db = createRecordingDatabase(executedSql)

        PrimaFocusDatabase.MIGRATION_5_6.migrate(db)

        assertEquals(1, executedSql.size)
        assertEquals("DROP TABLE IF EXISTS events", executedSql.first())
    }

    @Test
    fun migration6_7_addsMissedPolicyColumnToTasks() {
        val executedSql = mutableListOf<String>()
        val db = createRecordingDatabase(executedSql)

        PrimaFocusDatabase.MIGRATION_6_7.migrate(db)

        assertEquals(1, executedSql.size)
        assertEquals("ALTER TABLE tasks ADD COLUMN missedPolicy TEXT DEFAULT NULL", executedSql.first())
    }

    @Test
    fun migration4_5_addsTombstoneAndSyncVersionColumns() {
        val executedSql = mutableListOf<String>()
        val db = createRecordingDatabase(executedSql)

        PrimaFocusDatabase.MIGRATION_4_5.migrate(db)

        assertEquals(6, executedSql.size)
        assertTrue(executedSql.contains("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"))
        assertTrue(executedSql.contains("ALTER TABLE tasks ADD COLUMN deletedAt INTEGER DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE tasks ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1"))
        assertTrue(executedSql.contains("ALTER TABLE sessions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"))
        assertTrue(executedSql.contains("ALTER TABLE sessions ADD COLUMN deletedAt INTEGER DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE sessions ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1"))
    }
}
