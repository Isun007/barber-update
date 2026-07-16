package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val phone: String,
    val role: String, // "CLIENT", "CAPSTER", "ADMIN"
    val avatarUrl: String = "",
    val membershipTier: String = "Bronze",
    val queueNumber: String? = null,
    val balance: Double = 0.0,
    val points: Int = 0
) : Serializable

@Entity(tableName = "capsters")
data class CapsterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val specialties: String,
    val experience: String,
    val rating: Float,
    val reviewsCount: Int,
    val status: String, // "Available", "Busy", "Off"
    val supportsHomeService: Boolean,
    val imageUrl: String = ""
) : Serializable

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Hair Pomade", "Hair Styling", "Hair Care", "Shaving"
    val price: Double,
    val description: String,
    val stock: Int,
    val rating: Float,
    val imageUrl: String = ""
) : Serializable

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val userName: String,
    val userPhone: String,
    val capsterId: Int,
    val capsterName: String,
    val serviceName: String,
    val servicePrice: Double,
    val serviceType: String, // "STORE" (In-Store), "HOME" (Home Service)
    val homeAddress: String? = null,
    val date: String,
    val time: String,
    val status: String, // "PENDING", "APPROVED", "ON_THE_WAY", "IN_PROGRESS", "COMPLETED", "REJECTED"
    val queueNo: String? = null,
    val paymentMethod: String = "CASH", // "CASH", "TRANSFER", "QRIS"
    val paymentStatus: String = "UNPAID", // "UNPAID", "PAID"
    val isReviewed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val capsterId: Int,
    val capsterName: String,
    val userEmail: String,
    val userName: String,
    val rating: Float,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "waterfall_stages")
data class WaterfallStageEntity(
    @PrimaryKey val stageId: Int,
    val name: String,
    val description: String,
    val percentage: Int,
    val status: String // "COMPLETED", "IN_PROGRESS", "PENDING"
) : Serializable

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val type: String, // "TOPUP", "PAYMENT"
    val amount: Double,
    val paymentMethod: String, // "CASH", "TRANSFER", "QRIS", "SALDO" (using balance)
    val status: String, // "PENDING", "SUCCESS", "FAILED"
    val referenceNo: String, // e.g. "TXN-123456"
    val dateStr: String, // e.g., "14 Jul 2026 19:30"
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val title: String,
    val message: String,
    val type: String, // "RESERVATION", "PAYMENT", "BALANCE"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reservationId: Int,
    val senderEmail: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
