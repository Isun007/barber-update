package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        CapsterEntity::class,
        ProductEntity::class,
        ReservationEntity::class,
        WaterfallStageEntity::class,
        TransactionEntity::class,
        NotificationEntity::class,
        ChatMessageEntity::class,
        ReviewEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun capsterDao(): CapsterDao
    abstract fun productDao(): ProductDao
    abstract fun reservationDao(): ReservationDao
    abstract fun waterfallStageDao(): WaterfallStageDao
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barberteak_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
