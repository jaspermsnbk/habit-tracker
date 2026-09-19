package com.jaspermsnbk.habit_tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitEntryEntity::class, LabelEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var instance: HabitDatabase? = null

        /** Process-wide singleton — building Room more than once is wasteful and can lock the file. */
        fun get(context: Context): HabitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habits.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}

/** v2 adds labels: a `labels` table and a nullable `habits.labelId` referencing it. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `labels` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        // SQLite allows adding a REFERENCES column in place as long as it defaults to NULL.
        db.execSQL(
            "ALTER TABLE `habits` ADD COLUMN `labelId` TEXT " +
                "REFERENCES `labels`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_labelId` ON `habits` (`labelId`)")
    }
}

/** v3 adds a nullable `habits.emoji` shown on the habit card instead of the color dot. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `habits` ADD COLUMN `emoji` TEXT")
    }
}
