package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class BarberRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val capsterDao = db.capsterDao()
    private val productDao = db.productDao()
    private val reservationDao = db.reservationDao()
    private val waterfallStageDao = db.waterfallStageDao()
    private val transactionDao = db.transactionDao()
    private val notificationDao = db.notificationDao()
    private val chatMessageDao = db.chatMessageDao()
    private val reviewDao = db.reviewDao()

    // Seeding trigger
    suspend fun seedDatabaseIfNeeded() {
        try {
            // Check users
            val anyUser = userDao.getUserSync("admin@barberteak.com")
            if (anyUser == null) {
                Log.d("BarberRepository", "Seeding database...")
                
                // Seed Users
                userDao.insertUser(UserEntity("admin@barberteak.com", "Yudha Actaffian (Admin)", "08123456789", "ADMIN", "avatar_admin", "Gold", balance = 1000000.0))
                userDao.insertUser(UserEntity("capster@barberteak.com", "Budi (Capster)", "08122334455", "CAPSTER", "avatar_capster", "Silver", balance = 0.0))
                userDao.insertUser(UserEntity("agus@barberteak.com", "Agus (Capster)", "08122334466", "CAPSTER", "avatar_capster2", "Bronze", balance = 0.0))
                userDao.insertUser(UserEntity("yudhaactaffian007@gmail.com", "Yudha Actaffian (VIP)", "08556677889", "CLIENT", "avatar_vip", "Gold", "A-01", balance = 350000.0))
                userDao.insertUser(UserEntity("client@gmail.com", "Rico Wijaya", "08998877665", "CLIENT", "avatar_client", "Silver", balance = 120000.0))

                // Seed Transactions
                transactionDao.insertTransaction(TransactionEntity(
                    userEmail = "yudhaactaffian007@gmail.com",
                    type = "TOPUP",
                    amount = 500000.0,
                    paymentMethod = "TRANSFER",
                    status = "SUCCESS",
                    referenceNo = "BT-TOP-783291",
                    dateStr = "12 Jul 2026 09:30",
                    description = "Top Up Saldo via Bank Transfer BCA"
                ))
                transactionDao.insertTransaction(TransactionEntity(
                    userEmail = "yudhaactaffian007@gmail.com",
                    type = "PAYMENT",
                    amount = 150000.0,
                    paymentMethod = "SALDO",
                    status = "SUCCESS",
                    referenceNo = "BT-PAY-230981",
                    dateStr = "12 Jul 2026 11:45",
                    description = "Pembayaran Layanan Home Service"
                ))

                // Seed Notifications
                notificationDao.insertNotification(NotificationEntity(
                    userEmail = "yudhaactaffian007@gmail.com",
                    title = "Jadwal Reservasi Mendatang",
                    message = "Mengingatkan Anda bahwa reservasi di Toko bersama Budi (Capster Senior) dijadwalkan besok jam 10:00 WIB.",
                    type = "RESERVATION"
                ))
                notificationDao.insertNotification(NotificationEntity(
                    userEmail = "yudhaactaffian007@gmail.com",
                    title = "Status Pembayaran Sukses",
                    message = "Pembayaran untuk reservasi #101 sebesar Rp 90.000 telah lunas menggunakan Tunai.",
                    type = "PAYMENT"
                ))

                // Seed Capsters
                capsterDao.insertCapsters(listOf(
                    CapsterEntity(1, "Budi (Capster Senior)", "Classic Pompadour, Shaving, Hot Towel Massage", "8 Tahun", 4.9f, 128, "Available", true),
                    CapsterEntity(2, "Agus (Capster Junior)", "Undercut, Fade Cut, Creambath", "3 Tahun", 4.6f, 64, "Available", true),
                    CapsterEntity(3, "Rian (Hair Artist)", "Korean Hair Design, Hair Coloring, Perming", "5 Tahun", 4.8f, 96, "Available", false),
                    CapsterEntity(4, "Dendi (Kids Specialist)", "Kids Haircut, Flat Top, Beard Trim", "4 Tahun", 4.7f, 72, "Busy", true)
                ))

                // Seed Products
                productDao.insertProducts(listOf(
                    ProductEntity(1, "Teak & Clay Pomade Premium", "Hair Pomade", 125000.0, "Pomade berbahan dasar clay alami dengan aroma kayu jati (teakwood) yang maskulin dan mewah. Memberikan hold sangat kuat sepanjang hari dengan kilau natural (matte finish). Sangat mudah dibilas hanya dengan sekali keramas.", 15, 4.9f, "img_hair_pomade"),
                    ProductEntity(2, "Woodland Beard Oil Nourish", "Shaving", 85000.0, "Minyak nutrisi brewok premium dengan perpaduan minyak argan, jojoba, dan aroma esensial pinus woodland. Merangsang pertumbuhan bulu wajah agar lebat, lembut, dan bebas gatal.", 24, 4.7f, ""),
                    ProductEntity(3, "Royal Teak Hair Tonic Active", "Hair Care", 95000.0, "Hair tonic tonik aktif pencegah rambut rontok dan ketombe. Dilengkapi ekstrak ginseng alami dan aroma kayu manis yang menyegarkan dengan cooling sensation mentol dingin di kulit kepala.", 30, 4.8f, ""),
                    ProductEntity(4, "Carbon Premium Styling Comb", "Hair Styling", 35000.0, "Sisir penata carbon anti-statis barber profesional dengan gigi rapat dan jarang. Sangat fleksibel, tahan panas tinggi, sempurna untuk merapikan pompadour atau undercut.", 50, 4.5f, "")
                ))

                // Seed Waterfall Stages
                waterfallStageDao.insertStages(listOf(
                    WaterfallStageEntity(1, "Analisis Kebutuhan", "Mendefinisikan kebutuhan sistem reservasi Barberteak: Home Service, katalog produk, visual modern, multi-role (Admin, Capster, Client), dan email khusus.", 100, "COMPLETED"),
                    WaterfallStageEntity(2, "Desain Sistem", "Perancangan UI mewah bertema Jati Jati & Emas (Luxury Teak), skema database Room, alur navigasi Compose, dan fitur Mobile JKN.", 100, "COMPLETED"),
                    WaterfallStageEntity(3, "Implementasi", "Penulisan kode Kotlin, pembuatan antarmuka responsif Material 3, database Room, penanganan state visual, dan detail order on-demand.", 100, "COMPLETED"),
                    WaterfallStageEntity(4, "Verifikasi", "Pengujian fungsionalitas di emulator, verifikasi validitas navigasi antarmuka, uji coba transaksi, dan integrasi transisi status layanan.", 50, "IN_PROGRESS"),
                    WaterfallStageEntity(5, "Pemeliharaan", "Pemeliharaan basis data lokal, optimasi sinkronisasi pesanan antar-peran, dan penyesuaian katalog berkala.", 0, "PENDING")
                ))

                // Seed mock reservations
                reservationDao.insertReservation(ReservationEntity(
                    id = 101,
                    userEmail = "yudhaactaffian007@gmail.com",
                    userName = "Yudha Actaffian (VIP)",
                    userPhone = "08556677889",
                    capsterId = 1,
                    capsterName = "Budi (Capster Senior)",
                    serviceName = "Premium Cut + Wash + Tonic",
                    servicePrice = 90000.0,
                    serviceType = "STORE",
                    date = "Selasa, 7 Jul 2026",
                    time = "10:00 WIB",
                    status = "COMPLETED",
                    queueNo = "A-01"
                ))
                reservationDao.insertReservation(ReservationEntity(
                    id = 102,
                    userEmail = "yudhaactaffian007@gmail.com",
                    userName = "Yudha Actaffian (VIP)",
                    userPhone = "08556677889",
                    capsterId = 2,
                    capsterName = "Agus (Capster Junior)",
                    serviceName = "Exclusive Home Service Haircut",
                    servicePrice = 150000.0,
                    serviceType = "HOME",
                    homeAddress = "Jl. Jati Asri No. 12, Jakarta",
                    date = "Rabu, 8 Jul 2026",
                    time = "14:00 WIB",
                    status = "APPROVED",
                    queueNo = "H-01"
                ))
                reservationDao.insertReservation(ReservationEntity(
                    id = 103,
                    userEmail = "client@gmail.com",
                    userName = "Rico Wijaya",
                    userPhone = "08998877665",
                    capsterId = 1,
                    capsterName = "Budi (Capster Senior)",
                    serviceName = "Classic Haircut & Hot Shave",
                    servicePrice = 75000.0,
                    serviceType = "STORE",
                    date = "Selasa, 7 Jul 2026",
                    time = "11:30 WIB",
                    status = "IN_PROGRESS",
                    queueNo = "A-02"
                ))
                
                Log.d("BarberRepository", "Database seeded successfully.")
            }
        } catch (e: Exception) {
            Log.e("BarberRepository", "Error seeding database: ${e.message}")
        }
    }

    // User Operations
    fun getUser(email: String): Flow<UserEntity?> = userDao.getUser(email)
    suspend fun getUserSync(email: String): UserEntity? = userDao.getUserSync(email)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    // Capster Operations
    fun getAllCapsters(): Flow<List<CapsterEntity>> = capsterDao.getAllCapsters()
    suspend fun getCapsterById(id: Int): CapsterEntity? = capsterDao.getCapsterById(id)
    suspend fun insertCapster(capster: CapsterEntity) = capsterDao.insertCapster(capster)
    suspend fun updateCapsterStatus(id: Int, status: String) = capsterDao.updateCapsterStatus(id, status)

    // Product Operations
    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()
    suspend fun insertProduct(product: ProductEntity) = productDao.insertProduct(product)

    // Reservation Operations
    fun getAllReservations(): Flow<List<ReservationEntity>> = reservationDao.getAllReservations()
    fun getReservationsForUser(email: String): Flow<List<ReservationEntity>> = reservationDao.getReservationsForUser(email)
    fun getReservationsForCapster(capsterName: String): Flow<List<ReservationEntity>> = reservationDao.getReservationsForCapster("%$capsterName%")
    
    suspend fun insertReservation(reservation: ReservationEntity): Long {
        return reservationDao.insertReservation(reservation)
    }
    
    suspend fun updateReservationStatus(id: Int, status: String) = reservationDao.updateReservationStatus(id, status)
    suspend fun updateReservationQueue(id: Int, queueNo: String) = reservationDao.updateReservationQueue(id, queueNo)

    // Waterfall Stage Operations
    fun getWaterfallStages(): Flow<List<WaterfallStageEntity>> = waterfallStageDao.getStages()
    suspend fun updateWaterfallStage(id: Int, status: String, percentage: Int) = waterfallStageDao.updateStageStatus(id, status, percentage)

    // User Balance Operations
    suspend fun updateUserBalance(email: String, balance: Double) = userDao.updateUserBalance(email, balance)

    // Transaction Operations
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    fun getTransactionsForUser(email: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsForUser(email)
    suspend fun insertTransaction(transaction: TransactionEntity): Long = transactionDao.insertTransaction(transaction)

    // Notification Operations
    fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    fun getNotificationsForUser(email: String): Flow<List<NotificationEntity>> = notificationDao.getNotificationsForUser(email)
    suspend fun insertNotification(notification: NotificationEntity): Long = notificationDao.insertNotification(notification)
    suspend fun markAllNotificationsAsRead(email: String) = notificationDao.markAllAsRead(email)

    // Chat Operations
    fun getMessagesForReservation(reservationId: Int): Flow<List<ChatMessageEntity>> = chatMessageDao.getMessagesForReservation(reservationId)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long = chatMessageDao.insertMessage(message)

    // User Points Operations
    suspend fun updateUserPoints(email: String, points: Int) = userDao.updateUserPoints(email, points)

    // Reservation Review Operations
    suspend fun updateReservationReviewed(id: Int, isReviewed: Boolean) = reservationDao.updateReservationReviewed(id, isReviewed)

    // Review Operations
    fun getReviewsForCapster(capsterId: Int): Flow<List<ReviewEntity>> = reviewDao.getReviewsForCapster(capsterId)
    fun getAllReviews(): Flow<List<ReviewEntity>> = reviewDao.getAllReviews()
    suspend fun insertReview(review: ReviewEntity) = reviewDao.insertReview(review)
}
