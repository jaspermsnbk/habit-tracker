package com.jaspermsnbk.habit_tracker.data

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test moves by hand. Uses UTC so dates do not depend on the machine's zone. */
class MutableClock(start: LocalDate, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    @Volatile
    private var now: Instant = start.atTime(12, 0).atZone(zone).toInstant()

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(LocalDate.now(this), zone)

    override fun instant(): Instant = now

    fun advanceDays(days: Long) {
        now = now.plus(Duration.ofDays(days))
    }
}
