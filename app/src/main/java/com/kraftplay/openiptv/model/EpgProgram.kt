package com.kraftplay.openiptv.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

// UNIQUE(channelId, startTime) makes INSERT OR REPLACE actually deduplicate:
// the autoGenerate id never conflicts, so without this index every refresh appended
// a full copy of the EPG (DB grew to 300+ MB). startTime/endTime indices remove
// full-table scans and native sorters on ~1M rows (OOM kills on low-RAM devices).
// ponytail: channelId uniqueness is case-sensitive (BINARY collation); normalize at
// insert time if cross-case duplicates ever show up. LOWER(channelId) lookups still
// scan, but they no longer sort the whole table.
@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channelId", "startTime"], unique = true),
        Index("startTime"),
        Index("endTime")
    ]
)
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val title: String,
    val startTime: Long, // Using Long for Room
    val endTime: Long,
    val description: String? = null,
    val category: String? = null,
    val iconUrl: String? = null
) {
    // Helper to get Date objects if needed
    val start: Date get() = Date(startTime)
    val stop: Date get() = Date(endTime)
}
