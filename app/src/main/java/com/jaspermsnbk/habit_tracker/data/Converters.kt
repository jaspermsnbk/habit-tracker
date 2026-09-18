package com.jaspermsnbk.habit_tracker.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Room stores only primitives, so we convert our time types to/from longs.
 * Registered on the database via @TypeConverters.
 */
class Converters {
    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}
