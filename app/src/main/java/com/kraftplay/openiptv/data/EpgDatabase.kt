package com.kraftplay.openiptv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kraftplay.openiptv.model.EpgProgram

@Database(entities = [EpgProgram::class], version = 2, exportSchema = false)
abstract class EpgDatabase : RoomDatabase() {
    abstract fun epgDao(): EpgDao

    /** Trims the WAL without a full VACUUM; safe to call between bulk-insert batches. */
    fun checkpointWal() {
        runCatching { openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close() }
    }

    /** Trims the WAL and reclaims pages after pruning the rolling EPG window. */
    fun compactStorage() {
        checkpointWal()
        runCatching { openHelper.writableDatabase.execSQL("VACUUM") }
    }

    companion object {
        @Volatile
        private var INSTANCE: EpgDatabase? = null

        fun getDatabase(context: Context): EpgDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EpgDatabase::class.java,
                    "epg_database"
                )
                    // EPG is a re-downloadable cache: on schema change wipe and rebuild
                    // instead of migrating (v2 adds dedup + query indices).
                    .fallbackToDestructiveMigration()
                    // Rollback journal (TRUNCATE) instead of WAL: a live Room reader blocks
                    // WAL checkpoints, so epg_database-wal grew unbounded (4.4GB) and filled
                    // the disk during import. TRUNCATE journal is cleaned after each commit.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
