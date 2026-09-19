package com.jaspermsnbk.habit_tracker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun migrate1To2_keepsExistingData_andSupportsLabels() = runBlocking {
        createVersion1Database()

        // Opening runs the migrations, and Room then validates the result against the current entities.
        val db = Room.databaseBuilder(context, HabitDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.habitDao()
            val repository = HabitRepository(dao)

            val habit = dao.observeHabits().first().single()
            assertEquals("Read", habit.name)
            assertNull(habit.labelId)
            assertEquals(LocalDate.ofEpochDay(20_000), dao.observeAllEntries().first().single().date)

            assertTrue(repository.addLabel("Learning"))
            val label = repository.labels.first().single()
            dao.upsertHabit(habit.copy(labelId = label.id))
            assertEquals("Learning", repository.habits.first().single().labelName)

            // ON DELETE SET NULL: removing a label unlabels its habits rather than deleting them.
            db.openHelper.writableDatabase.execSQL("DELETE FROM labels")
            assertNull(dao.observeHabits().first().single().labelId)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2To3_keepsExistingData_andSupportsEmoji() = runBlocking {
        createVersion2Database()

        // Opening runs the migration, and Room then validates the result against the v3 entities.
        val db = Room.databaseBuilder(context, HabitDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.habitDao()
            val repository = HabitRepository(dao)

            val habit = dao.observeHabits().first().single()
            assertEquals("Read", habit.name)
            assertNull(habit.emoji)
            assertEquals(LocalDate.ofEpochDay(20_000), dao.observeAllEntries().first().single().date)

            assertTrue(repository.updateHabit(habit.id, habit.name, habit.color, habit.labelId, "📚"))
            assertEquals("📚", repository.habits.first().single().emoji)
        } finally {
            db.close()
        }
    }

    /** Recreates the database exactly as Room version 1 of the app left it, with one habit and entry. */
    private fun createVersion1Database() {
        val file = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            VERSION_1_SCHEMA.forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO habits (id, name, color, archived, createdAt, updatedAt) " +
                    "VALUES ('h1', 'Read', '#2E7D32', 0, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO habit_entries (id, habitId, date, createdAt) VALUES ('e1', 'h1', 20000, 0)"
            )
            db.version = 1
        }
    }

    /** Recreates the database exactly as Room version 2 of the app left it, with one habit and entry. */
    private fun createVersion2Database() {
        val file = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            VERSION_2_SCHEMA.forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO habits (id, name, color, archived, createdAt, updatedAt, labelId) " +
                    "VALUES ('h1', 'Read', '#2E7D32', 0, 0, 0, NULL)"
            )
            db.execSQL(
                "INSERT INTO habit_entries (id, habitId, date, createdAt) VALUES ('e1', 'h1', 20000, 0)"
            )
            db.version = 2
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"

        /** Copied from Room's generated HabitDatabase_Impl for schema version 1. */
        val VERSION_1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL, `archived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `habit_entries` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_entries_habitId_date` ON `habit_entries` (`habitId`, `date`)",
            "CREATE INDEX IF NOT EXISTS `index_habit_entries_habitId` ON `habit_entries` (`habitId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'adef16d81269d09644efd31932296bf4')",
        )

        /**
         * The v1 habits/habit_entries tables plus what [MIGRATION_1_2] adds: a `labels` table and
         * `habits.labelId` (with its index), reproducing schema version 2 exactly.
         */
        val VERSION_2_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `labels` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL, `archived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `labelId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`labelId`) REFERENCES `labels`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_habits_labelId` ON `habits` (`labelId`)",
            "CREATE TABLE IF NOT EXISTS `habit_entries` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_entries_habitId_date` ON `habit_entries` (`habitId`, `date`)",
            "CREATE INDEX IF NOT EXISTS `index_habit_entries_habitId` ON `habit_entries` (`habitId`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b9a1c1e6d4e2fa5b7e3d1c9a2f4e6b8c')",
        )
    }
}
