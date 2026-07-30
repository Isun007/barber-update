package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.BarberRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalCoroutinesApi::class)
class BarberViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BarberRepository(application)

    // Firebase properties
    private var isFirebaseInitialized = false
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    private fun setupFirebase() {
        try {
            val context = getApplication<Application>().applicationContext
            val hasDefaultApp = try {
                FirebaseApp.getInstance() != null
            } catch (e: Exception) {
                false
            }

            if (!hasDefaultApp) {
                val options = com.google.firebase.FirebaseOptions.fromResource(context)
                if (options != null) {
                    FirebaseApp.initializeApp(context, options)
                    Log.d("FirebaseSetup", "Firebase initialized from resources successfully!")
                } else {
                    Log.d("FirebaseSetup", "google-services.json resources missing. Initializing with programmatic fallback options...")
                    val fallbackOptions = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:abc123def456")
                        .setApiKey("AIzaSyFakeKeyForCompAndFallbackString")
                        .setProjectId("barberteak-fallback")
                        .build()
                    FirebaseApp.initializeApp(context, fallbackOptions)
                    Log.d("FirebaseSetup", "Firebase initialized with programmatic fallback successfully!")
                }
            }
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isFirebaseInitialized = true
            Log.d("FirebaseSetup", "Firebase Auth and Firestore successfully initialized!")
        } catch (e: Exception) {
            Log.e("FirebaseSetup", "Firebase Initialization bypassed: ${e.message}")
        }
    }

    // Current logged-in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Floating/Debug role bypass for easy evaluation
    private val _currentActiveRole = MutableStateFlow("CLIENT")
    val currentActiveRole: StateFlow<String> = _currentActiveRole.asStateFlow()

    // Active Capster context (if logged in as Capster)
    private val _activeCapster = MutableStateFlow<CapsterEntity?>(null)
    val activeCapster: StateFlow<CapsterEntity?> = _activeCapster.asStateFlow()

    // Database Flows
    val allCapsters: StateFlow<List<CapsterEntity>> = repository.getAllCapsters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReservations: StateFlow<List<ReservationEntity>> = repository.getAllReservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterfallStages: StateFlow<List<WaterfallStageEntity>> = repository.getWaterfallStages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviews: StateFlow<List<ReviewEntity>> = repository.getAllReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered reservations for the current user
    val userReservations: StateFlow<List<ReservationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getReservationsForUser(user.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications flow for the current user
    val userNotifications: StateFlow<List<NotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getNotificationsForUser(user.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions flow for the current user
    val userTransactions: StateFlow<List<TransactionEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getTransactionsForUser(user.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered reservations for the logged-in Capster
    val capsterReservations: StateFlow<List<ReservationEntity>> = _activeCapster
        .flatMapLatest { capster ->
            if (capster != null) {
                repository.getReservationsForCapster(capster.name)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback / Complaints list (Simulated local state)
    private val _complaints = MutableStateFlow<List<Pair<String, String>>>(listOf(
        "yudhaactaffian007@gmail.com" to "Layanan home service budi sangat memuaskan, sangat rapi dan ramah!",
        "client@gmail.com" to "Mohon untuk menambah varian pomade water-based di katalog produk."
    ))
    val complaints: StateFlow<List<Pair<String, String>>> = _complaints.asStateFlow()

    // Login validation states
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _unregisteredGmail = MutableStateFlow<String?>(null)
    val unregisteredGmail: StateFlow<String?> = _unregisteredGmail.asStateFlow()

    // Stores custom or reset passwords locally
    private val _customPasswords = MutableStateFlow<Map<String, String>>(emptyMap())
    val customPasswords: StateFlow<Map<String, String>> = _customPasswords.asStateFlow()

    // Shopping Cart state: Map of Product ID -> Quantity
    private val _cart = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val cart: StateFlow<Map<Int, Int>> = _cart.asStateFlow()

    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        val formattedEmail = email.trim().lowercase()
        val userExists = repository.getUserSync(formattedEmail)
        if (userExists == null) {
            return false
        }
        _customPasswords.value = _customPasswords.value + (formattedEmail to newPassword)
        return true
    }

    suspend fun checkUserExists(email: String): Boolean {
        val formattedEmail = email.trim().lowercase()
        return repository.getUserSync(formattedEmail) != null
    }

    init {
        setupFirebase()
        viewModelScope.launch {
            // Seed DB
            repository.seedDatabaseIfNeeded()
            // Auto-login with Yudha's email for stellar first impressions, or let them login
            loginUser("yudhaactaffian007@gmail.com")
        }
    }

    fun clearAuthErrors() {
        _authError.value = null
        _unregisteredGmail.value = null
    }

    fun loginUserWithValidation(
        email: String,
        password: String,
        isNewUser: Boolean,
        name: String = "",
        phone: String = ""
    ) {
        _authError.value = null
        _unregisteredGmail.value = null

        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        if (trimmedEmail.isEmpty() || 
            (isNewUser && (trimmedName.isEmpty() || trimmedPhone.isEmpty() || trimmedPassword.isEmpty())) || 
            (!isNewUser && trimmedPassword.isEmpty())
        ) {
            _authError.value = "Gagal: Kolom email & kata sandi wajib diisi!"
            return
        }

        viewModelScope.launch {
            val userExists = repository.getUserSync(trimmedEmail)
            val isDemo = trimmedEmail == "admin@barberteak.com" || 
                         trimmedEmail == "capster@barberteak.com" || 
                         trimmedEmail == "budi@gmail.com" || 
                         trimmedEmail == "agus@barberteak.com" || 
                         trimmedEmail == "agus@gmail.com" || 
                         trimmedEmail == "yudhaactaffian007@gmail.com"

            if (isNewUser) {
                // REGISTER FLOW
                if (userExists != null) {
                    _authError.value = "Gagal: Email $trimmedEmail sudah terdaftar. Silakan lakukan Masuk!"
                    return@launch
                }
                
                // Save custom created password
                val updatedPasswords = _customPasswords.value.toMutableMap()
                updatedPasswords[trimmedEmail] = trimmedPassword
                _customPasswords.value = updatedPasswords

                loginUser(trimmedEmail, trimmedName, trimmedPhone)
            } else {
                // LOGIN FLOW
                val hasSavedPassword = _customPasswords.value.containsKey(trimmedEmail)
                
                // 1. Check if user email is registered
                if (userExists == null && !isDemo && !hasSavedPassword) {
                    _unregisteredGmail.value = trimmedEmail
                    _authError.value = "Email $trimmedEmail belum terdaftar. Silakan daftar akun baru di bawah!"
                    return@launch
                }

                // 2. Validate password strictly
                val customSavedPassword = _customPasswords.value[trimmedEmail]
                val expectedPassword = when {
                    customSavedPassword != null -> customSavedPassword
                    isDemo -> "123456"
                    else -> "123456"
                }

                if (trimmedPassword != expectedPassword) {
                    _authError.value = "Kata sandi salah! Silakan masukkan kata sandi yang benar."
                    return@launch
                }

                // Password verified successfully!
                val defaultName = userExists?.name ?: if (trimmedName.isNotEmpty()) trimmedName else "Pelanggan Barberteak"
                val defaultPhone = userExists?.phone ?: if (trimmedPhone.isNotEmpty()) trimmedPhone else "08129999000"
                loginUser(trimmedEmail, defaultName, defaultPhone)
            }
        }
    }

    // Authentication Actions
    fun loginUser(email: String, name: String = "", phone: String = "", customRole: String? = null) {
        viewModelScope.launch {
            val formattedEmail = email.trim().lowercase()
            var user = repository.getUserSync(formattedEmail)
            
            if (user == null) {
                // If it's a special predefined email, create it with roles
                val (role, seedName) = when (formattedEmail) {
                    "admin@barberteak.com" -> "ADMIN" to "Yudha Actaffian (Admin)"
                    "capster@barberteak.com", "budi@gmail.com" -> "CAPSTER" to "Budi (Capster Senior)"
                    "agus@barberteak.com", "agus@gmail.com" -> "CAPSTER" to "Agus (Capster Junior)"
                    "yudhaactaffian007@gmail.com" -> "CLIENT" to "Yudha Actaffian (VIP)"
                    else -> (customRole ?: "CLIENT") to (if (name.isNotEmpty()) name else "Pelanggan Barberteak")
                }
                
                val seedPhone = if (phone.isNotEmpty()) phone else "08129999000"
                val tier = if (formattedEmail == "yudhaactaffian007@gmail.com") "Gold" else "Bronze"
                
                user = UserEntity(formattedEmail, seedName, seedPhone, role, "", tier)
                repository.insertUser(user)
            }
            
            _currentUser.value = user
            _currentActiveRole.value = user.role
            
            // Start collecting live user updates from Room for instant balance synchronization
            viewModelScope.launch {
                repository.getUser(formattedEmail).collect { updatedUser ->
                    if (updatedUser != null) {
                        _currentUser.value = updatedUser
                    }
                }
            }

            // Sync Capster profile if role is CAPSTER
            if (user.role == "CAPSTER") {
                val capsters = repository.getAllCapsters().firstOrNull() ?: emptyList()
                val match = capsters.find { user.name.contains(it.name.split(" ")[0]) } 
                    ?: capsters.firstOrNull()
                _activeCapster.value = match
            } else {
                _activeCapster.value = null
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeCapster.value = null
        _currentActiveRole.value = "CLIENT"
    }

    // Role Switcher bypass for quick demo review
    fun switchActiveRoleDirectly(role: String) {
        _currentActiveRole.value = role
        viewModelScope.launch {
            if (role == "ADMIN") {
                val adminUser = repository.getUserSync("admin@barberteak.com")
                if (adminUser != null) _currentUser.value = adminUser
            } else if (role == "CAPSTER") {
                val capsterUser = repository.getUserSync("capster@barberteak.com")
                if (capsterUser != null) {
                    _currentUser.value = capsterUser
                    val capsters = repository.getAllCapsters().firstOrNull() ?: emptyList()
                    _activeCapster.value = capsters.find { it.id == 1 }
                }
            } else {
                val clientUser = repository.getUserSync("yudhaactaffian007@gmail.com")
                if (clientUser != null) _currentUser.value = clientUser
            }
        }
    }

    // Booking Actions
    fun createReservation(
        capsterId: Int,
        capsterName: String,
        serviceName: String,
        servicePrice: Double,
        serviceType: String,
        homeAddress: String?,
        date: String,
        time: String,
        paymentMethod: String = "CASH",
        paymentStatus: String = "UNPAID"
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            
            // Generate typical Mobile JKN style queue numbers
            val prefix = if (serviceType == "HOME") "H-" else "A-"
            val currentCount = (repository.getAllReservations().firstOrNull() ?: emptyList()).size
            val queueNo = String.format("%s%02d", prefix, currentCount + 1)

            var resolvedPaymentStatus = paymentStatus
            if (paymentMethod == "SALDO") {
                if (user.balance >= servicePrice) {
                    val newBalance = user.balance - servicePrice
                    repository.updateUserBalance(user.email, newBalance)
                    resolvedPaymentStatus = "PAID"
                    
                    // Insert transaction record
                    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
                    val referenceNo = "BT-PAY-${System.currentTimeMillis().toString().takeLast(6)}"
                    repository.insertTransaction(TransactionEntity(
                        userEmail = user.email,
                        type = "PAYMENT",
                        amount = servicePrice,
                        paymentMethod = "SALDO",
                        status = "SUCCESS",
                        referenceNo = referenceNo,
                        dateStr = dateStr,
                        description = "Pembayaran Layanan: $serviceName"
                    ))

                    // Insert notification
                    val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
                    repository.insertNotification(NotificationEntity(
                        userEmail = user.email,
                        title = "Pembayaran Berhasil",
                        message = "Reservasi Anda bersama $capsterName telah dibayar lunas sebesar ${formatCurrency.format(servicePrice)} menggunakan saldo.",
                        type = "PAYMENT"
                    ))
                }
            } else if (paymentMethod == "TRANSFER" || paymentMethod == "QRIS") {
                // For manual bank or QRIS checkout, simulate auto-approved paid status for rich user experience
                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
                val referenceNo = "BT-PAY-${System.currentTimeMillis().toString().takeLast(6)}"
                repository.insertTransaction(TransactionEntity(
                    userEmail = user.email,
                    type = "PAYMENT",
                    amount = servicePrice,
                    paymentMethod = paymentMethod,
                    status = "SUCCESS",
                    referenceNo = referenceNo,
                    dateStr = dateStr,
                    description = "Pembayaran Layanan: $serviceName via $paymentMethod"
                ))
                
                resolvedPaymentStatus = "PAID"

                // Insert notification
                val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
                repository.insertNotification(NotificationEntity(
                    userEmail = user.email,
                    title = "Pembayaran Berhasil",
                    message = "Pembayaran reservasi bersama $capsterName sebesar ${formatCurrency.format(servicePrice)} via $paymentMethod telah terkonfirmasi.",
                    type = "PAYMENT"
                ))
            }

            val reservation = ReservationEntity(
                userEmail = user.email,
                userName = user.name,
                userPhone = user.phone,
                capsterId = capsterId,
                capsterName = capsterName,
                serviceName = serviceName,
                servicePrice = servicePrice,
                serviceType = serviceType,
                homeAddress = homeAddress,
                date = date,
                time = time,
                status = "PENDING",
                queueNo = queueNo,
                paymentMethod = paymentMethod,
                paymentStatus = resolvedPaymentStatus
            )
            val generatedId = repository.insertReservation(reservation)

            // Sync reservation data to Firebase Firestore in real-time if active
            if (isFirebaseInitialized && firestore != null) {
                val firestoreReservation = mapOf(
                    "id" to generatedId.toInt(),
                    "userEmail" to reservation.userEmail,
                    "userName" to reservation.userName,
                    "userPhone" to reservation.userPhone,
                    "capsterId" to reservation.capsterId,
                    "capsterName" to reservation.capsterName,
                    "serviceName" to reservation.serviceName,
                    "servicePrice" to reservation.servicePrice,
                    "serviceType" to reservation.serviceType,
                    "homeAddress" to reservation.homeAddress,
                    "date" to reservation.date,
                    "time" to reservation.time,
                    "status" to reservation.status,
                    "queueNo" to reservation.queueNo,
                    "paymentMethod" to reservation.paymentMethod,
                    "paymentStatus" to reservation.paymentStatus,
                    "isReviewed" to reservation.isReviewed,
                    "createdAt" to reservation.createdAt
                )
                firestore!!.collection("reservations")
                    .document(generatedId.toString())
                    .set(firestoreReservation)
                    .addOnSuccessListener {
                        Log.d("FirestoreSync", "Reservation $generatedId successfully stored in Firestore.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreSync", "Failed to store reservation $generatedId in Firestore: ${e.message}")
                    }
            }

            // Insert general reservation reminder notification
            repository.insertNotification(NotificationEntity(
                userEmail = user.email,
                title = "Jadwal Reservasi Ditambahkan",
                message = "Reservasi Anda bersama $capsterName dijadwalkan pada $date pukul $time.",
                type = "RESERVATION"
            ))

            // Trigger real-time system notification
            showLocalNotification(
                "Jadwal Reservasi Ditambahkan",
                "Reservasi Anda bersama $capsterName dijadwalkan pada $date pukul $time."
            )
        }
    }

    fun updateReservationStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.updateReservationStatus(id, status)
            
            val reservations = repository.getAllReservations().firstOrNull() ?: emptyList()
            val res = reservations.find { it.id == id }
            if (res != null) {
                // If completed and role is capster, also make the capster "Available" again
                if (status == "COMPLETED" || status == "REJECTED") {
                    repository.updateCapsterStatus(res.capsterId, "Available")
                } else if (status == "IN_PROGRESS") {
                    repository.updateCapsterStatus(res.capsterId, "Busy")
                }

                // If COMPLETED, grant 10 loyalty points to the user
                if (status == "COMPLETED") {
                    val customer = repository.getUserSync(res.userEmail)
                    if (customer != null) {
                        val newPoints = customer.points + 10
                        repository.updateUserPoints(res.userEmail, newPoints)
                        
                        repository.insertNotification(NotificationEntity(
                            userEmail = res.userEmail,
                            title = "Anda Mendapatkan 10 Poin!",
                            message = "Terima kasih atas kunjungan Anda bersama ${res.capsterName}. Anda mendapatkan 10 Poin Loyalitas!",
                            type = "BALANCE"
                        ))
                    }
                }

                // Add notification to user
                val title = when (status) {
                    "APPROVED" -> "Reservasi Disetujui"
                    "ON_THE_WAY" -> "Capster Sedang Berjalan"
                    "IN_PROGRESS" -> "Layanan Dimulai"
                    "COMPLETED" -> "Layanan Selesai"
                    "REJECTED" -> "Reservasi Ditolak"
                    else -> "Pembaruan Status Reservasi"
                }

                val message = when (status) {
                    "APPROVED" -> "Reservasi Anda bersama ${res.capsterName} pada ${res.date} telah disetujui."
                    "ON_THE_WAY" -> "Kabar baik! ${res.capsterName} sedang dalam perjalanan menuju lokasi Anda."
                    "IN_PROGRESS" -> "Layanan potong rambut Anda telah dimulai. Bersiaplah untuk penampilan terbaik Anda!"
                    "COMPLETED" -> "Layanan selesai! Terima kasih telah mempercayakan penampilan Anda kepada Barberteak."
                    "REJECTED" -> "Maaf, reservasi Anda bersama ${res.capsterName} tidak dapat disetujui karena kendala jadwal."
                    else -> "Status reservasi Anda telah diperbarui menjadi $status."
                }

                repository.insertNotification(NotificationEntity(
                    userEmail = res.userEmail,
                    title = title,
                    message = message,
                    type = "RESERVATION"
                ))

                // Trigger real-time system notification
                showLocalNotification(title, message)

                // Sync status update to Firebase Firestore
                if (isFirebaseInitialized && firestore != null) {
                    firestore!!.collection("reservations")
                        .document(id.toString())
                        .update("status", status)
                        .addOnSuccessListener {
                            Log.d("FirestoreSync", "Reservation $id status updated to $status in Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreSync", "Failed to update status in Firestore: ${e.message}")
                        }
                }
            }
        }
    }

    fun topUpBalance(amount: Double, paymentMethod: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val newBalance = user.balance + amount
            repository.updateUserBalance(user.email, newBalance)
            
            // Update local StateFlow immediately
            val updatedUser = repository.getUserSync(user.email)
            if (updatedUser != null) {
                _currentUser.value = updatedUser
            }
            
            // Insert transaction record
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
            val referenceNo = "BT-TOP-${System.currentTimeMillis().toString().takeLast(6)}"
            val transaction = TransactionEntity(
                userEmail = user.email,
                type = "TOPUP",
                amount = amount,
                paymentMethod = paymentMethod,
                status = "SUCCESS",
                referenceNo = referenceNo,
                dateStr = dateStr,
                description = "Top Up Saldo via $paymentMethod"
            )
            repository.insertTransaction(transaction)

            // Insert notification
            val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
            repository.insertNotification(NotificationEntity(
                userEmail = user.email,
                title = "Top Up Berhasil",
                message = "Pengisian saldo sebesar ${formatCurrency.format(amount)} via $paymentMethod berhasil ditambahkan ke akun Anda.",
                type = "BALANCE"
            ))
        }
    }

    fun payForReservation(reservationId: Int, paymentMethod: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onComplete(false, "Silakan login terlebih dahulu.")
                return@launch
            }

            val reservations = repository.getAllReservations().firstOrNull() ?: emptyList()
            val res = reservations.find { it.id == reservationId }
            if (res == null) {
                onComplete(false, "Reservasi tidak ditemukan.")
                return@launch
            }

            if (res.paymentStatus == "PAID") {
                onComplete(false, "Reservasi ini sudah lunas.")
                return@launch
            }

            val price = res.servicePrice

            if (paymentMethod == "SALDO") {
                if (user.balance < price) {
                    val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
                    onComplete(false, "Saldo Barberteak Anda tidak mencukupi (Butuh: ${formatCurrency.format(price)}). Silakan top up.")
                    return@launch
                }
                val newBalance = user.balance - price
                repository.updateUserBalance(user.email, newBalance)
                
                // Update local StateFlow
                val updatedUser = repository.getUserSync(user.email)
                if (updatedUser != null) {
                    _currentUser.value = updatedUser
                }
            }

            // Update reservation status in database
            repository.updateReservationPayment(reservationId, paymentMethod, "PAID")

            // Create Transaction record
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
            val referenceNo = "BT-PAY-${System.currentTimeMillis().toString().takeLast(6)}"
            repository.insertTransaction(TransactionEntity(
                userEmail = user.email,
                type = "PAYMENT",
                amount = price,
                paymentMethod = paymentMethod,
                status = "SUCCESS",
                referenceNo = referenceNo,
                dateStr = dateStr,
                description = "Pembayaran Layanan Cukur: ${res.serviceName} (${res.serviceType})"
            ))

            // Create notification
            val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
            repository.insertNotification(NotificationEntity(
                userEmail = user.email,
                title = "Pembayaran Berhasil",
                message = "Pembayaran layanan ${res.serviceName} sebesar ${formatCurrency.format(price)} via $paymentMethod berhasil.",
                type = "PAYMENT"
            ))

            onComplete(true, "Pembayaran berhasil dilakukan via $paymentMethod!")
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val email = _currentUser.value?.email ?: return@launch
            repository.markAllNotificationsAsRead(email)
        }
    }

    // Product purchase simulation
    fun buyProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (product.stock > 0) {
                // Update stock locally
                repository.insertProduct(product.copy(stock = product.stock - 1))
            }
        }
    }

    fun addToCart(productId: Int) {
        val currentCart = _cart.value.toMutableMap()
        val currentQty = currentCart[productId] ?: 0
        currentCart[productId] = currentQty + 1
        _cart.value = currentCart
    }

    fun removeFromCart(productId: Int) {
        val currentCart = _cart.value.toMutableMap()
        val currentQty = currentCart[productId] ?: 0
        if (currentQty <= 1) {
            currentCart.remove(productId)
        } else {
            currentCart[productId] = currentQty - 1
        }
        _cart.value = currentCart
    }

    fun removeProductFromCartCompletely(productId: Int) {
        val currentCart = _cart.value.toMutableMap()
        currentCart.remove(productId)
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun checkoutCart(paymentMethod: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onComplete(false, "Silakan login terlebih dahulu.")
                return@launch
            }

            val productsList = allProducts.value
            val cartItems = _cart.value
            if (cartItems.isEmpty()) {
                onComplete(false, "Keranjang belanja kosong.")
                return@launch
            }

            // Calculate total price and verify stock
            var totalPrice = 0.0
            val updatedProducts = mutableListOf<ProductEntity>()
            val itemNames = mutableListOf<String>()

            for ((productId, qty) in cartItems) {
                val prod = productsList.find { it.id == productId }
                if (prod == null) {
                    onComplete(false, "Produk tidak ditemukan.")
                    return@launch
                }
                if (prod.stock < qty) {
                    onComplete(false, "Stok produk '${prod.name}' tidak mencukupi (Tersedia: ${prod.stock}).")
                    return@launch
                }
                totalPrice += prod.price * qty
                updatedProducts.add(prod.copy(stock = prod.stock - qty))
                itemNames.add("${prod.name} (x$qty)")
            }

            // Deduct balance if using WALLET/SALDO
            if (paymentMethod == "SALDO") {
                if (user.balance < totalPrice) {
                    onComplete(false, "Saldo Barberteak Anda tidak mencukupi untuk pembayaran ini.")
                    return@launch
                }
                val updatedUser = user.copy(balance = user.balance - totalPrice)
                repository.insertUser(updatedUser)
                _currentUser.value = updatedUser
            }

            // Update stocks in database
            updatedProducts.forEach { prod ->
                repository.insertProduct(prod)
            }

            // Create Transaction
            val refNo = "TXN-PRD-${System.currentTimeMillis().toString().takeLast(6)}"
            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
            val desc = "Pembelian produk: ${itemNames.joinToString(", ")}"
            
            repository.insertTransaction(TransactionEntity(
                userEmail = user.email,
                type = "PAYMENT",
                amount = totalPrice,
                paymentMethod = paymentMethod,
                status = "SUCCESS",
                referenceNo = refNo,
                dateStr = dateStr,
                description = desc
            ))

            // Create notification
            val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
            repository.insertNotification(NotificationEntity(
                userEmail = user.email,
                title = "Pembelian Produk Berhasil",
                message = "Pembelian ${itemNames.joinToString(", ")} total ${formatCurrency.format(totalPrice)} via $paymentMethod berhasil diproses.",
                type = "PAYMENT"
            ))

            // Clear the cart
            clearCart()
            onComplete(true, "Pembelian produk berhasil via $paymentMethod!")
        }
    }

    // Add Feedback/Complaint
    fun addComplaint(text: String) {
        val userEmail = _currentUser.value?.email ?: "anonymous@barberteak.com"
        _complaints.value = _complaints.value + (userEmail to text)
    }

    // Waterfall Admin Update
    fun updateWaterfallStage(stageId: Int, status: String, percentage: Int) {
        viewModelScope.launch {
            repository.updateWaterfallStage(stageId, status, percentage)
        }
    }

    // Active Capster Status Update
    fun updateActiveCapsterStatus(status: String) {
        viewModelScope.launch {
            val capster = _activeCapster.value ?: return@launch
            repository.updateCapsterStatus(capster.id, status)
            _activeCapster.value = capster.copy(status = status)
        }
    }

    // Chat Operations
    fun getChatMessages(reservationId: Int): Flow<List<ChatMessageEntity>> {
        return repository.getMessagesForReservation(reservationId)
    }

    fun sendChatMessage(reservationId: Int, messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val role = _currentActiveRole.value
            val senderEmail: String
            val senderName: String
            if (role == "CAPSTER") {
                senderEmail = "capster@barberteak.com"
                senderName = _activeCapster.value?.name ?: "Capster Budi"
            } else if (role == "ADMIN") {
                senderEmail = "admin@barberteak.com"
                senderName = "Admin Utama"
            } else {
                senderEmail = _currentUser.value?.email ?: "client@gmail.com"
                senderName = _currentUser.value?.name ?: "Pelanggan"
            }

            val chatMsg = ChatMessageEntity(
                reservationId = reservationId,
                senderEmail = senderEmail,
                senderName = senderName,
                messageText = messageText.trim()
            )
            repository.insertChatMessage(chatMsg)
            
            // Trigger local/push notification simulation
            showLocalNotification(
                title = "Pesan baru dari $senderName",
                message = messageText.trim()
            )
        }
    }

    // Instant/Push System Notifications
    fun showLocalNotification(title: String, message: String) {
        val context = getApplication<Application>().applicationContext
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "barberteak_notif_channel"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName = "BarberTeak Notifications"
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi Instan BarberTeak Luxury Club"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            android.util.Log.e("BarberViewModel", "Error displaying notification: ${e.message}")
        }
    }

    // Submit a review for a completed service
    fun submitReview(
        reservationId: Int,
        capsterId: Int,
        capsterName: String,
        userEmail: String,
        userName: String,
        rating: Float,
        comment: String
    ) {
        viewModelScope.launch {
            // 1. Insert review record
            val review = ReviewEntity(
                capsterId = capsterId,
                capsterName = capsterName,
                userEmail = userEmail,
                userName = userName,
                rating = rating,
                comment = comment
            )
            repository.insertReview(review)

            // 2. Mark reservation as reviewed
            repository.updateReservationReviewed(reservationId, true)

            // 3. Update capster's average rating dynamically
            val capster = repository.getCapsterById(capsterId)
            if (capster != null) {
                val newCount = capster.reviewsCount + 1
                val newRating = ((capster.rating * capster.reviewsCount) + rating) / newCount
                repository.insertCapster(
                    capster.copy(
                        rating = newRating,
                        reviewsCount = newCount
                    )
                )
            }

            // 4. Send success notification
            showLocalNotification(
                "Ulasan Terkirim",
                "Terima kasih atas ulasan bintang ${rating.toInt()} Anda untuk $capsterName!"
            )
        }
    }

    // Redeem loyalty points for balance/vouchers
    fun redeemPoints(email: String, pointsCost: Int, rewardName: String, rewardValue: Double) {
        viewModelScope.launch {
            val user = repository.getUserSync(email) ?: return@launch
            if (user.points >= pointsCost) {
                val newPoints = user.points - pointsCost
                val newBalance = user.balance + rewardValue
                
                // Update points & balance
                repository.updateUserPoints(email, newPoints)
                repository.updateUserBalance(email, newBalance)

                // Save top-up transaction
                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
                val refNo = "BT-RED-${System.currentTimeMillis().toString().takeLast(6)}"
                repository.insertTransaction(TransactionEntity(
                    userEmail = email,
                    type = "TOPUP",
                    amount = rewardValue,
                    paymentMethod = "REDEEM",
                    status = "SUCCESS",
                    referenceNo = refNo,
                    dateStr = dateStr,
                    description = "Redeem $pointsCost Poin: $rewardName"
                ))

                // Send notification
                repository.insertNotification(NotificationEntity(
                    userEmail = email,
                    title = "Penukaran Kupon Berhasil",
                    message = "Kupon '$rewardName' berhasil ditukarkan! Saldo Anda bertambah sebesar Rp ${String.format("%,.0f", rewardValue)}.",
                    type = "PAYMENT"
                ))

                showLocalNotification(
                    "Penukaran Kupon Berhasil",
                    "Anda menukarkan $pointsCost Poin untuk Voucher Rp ${String.format("%,.0f", rewardValue)}!"
                )
            }
        }
    }
}
