package com.stemaker.arbeitsbericht.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stemaker.arbeitsbericht.data.client.ClientDao
import com.stemaker.arbeitsbericht.data.client.ClientDb
import com.stemaker.arbeitsbericht.data.report.Converters
import com.stemaker.arbeitsbericht.data.report.ReportDao
import com.stemaker.arbeitsbericht.data.report.ReportDb

@Database(entities = [ReportDb::class, ClientDb::class], version = 3)
@TypeConverters(Converters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun clientDao(): ClientDao
    companion object {
        val migr_2_3 = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // V2 -V3 added ClientDb table and add default values to ReportDb
                database.execSQL("CREATE TABLE IF NOT EXISTS `ClientDb` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `street` TEXT NOT NULL, `zip` TEXT NOT NULL, " +
                        "`city` TEXT NOT NULL, `distance` INTEGER NOT NULL, `useDistance` INTEGER NOT NULL, `driveTime` TEXT NOT NULL, " +
                        "`useDriveTime` INTEGER NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))")
                // V2 - V3 added 4 new default elements that are taken from a client but not directly taken from there
                database.execSQL("ALTER TABLE ReportDb ADD defaultDriveTime TEXT NOT NULL DEFAULT `00:00`")
                database.execSQL("ALTER TABLE ReportDb ADD useDefaultDriveTime INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ReportDb ADD defaultDistance INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ReportDb ADD useDefaultDistance INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE ReportDb ADD clientId INTEGER NOT NULL DEFAULT ${Int.MAX_VALUE}")
            }
        }
    }
}
