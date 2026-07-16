package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUser(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserSync(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET balance = :balance WHERE email = :email")
    suspend fun updateUserBalance(email: String, balance: Double)

    @Query("UPDATE users SET points = :points WHERE email = :email")
    suspend fun updateUserPoints(email: String, points: Int)
}

@Dao
interface CapsterDao {
    @Query("SELECT * FROM capsters")
    fun getAllCapsters(): Flow<List<CapsterEntity>>

    @Query("SELECT * FROM capsters WHERE id = :id LIMIT 1")
    suspend fun getCapsterById(id: Int): CapsterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapster(capster: CapsterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapsters(capsters: List<CapsterEntity>)

    @Query("UPDATE capsters SET status = :status WHERE id = :id")
    suspend fun updateCapsterStatus(id: Int, status: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)
}

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY createdAt DESC")
    fun getAllReservations(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getReservationsForUser(email: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE capsterName LIKE :capsterName ORDER BY createdAt DESC")
    fun getReservationsForCapster(capsterName: String): Flow<List<ReservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: ReservationEntity): Long

    @Query("UPDATE reservations SET status = :status WHERE id = :id")
    suspend fun updateReservationStatus(id: Int, status: String)

    @Query("UPDATE reservations SET queueNo = :queueNo WHERE id = :id")
    suspend fun updateReservationQueue(id: Int, queueNo: String)

    @Query("UPDATE reservations SET isReviewed = :isReviewed WHERE id = :id")
    suspend fun updateReservationReviewed(id: Int, isReviewed: Boolean)
}

@Dao
interface WaterfallStageDao {
    @Query("SELECT * FROM waterfall_stages ORDER BY stageId ASC")
    fun getStages(): Flow<List<WaterfallStageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<WaterfallStageEntity>)

    @Query("UPDATE waterfall_stages SET status = :status, percentage = :percentage WHERE stageId = :stageId")
    suspend fun updateStageStatus(stageId: Int, status: String, percentage: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getTransactionsForUser(email: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getNotificationsForUser(email: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userEmail = :email")
    suspend fun markAllAsRead(email: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE reservationId = :reservationId ORDER BY timestamp ASC")
    fun getMessagesForReservation(reservationId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE capsterId = :capsterId ORDER BY createdAt DESC")
    fun getReviewsForCapster(capsterId: Int): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}
