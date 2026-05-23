package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Tenant::class, MonthlyBill::class, AppSettings::class],
    version = 2,
    exportSchema = false
)
abstract class RentDatabase : RoomDatabase() {
    abstract val rentDao: RentDao

    companion object {
        @Volatile
        private var INSTANCE: RentDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RentDatabase::class.java,
                    "rentease_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    database.rentDao.insertSettings(AppSettings())
                }
            }
        }
    }
}
