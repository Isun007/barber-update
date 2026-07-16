package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarberViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

// Modern bounce click scale animation for buttons and cards
@Composable
fun Modifier.clickableWithBounce(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}



@Composable
fun BarberAppContent(viewModel: BarberViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeRole by viewModel.currentActiveRole.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("home") } // "home", "book_store", "book_home", "catalog", "capsters", "ask_ai", "history", "complaints"
    var selectedCapsterIdForBooking by remember { mutableStateOf<Int?>(null) }
    var isAiBubbleOpen by remember { mutableStateOf(false) }

    var isNotificationsDialogOpen by remember { mutableStateOf(false) }
    var isTopUpDialogOpen by remember { mutableStateOf(false) }
    var isAccountSettingsDialogOpen by remember { mutableStateOf(false) }
    var activeReceiptTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var activeBookingSuccessInfo by remember { mutableStateOf<Pair<Double, String>?>(null) }

    // Intercept ask_ai and open floating bubble instead of navigating away
    LaunchedEffect(currentScreen) {
        if (currentScreen == "ask_ai") {
            isAiBubbleOpen = true
            currentScreen = "home"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentUser == null) {
                LoginScreen(
                    viewModel = viewModel,
                    onLogin = { email, name, phone ->
                        viewModel.loginUser(email, name, phone)
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
                        val unreadCount = notifications.count { !it.isRead }
                        BarberTopBar(
                            user = currentUser,
                            activeRole = activeRole,
                            currentScreen = currentScreen,
                            unreadNotificationsCount = unreadCount,
                            onNotificationClick = { isNotificationsDialogOpen = true },
                            onBackClick = { currentScreen = "home" },
                            onLogout = {
                                viewModel.logout()
                                currentScreen = "home"
                            },
                            onSettingsClick = { isAccountSettingsDialogOpen = true }
                        )
                    },
                    modifier = Modifier.testTag("app_scaffold")
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Routing screens based on activeRole and currentScreen
                        when (activeRole) {
                            "ADMIN" -> {
                                AdminDashboardScreen(viewModel = viewModel)
                            }
                            "CAPSTER" -> {
                                CapsterDashboardScreen(viewModel = viewModel)
                            }
                            else -> {
                                // Client Flows
                                when (currentScreen) {
                                    "home" -> ClientHomeScreen(
                                        viewModel = viewModel,
                                        onNavigate = { screen, capsterId ->
                                            if (screen == "ask_ai") {
                                                isAiBubbleOpen = true
                                            } else {
                                                currentScreen = screen
                                                selectedCapsterIdForBooking = capsterId
                                            }
                                        },
                                        onTopUpClick = { isTopUpDialogOpen = true },
                                        onTransactionClick = { transaction -> activeReceiptTransaction = transaction }
                                    )
                                    "book_store" -> BookingFormScreen(
                                        viewModel = viewModel,
                                        isHomeService = false,
                                        preselectedCapsterId = selectedCapsterIdForBooking,
                                        onSuccess = { amount, method ->
                                            activeBookingSuccessInfo = amount to method
                                            currentScreen = "history"
                                        },
                                        onCancel = { currentScreen = "home" }
                                    )
                                    "book_home" -> BookingFormScreen(
                                        viewModel = viewModel,
                                        isHomeService = true,
                                        preselectedCapsterId = selectedCapsterIdForBooking,
                                        onSuccess = { amount, method ->
                                            activeBookingSuccessInfo = amount to method
                                            currentScreen = "history"
                                        },
                                        onCancel = { currentScreen = "home" }
                                    )
                                    "catalog" -> ProductCatalogScreen(viewModel = viewModel)
                                    "capsters" -> CapstersListScreen(
                                        viewModel = viewModel,
                                        onSelectCapsterForBooking = { id, type ->
                                            selectedCapsterIdForBooking = id
                                            currentScreen = if (type == "HOME") "book_home" else "book_store"
                                        }
                                    )
                                    "ask_ai" -> AskAiScreen(viewModel = viewModel)
                                    "history" -> BookingHistoryScreen(viewModel = viewModel)
                                    "complaints" -> ComplaintsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }

                // Modern Floating AI Chat Bubble
                if (activeRole == "CLIENT") {
                    FloatingAiBubble(
                        viewModel = viewModel,
                        isOpen = isAiBubbleOpen,
                        onToggle = { isAiBubbleOpen = !isAiBubbleOpen }
                    )
                }

                // Render dialogs if opened
                if (isNotificationsDialogOpen) {
                    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
                    NotificationsDialog(
                        notifications = notifications,
                        onDismiss = { isNotificationsDialogOpen = false },
                        onMarkAllRead = { viewModel.markAllNotificationsAsRead() }
                    )
                }

                if (isTopUpDialogOpen) {
                    TopUpDialog(
                        onDismiss = { isTopUpDialogOpen = false },
                        onConfirm = { amount, method ->
                            viewModel.topUpBalance(amount, method)
                            isTopUpDialogOpen = false
                            // Construct a fake transaction entity to show immediately as receipt
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
                            val dummyTxn = TransactionEntity(
                                referenceNo = "BT-TOP-${System.currentTimeMillis().toString().takeLast(6)}",
                                dateStr = dateStr,
                                type = "TOPUP",
                                amount = amount,
                                paymentMethod = method,
                                status = "SUCCESS",
                                userEmail = currentUser?.email ?: ""
                            )
                            activeReceiptTransaction = dummyTxn
                        }
                    )
                }

                activeReceiptTransaction?.let { transaction ->
                    ReceiptDialog(
                        transaction = transaction,
                        user = currentUser,
                        onDismiss = { activeReceiptTransaction = null }
                    )
                }

                activeBookingSuccessInfo?.let { (amount, method) ->
                    val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
                    AlertDialog(
                        onDismissRequest = { activeBookingSuccessInfo = null },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Booking Berhasil!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Terima kasih! Pesanan Anda telah berhasil dibuat dan antrean Anda sedang diproses oleh Capster.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Rincian Pembayaran:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TeakHoney
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Biaya:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = formatCurrency.format(amount),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Metode Pembayaran:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = when (method) {
                                            "TRANSFER" -> "Transfer Bank"
                                            "QRIS" -> "QRIS"
                                            "SALDO" -> "Saldo Barberteak"
                                            else -> "Tunai (Cash)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                if (method == "TRANSFER" || method == "QRIS") {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = "Silakan periksa detail rekening / QRIS di menu Riwayat untuk menyelesaikan pembayaran Anda.",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.padding(8.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { activeBookingSuccessInfo = null },
                                colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary)
                            ) {
                                Text("Selesai & Lihat Riwayat", color = Color.White)
                            }
                        }
                    )
                }

                if (isAccountSettingsDialogOpen) {
                    AccountSettingsDialog(
                        currentUser = currentUser,
                        viewModel = viewModel,
                        onDismiss = { isAccountSettingsDialogOpen = false },
                        onSwitchAccount = { email, name, role ->
                            viewModel.loginUser(email, name ?: "", "", role)
                        },
                        onLogout = {
                            viewModel.logout()
                            currentScreen = "home"
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// COMPONENTS: REUSABLE HEADERS, ROLE SELECTORS, ETC
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberTopBar(
    user: UserEntity?,
    activeRole: String,
    currentScreen: String,
    unreadNotificationsCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Logo",
                    tint = TeakGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "BARBER TEAK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.5.sp
                    ),
                    color = TeakGold
                )
            }
        },
        navigationIcon = {
            if (activeRole == "CLIENT" && currentScreen != "home") {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = {
            if (activeRole == "CLIENT") {
                IconButton(onClick = onNotificationClick, modifier = Modifier.testTag("notification_bell_button")) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = TeakGold
                        )
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(14.dp)
                                    .background(StatusRed, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadNotificationsCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Neat role indicator badge next to menu button
            Card(
                colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickableWithBounce { onSettingsClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (activeRole) {
                            "ADMIN" -> Icons.Default.AdminPanelSettings
                            "CAPSTER" -> Icons.Default.ContentCut
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = TeakGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = when (activeRole) {
                            "ADMIN" -> "ADMIN"
                            "CAPSTER" -> "CAPSTER"
                            else -> "CLIENT"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = TeakGold
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("account_settings_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Pengaturan Akun",
                    tint = TeakGold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun RoleSelectorBar(
    activeRole: String,
    onRoleSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = "EVALUASI MULTI-ROLE (Pilih peran untuk review instan):",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = TeakHoney,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val roles = listOf(
                "CLIENT" to "1. Pelanggan",
                "CAPSTER" to "2. Capster (Budi)",
                "ADMIN" to "3. Admin Utama"
            )
            roles.forEach { (roleKey, label) ->
                val isSelected = activeRole == roleKey
                Button(
                    onClick = { onRoleSelected(roleKey) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) TeakWoodPrimary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("role_btn_$roleKey"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// LOGIN SCREEN
// ---------------------------------------------------------------------------------

@Composable
fun LoginScreen(
    viewModel: BarberViewModel? = null,
    onLogin: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var email by remember { mutableStateOf("yudhaactaffian007@gmail.com") }
    var name by remember { mutableStateOf("Yudha Actaffian") }
    var phone by remember { mutableStateOf("08556677889") }
    var password by remember { mutableStateOf("123456") }
    var isNewUser by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Forgot password states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetStep by remember { mutableStateOf(1) } // 1: Input Email, 2: Verification Code & New Password
    var verificationCodeInput by remember { mutableStateOf("") }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }
    var resetIsLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val authError = viewModel?.authError?.collectAsStateWithLifecycle()?.value
    val unregisteredGmail = viewModel?.unregisteredGmail?.collectAsStateWithLifecycle()?.value

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(BackgroundDark, Color(0xFF1E1712))
    )

    // Clear error states on switch between login/register
    LaunchedEffect(isNewUser) {
        viewModel?.clearAuthErrors()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Hero Branding
        Card(
            modifier = Modifier
                .size(110.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Barberteak",
                    tint = TeakGold,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Text(
            text = "BARBERTEAK",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 4.sp
            ),
            color = TeakGold
        )
        Text(
            text = "Layanan Barber On-Demand & Home Service Modern",
            style = MaterialTheme.typography.bodySmall,
            color = OnBackgroundDark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Tampilan jika akun gmail belum terdaftar
        if (unregisteredGmail != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Unregistered",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Akun Gmail Belum Terdaftar!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "Email \"$unregisteredGmail\" belum terdaftar di sistem Barberteak. Silakan buat akun baru dengan mengklik tombol daftar di bawah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isNewUser = true
                                email = unregisteredGmail
                                viewModel.clearAuthErrors()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Daftar Sekarang", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearAuthErrors() },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        // 2. Notifikasi Gagal (Kolom Kosong / Password atau Email salah)
        if (authError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel?.clearAuthErrors() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isNewUser) "Daftar Akun Baru" else "Masuk Aplikasi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackgroundDark
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        viewModel?.clearAuthErrors()
                    },
                    label = { Text("Email (Gmail)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeakGold,
                        focusedLabelColor = TeakGold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = TeakGold)
                    }
                )

                if (isNewUser) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            viewModel?.clearAuthErrors()
                        },
                        label = { Text("Nama Lengkap") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeakGold,
                            focusedLabelColor = TeakGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_name_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TeakGold)
                        }
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { 
                            phone = it
                            viewModel?.clearAuthErrors()
                        },
                        label = { Text("Nomor HP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeakGold,
                            focusedLabelColor = TeakGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = TeakGold)
                        }
                    )
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        viewModel?.clearAuthErrors()
                    },
                    label = { Text("Kata Sandi") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeakGold,
                        focusedLabelColor = TeakGold
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TeakGold)
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = TeakGold)
                        }
                    }
                )

                if (!isNewUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "*Sandi \"123456\" untuk demo.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeakHoney,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showForgotPasswordDialog = true },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp).testTag("forgot_password_button")
                        ) {
                            Text(
                                text = "Lupa Kata Sandi?",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TeakGold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (viewModel != null) {
                            viewModel.loginUserWithValidation(email, password, isNewUser, name, phone)
                        } else {
                            onLogin(email, if (isNewUser) name else "", if (isNewUser) phone else "")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_submit_button")
                ) {
                    Text(
                        text = if (isNewUser) "Daftar" else "Masuk",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                // Switch Sign in / Sign up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNewUser) "Sudah punya akun?" else "Pengguna baru?",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackgroundDark.copy(alpha = 0.6f)
                    )
                    TextButton(onClick = { isNewUser = !isNewUser }) {
                        Text(
                            text = if (isNewUser) "Masuk di sini" else "Daftar di sini",
                            color = TeakGold,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Predefined Quick Accounts Selector (STELAR UX FOR DEMO EVALUATION)
        Text(
            text = "DEMO QUICK LOGIN (Pilih email khusus):",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TeakHoney,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val specialAccounts = listOf(
                Triple("yudhaactaffian007@gmail.com", "Client (VIP)", "vip_btn"),
                Triple("capster@barberteak.com", "Capster Budi", "capster_btn"),
                Triple("admin@barberteak.com", "Admin Utama", "admin_btn")
            )
            specialAccounts.forEach { (specEmail, label, tag) ->
                Button(
                    onClick = {
                        email = specEmail
                        password = "123456"
                        isNewUser = false
                        if (viewModel != null) {
                            viewModel.loginUserWithValidation(specEmail, "123456", false, "", "")
                        } else {
                            onLogin(specEmail, "", "")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("quick_$tag"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TeakGold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // LUPA PASSWORD DIALOG FLOW
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                resetEmail = ""
                resetNewPassword = ""
                resetStep = 1
                verificationCodeInput = ""
                resetErrorMessage = null
                resetSuccessMessage = null
            },
            title = {
                Text(
                    text = "Atur Ulang Kata Sandi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TeakGold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (resetSuccessMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, StatusGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = resetSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StatusGreen,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (resetErrorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = resetErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (resetSuccessMessage == null) {
                        if (resetStep == 1) {
                            Text(
                                text = "Masukkan email akun Barberteak Anda untuk verifikasi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnBackgroundDark.copy(alpha = 0.8f)
                            )

                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { 
                                    resetEmail = it
                                    resetErrorMessage = null
                                },
                                label = { Text("Email (Gmail)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TeakGold,
                                    focusedLabelColor = TeakGold,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = TeakGold)
                                }
                            )
                        } else {
                            Text(
                                text = "Kode verifikasi (simulasi) telah dikirim ke $resetEmail.\n\nGunakan kode '1234' untuk memverifikasi dan masukkan sandi baru Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnBackgroundDark.copy(alpha = 0.8f)
                            )

                            OutlinedTextField(
                                value = verificationCodeInput,
                                onValueChange = { 
                                    verificationCodeInput = it
                                    resetErrorMessage = null
                                },
                                label = { Text("Kode Verifikasi (OTP)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TeakGold,
                                    focusedLabelColor = TeakGold,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("reset_otp_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = TeakGold)
                                }
                            )

                            var resetPasswordVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = resetNewPassword,
                                onValueChange = { 
                                    resetNewPassword = it
                                    resetErrorMessage = null
                                },
                                label = { Text("Kata Sandi Baru") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TeakGold,
                                    focusedLabelColor = TeakGold,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                                ),
                                visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("reset_new_password_input"),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TeakGold)
                                },
                                trailingIcon = {
                                    val icon = if (resetPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                        Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = TeakGold)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (resetSuccessMessage != null) {
                    Button(
                        onClick = {
                            showForgotPasswordDialog = false
                            resetEmail = ""
                            resetNewPassword = ""
                            resetStep = 1
                            verificationCodeInput = ""
                            resetErrorMessage = null
                            resetSuccessMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary)
                    ) {
                        Text("Selesai", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            if (resetIsLoading) return@Button
                            resetErrorMessage = null

                            coroutineScope.launch {
                                resetIsLoading = true
                                val emailFormatted = resetEmail.trim().lowercase()
                                if (resetStep == 1) {
                                    if (emailFormatted.isEmpty()) {
                                        resetErrorMessage = "Gagal: Email tidak boleh kosong!"
                                        resetIsLoading = false
                                        return@launch
                                    }

                                    // Verify user exists in local database
                                    val exists = viewModel?.checkUserExists(emailFormatted) ?: false
                                    if (!exists) {
                                        resetErrorMessage = "Gagal: Email \"$resetEmail\" tidak terdaftar di sistem!"
                                    } else {
                                        resetStep = 2
                                    }
                                } else {
                                    val otp = verificationCodeInput.trim()
                                    val pass = resetNewPassword.trim()

                                    if (otp.isEmpty() || pass.isEmpty()) {
                                        resetErrorMessage = "Gagal: Kolom tidak boleh kosong!"
                                        resetIsLoading = false
                                        return@launch
                                    }

                                    if (otp != "1234") {
                                        resetErrorMessage = "Gagal: Kode verifikasi (OTP) salah!"
                                        resetIsLoading = false
                                        return@launch
                                    }

                                    if (pass.length < 6) {
                                        resetErrorMessage = "Gagal: Kata sandi minimal harus 6 karakter!"
                                        resetIsLoading = false
                                        return@launch
                                    }

                                    val resetOk = viewModel?.resetPassword(emailFormatted, pass) ?: false
                                    if (resetOk) {
                                        resetSuccessMessage = "Sukses! Kata sandi Anda berhasil diubah. Silakan gunakan kata sandi baru untuk masuk."
                                    } else {
                                        resetErrorMessage = "Gagal: Terjadi kesalahan saat menyimpan kata sandi baru."
                                    }
                                }
                                resetIsLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                        enabled = !resetIsLoading
                    ) {
                        if (resetIsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(if (resetStep == 1) "Verifikasi Email" else "Atur Ulang Sandi", color = Color.White)
                        }
                    }
                }
            },
            dismissButton = {
                if (resetSuccessMessage == null) {
                    TextButton(
                        onClick = {
                            showForgotPasswordDialog = false
                            resetEmail = ""
                            resetNewPassword = ""
                            resetStep = 1
                            verificationCodeInput = ""
                            resetErrorMessage = null
                            resetSuccessMessage = null
                        }
                    ) {
                        Text("Batal", color = Color.LightGray)
                    }
                }
            },
            containerColor = SurfaceDark,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun LoyaltyAndRewardsWidget(
    user: UserEntity?,
    onRedeem: (Int, String, Double) -> Unit
) {
    val points = user?.points ?: 0
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Loyalty Points",
                        tint = TeakGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Loyalty Club & Poin",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TeakHoney
                        )
                        Text(
                            text = "Dapatkan 10 Poin tiap potong rambut!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$points POIN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TeakGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            
            Text(
                text = "Tukarkan Poin dengan Voucher Saldo Gratis:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            val rewards = listOf(
                Triple(50, "Voucher Saldo Rp 25.000", 25000.0),
                Triple(100, "Voucher Saldo Rp 60.000", 60000.0),
                Triple(150, "Voucher Saldo Rp 100.000", 100000.0)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rewards.forEach { (cost, name, value) ->
                    val canRedeem = points >= cost
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (canRedeem) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canRedeem) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Biaya: $cost Poin",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canRedeem) TeakHoney else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = { onRedeem(cost, name, value) },
                            enabled = canRedeem,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TeakGold,
                                contentColor = Color.Black,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp).testTag("redeem_${cost}_points")
                        ) {
                            Text(text = "Tukarkan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT HOME SCREEN (MOBILE JKN STYLE)
// ---------------------------------------------------------------------------------

@Composable
fun ClientHomeScreen(
    viewModel: BarberViewModel,
    onNavigate: (String, Int?) -> Unit,
    onTopUpClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val reservations by viewModel.userReservations.collectAsStateWithLifecycle()
    val transactions by viewModel.userTransactions.collectAsStateWithLifecycle()

    val activeReservation = reservations.firstOrNull {
        it.status != "COMPLETED" && it.status != "REJECTED"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // GREETING & MEMBERSHIP CARD (Mobile JKN Style)
        item {
            ClientMembershipCard(user = currentUser)
        }

        // WALLET & TRANSACTION CARD (Saldo & Top Up)
        item {
            WalletAndTransactionsCard(
                user = currentUser,
                transactions = transactions,
                onTopUpClick = onTopUpClick,
                onTransactionClick = onTransactionClick
            )
        }

        // LOYALTY POINTS & REDEEM SECTION
        item {
            LoyaltyAndRewardsWidget(
                user = currentUser,
                onRedeem = { points, name, value ->
                    currentUser?.email?.let { email ->
                        viewModel.redeemPoints(email, points, name, value)
                    }
                }
            )
        }

        // ACTIVE QUEUE WIDGET (If booking exists)
        if (activeReservation != null) {
            item {
                ActiveQueueWidget(
                    reservation = activeReservation,
                    onClick = { onNavigate("history", null) }
                )
            }
        }

        // SERVICE GRID MENU
        item {
            Text(
                text = "Menu Layanan Utama",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceMenuItem(
                    title = "Reservasi Toko",
                    icon = Icons.Default.Store,
                    color = TeakWoodPrimary,
                    description = "Antre di toko",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("book_store", null) }
                )
                ServiceMenuItem(
                    title = "Home Service",
                    icon = Icons.Default.DirectionsCar,
                    color = TeakGold,
                    description = "Panggil ke rumah",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("book_home", null) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceMenuItem(
                    title = "Katalog Produk",
                    icon = Icons.Default.ShoppingBag,
                    color = TeakHoney,
                    description = "Pomade & Hair Tonic",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("catalog", null) }
                )
                ServiceMenuItem(
                    title = "Pilih Capster",
                    icon = Icons.Default.ContentCut,
                    color = Color(0xFF6D4C41),
                    description = "Daftar pemotong",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("capsters", null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceMenuItem(
                    title = "Tanya AI Barber",
                    icon = Icons.Default.AutoAwesome,
                    color = StatusBlue,
                    description = "Saran gaya & rambut",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("ask_ai", null) }
                )
                ServiceMenuItem(
                    title = "Riwayat Reservasi",
                    icon = Icons.Default.History,
                    color = StatusGreen,
                    description = "Cek status booking",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("history", null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                ServiceMenuItem(
                    title = "Pengaduan & Kritik",
                    icon = Icons.Default.Feedback,
                    color = StatusAmber,
                    description = "Kotak kritik/saran demi peningkatan kualitas layanan kami",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate("complaints", null) }
                )
            }
        }

        // HERO BANNER SHOWCASE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_barber_hero),
                        contentDescription = "Barber Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Text(
                        text = "BARBER TEAK: Potongan Rapi, Tepat Waktu.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClientMembershipCard(user: UserEntity?) {
    val brush = Brush.linearGradient(
        colors = listOf(TeakWoodDark, TeakWoodPrimary, Color(0xFF5D4037)),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(20.dp)
        ) {
            // Background branding lines
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawLine(
                            color = TeakGold.copy(alpha = 0.1f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 10f
                        )
                    }
            )

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KARTU ANGGOTA VIRTUAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = TeakGold
                        )
                        Text(
                            text = "BARBER TEAK LUXURY CLUB",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = Color.White
                        )
                    }
                    // Tier Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TeakGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = user?.membershipTier?.uppercase() ?: "BRONZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                var currentTimeString by remember { mutableStateOf("") }
                var currentDateString by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale("id", "ID"))
                    val dateFormat = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
                    while (true) {
                        val now = java.util.Date()
                        currentTimeString = timeFormat.format(now)
                        currentDateString = dateFormat.format(now)
                        kotlinx.coroutines.delay(1000)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "NAMA ANGGOTA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = user?.name ?: "PELANGGAN",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = user?.phone ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (currentDateString.isNotEmpty()) {
                            Text(
                                text = currentDateString,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = currentTimeString,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TeakGold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Card",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveQueueWidget(
    reservation: ReservationEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickableWithBounce(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(TeakGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reservation.serviceType == "HOME") Icons.Default.DirectionsCar else Icons.Default.Store,
                    contentDescription = "Tipe",
                    tint = TeakGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ANTREAN AKTIF: ${reservation.queueNo ?: "-"}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TeakHoney
                )
                Text(
                    text = "${reservation.serviceName} dengan ${reservation.capsterName}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (reservation.status) {
                                "PENDING" -> StatusAmber.copy(alpha = 0.2f)
                                "APPROVED" -> StatusGreen.copy(alpha = 0.2f)
                                "ON_THE_WAY" -> StatusBlue.copy(alpha = 0.2f)
                                "IN_PROGRESS" -> TeakWoodPrimary.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = reservation.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when (reservation.status) {
                                    "PENDING" -> StatusAmber
                                    "APPROVED" -> StatusGreen
                                    "ON_THE_WAY" -> StatusBlue
                                    "IN_PROGRESS" -> TeakHoney
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint = TeakGold
            )
        }
    }
}

@Composable
fun ServiceMenuItem(
    title: String,
    icon: ImageVector,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickableWithBounce(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: BOOKING FORM
// ---------------------------------------------------------------------------------

fun getDynamicDates(): List<String> {
    val locale = java.util.Locale("id", "ID")
    val sdf = java.text.SimpleDateFormat("EEEE, d MMM yyyy", locale)
    val today = java.util.Calendar.getInstance()
    
    val list = mutableListOf<String>()
    
    // Today
    val todayStr = "Hari Ini (${sdf.format(today.time)})"
    list.add(todayStr)
    
    // Tomorrow
    today.add(java.util.Calendar.DATE, 1)
    val tomorrowStr = "Besok (${sdf.format(today.time)})"
    list.add(tomorrowStr)
    
    // Day after tomorrow
    today.add(java.util.Calendar.DATE, 1)
    val dayAfterStr = sdf.format(today.time)
    list.add(dayAfterStr)
    
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    viewModel: BarberViewModel,
    isHomeService: Boolean,
    preselectedCapsterId: Int?,
    onSuccess: (Double, String) -> Unit,
    onCancel: () -> Unit
) {
    val capsters by viewModel.allCapsters.collectAsStateWithLifecycle()
    val availableCapsters = capsters.filter { !isHomeService || it.supportsHomeService }

    var selectedCapster by remember {
        mutableStateOf(availableCapsters.find { it.id == preselectedCapsterId } ?: availableCapsters.firstOrNull())
    }

    // Preselected services
    val services = listOf(
        Pair("Gentleman Classic Haircut", 60000.0),
        Pair("Premium Cut + Wash + Styling", 90000.0),
        Pair("Hair Coloring (Golden/Brown/Matte)", 120000.0),
        Pair("Beard Shave & Hot Towel Massage", 45000.0),
        Pair("Full Package Royal Treatment", 150000.0)
    )

    var selectedService by remember { mutableStateOf(services.first()) }
    var address by remember { mutableStateOf("") }
    
    val dynamicDates = remember { getDynamicDates() }
    var dateSelection by remember { mutableStateOf(dynamicDates.firstOrNull() ?: "") }
    
    var timeSelection by remember { mutableStateOf("10:00 WIB") }
    var paymentMethod by remember { mutableStateOf("CASH") } // "CASH", "TRANSFER", "QRIS", "SALDO"
    var selectedBank by remember { mutableStateOf("BCA") } // "BCA", "MANDIRI"

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentBalance = currentUser?.balance ?: 0.0
    
    val basePrice = if (isHomeService) selectedService.second * 1.30 else selectedService.second
    val transportFee = if (isHomeService) 30000.0 else 0.0
    val totalCost = basePrice + transportFee
    val hasEnoughBalance = currentBalance >= totalCost

    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isHomeService) "Form On-Demand Home Service" else "Form Reservasi Antrean Toko",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = if (isHomeService) "Capster kami akan datang ke alamat Anda lengkap dengan peralatan cukur steril." 
                   else "Sistem antrean digital akan memberi Anda nomor antrean setelah reservasi disetujui.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // Select Service Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Jenis Layanan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                services.forEach { service ->
                    val isSelected = selectedService.first == service.first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableWithBounce { selectedService = service }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedService = service },
                            colors = RadioButtonDefaults.colors(selectedColor = TeakWoodPrimary)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = service.first,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatCurrency.format(if (isHomeService) service.second * 1.30 else service.second),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TeakHoney
                                )
                                if (isHomeService) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TeakGold.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Premium (+30%)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TeakGold, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Select Capster Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Capster / Barber",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableCapsters) { capster ->
                        val isSelected = selectedCapster?.id == capster.id
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickableWithBounce { selectedCapster = capster },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TeakWoodPrimary.copy(alpha = 0.15f) 
                                                 else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TeakWoodPrimary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(TeakWoodPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Capster",
                                        tint = TeakWoodPrimary
                                    )
                                }
                                Text(
                                    text = capster.name.split(" ")[0],
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Text(
                                    text = "⭐ ${capster.rating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TeakHoney
                                )
                                Text(
                                    text = capster.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = if (capster.status == "Available") StatusGreen else StatusAmber
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Home Service Address if needed
        if (isHomeService) {
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Alamat Lengkap Kunjungan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Alamat akan terintegrasi dengan Google Maps untuk navigasi akurat Capster menuju lokasi Anda.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("Tulis alamat rumah lengkap Anda...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("home_address_input")
                    )
                    Button(
                        onClick = {
                            val mapUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(address.ifEmpty { "Barbershop" }))
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri)
                            context.startActivity(mapIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("search_on_maps_button")
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Google Maps & Verifikasi Alamat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Schedule Picker Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pilih Waktu Kunjungan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Dynamic schedules
                val dates = dynamicDates
                val times = listOf("09:00 WIB", "10:30 WIB", "13:00 WIB", "15:00 WIB", "17:30 WIB")

                Text(text = "Tanggal:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(dates) { date ->
                        val isSel = dateSelection == date
                        FilterChip(
                            selected = isSel,
                            onClick = { dateSelection = date },
                            label = { Text(date, fontSize = 11.sp) }
                        )
                    }
                }

                Text(text = "Jam Mulai:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(times) { time ->
                        val isSel = timeSelection == time
                        FilterChip(
                            selected = isSel,
                            onClick = { timeSelection = time },
                            label = { Text(time, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Payment Method Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pilih Metode Pembayaran",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TeakGold
                )

                // Cash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableWithBounce { paymentMethod = "CASH" }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "CASH",
                        onClick = { paymentMethod = "CASH" },
                        colors = RadioButtonDefaults.colors(selectedColor = TeakWoodPrimary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Tunai di Tempat (Cash)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Bayar tunai ke capster / kasir setelah selesai pengerjaan.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                // Bank Transfer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableWithBounce { paymentMethod = "TRANSFER" }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "TRANSFER",
                        onClick = { paymentMethod = "TRANSFER" },
                        colors = RadioButtonDefaults.colors(selectedColor = TeakWoodPrimary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Transfer Bank Manual", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Transfer manual ke rekening resmi bank Barberteak.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                // QRIS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableWithBounce { paymentMethod = "QRIS" }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "QRIS",
                        onClick = { paymentMethod = "QRIS" },
                        colors = RadioButtonDefaults.colors(selectedColor = TeakWoodPrimary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("QRIS (E-Wallet & M-Banking)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Scan kode QR instan via DANA, OVO, GoPay, ShopeePay, dll.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }

                // Saldo Barberteak
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableWithBounce { paymentMethod = "SALDO" }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == "SALDO",
                        onClick = { paymentMethod = "SALDO" },
                        colors = RadioButtonDefaults.colors(selectedColor = TeakWoodPrimary)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Saldo Barberteak", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TeakGold.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatCurrency.format(currentBalance),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TeakGold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!hasEnoughBalance) {
                            Text("Saldo Anda tidak mencukupi. Silakan lakukan top up terlebih dahulu.", style = MaterialTheme.typography.labelSmall, color = StatusRed)
                        } else {
                            Text("Bayar instan menggunakan saldo dompet digital Anda.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }

                // Show conditional payment details
                if (paymentMethod == "TRANSFER") {
                    Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedBank == "BCA",
                            onClick = { selectedBank = "BCA" },
                            label = { Text("Bank BCA", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedBank == "MANDIRI",
                            onClick = { selectedBank = "MANDIRI" },
                            label = { Text("Bank Mandiri", fontSize = 11.sp) }
                        )
                    }

                    val bankName = if (selectedBank == "BCA") "Bank BCA" else "Bank Mandiri"
                    val bankNumber = if (selectedBank == "BCA") "782-901-2334" else "131-00-988-2134"
                    val accountHolder = "PT Barberteak Indonesia Jaya"
                    
                    val context = LocalContext.current
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Detail Rekening Transfer:", style = MaterialTheme.typography.labelSmall, color = TeakHoney)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "$bankName", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "$bankNumber", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TeakGold)
                                    Text(text = "a/n $accountHolder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("No Rekening", bankNumber)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "No Rekening $bankName berhasil disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("copy_bank_button")
                                ) {
                                    Text("Salin", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                } else if (paymentMethod == "QRIS") {
                    Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // QRIS Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "QRIS",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF1B4E8F)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GPN",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            
                            // Mock QR Code generated with custom Canvas drawings
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color.White)
                                    .border(1.dp, Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(140.dp)) {
                                    val size = this.size.width
                                    val cellSize = size / 7
                                    
                                    // Finder patterns (top-left, top-right, bottom-left)
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.3f, cellSize * 0.3f), size = androidx.compose.ui.geometry.Size(cellSize * 1.4f, cellSize * 1.4f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.6f, cellSize * 0.6f), size = androidx.compose.ui.geometry.Size(cellSize * 0.8f, cellSize * 0.8f))
                                    
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size - cellSize * 2, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size - cellSize * 1.7f, cellSize * 0.3f), size = androidx.compose.ui.geometry.Size(cellSize * 1.4f, cellSize * 1.4f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size - cellSize * 1.4f, cellSize * 0.6f), size = androidx.compose.ui.geometry.Size(cellSize * 0.8f, cellSize * 0.8f))
                                    
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, size - cellSize * 2), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.3f, size - cellSize * 1.7f), size = androidx.compose.ui.geometry.Size(cellSize * 1.4f, cellSize * 1.4f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.6f, size - cellSize * 1.4f), size = androidx.compose.ui.geometry.Size(cellSize * 0.8f, cellSize * 0.8f))
                                    
                                    // Custom mock QR cells
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 3, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 4, cellSize * 3), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize, cellSize * 4), size = androidx.compose.ui.geometry.Size(cellSize, cellSize * 2))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 3, cellSize * 5), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 5, cellSize * 5), size = androidx.compose.ui.geometry.Size(cellSize, cellSize * 2))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 4, cellSize * 4), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                    
                                    // Small QRIS logo center
                                    drawRect(Color(0xFF1B4E8F), topLeft = androidx.compose.ui.geometry.Offset(size/2 - cellSize/2, size/2 - cellSize/2), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                }
                            }
                            
                            Text(
                                text = "BARBERTEAK OFFICIAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                            Text(
                                text = "NMID: ID1020304050607",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Pindai kode QR di atas untuk menyelesaikan pembayaran digital.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Summary Price
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = if (isHomeService) "Total Biaya Layanan (Premium)" else "Total Biaya Layanan",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formatCurrency.format(totalCost),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TeakHoney
                    )
                    if (isHomeService) {
                        Text(
                            text = "(Termasuk tarif premium +30% & transport Rp 30.000)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = {
                            selectedCapster?.let {
                                val resolvedServiceName = if (isHomeService) {
                                    "${selectedService.first} (Premium Home)"
                                } else {
                                    selectedService.first
                                }
                                viewModel.createReservation(
                                    capsterId = it.id,
                                    capsterName = it.name,
                                    serviceName = resolvedServiceName,
                                    servicePrice = totalCost,
                                    serviceType = if (isHomeService) "HOME" else "STORE",
                                    homeAddress = if (isHomeService) address else null,
                                    date = dateSelection,
                                    time = timeSelection,
                                    paymentMethod = paymentMethod,
                                    paymentStatus = if (paymentMethod == "CASH") "UNPAID" else "PAID"
                                )
                                onSuccess(totalCost, paymentMethod)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                        enabled = selectedCapster != null && (!isHomeService || address.isNotEmpty()) && (paymentMethod != "SALDO" || hasEnoughBalance),
                        modifier = Modifier.testTag("confirm_booking_button")
                    ) {
                        Text("Konfirmasi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    TextButton(onClick = onCancel) {
                        Text("Batalkan", color = StatusRed, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: PRODUCTS CATALOG
// ---------------------------------------------------------------------------------

@Composable
fun ProductCatalogScreen(viewModel: BarberViewModel) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val cartItems by viewModel.cart.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    var isCartDialogOpen by remember { mutableStateOf(false) }

    val totalItemsInCart = cartItems.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Shopping Cart Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Katalog Produk Barberteak",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                    color = TeakGold
                )
                Text(
                    text = "Dapatkan produk perawatan rambut dan pomade berkualitas salon profesional langsung dari genggaman Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Cart icon with Badge
            IconButton(
                onClick = { isCartDialogOpen = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(TeakWoodPrimary.copy(alpha = 0.15f), shape = CircleShape)
                    .border(1.dp, TeakGold.copy(alpha = 0.3f), shape = CircleShape)
                    .testTag("open_cart_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Keranjang Belanja",
                        tint = TeakGold,
                        modifier = Modifier.size(22.dp)
                    )
                    if (totalItemsInCart > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .background(StatusRed, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = totalItemsInCart.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(products) { product ->
                val quantityInCart = cartItems[product.id] ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.15f))
                ) {
                    Column {
                        // Product image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl == "img_hair_pomade") {
                                Image(
                                    painter = painterResource(id = R.drawable.img_hair_pomade),
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(TeakWoodPrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Produk",
                                        tint = TeakWoodPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            if (quantityInCart > 0) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = TeakHoney),
                                    shape = RoundedCornerShape(bottomStart = 8.dp),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = "$quantityInCart di Keranjang",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = product.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TeakHoney
                            )
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "⭐ ${product.rating}  •  Stok: ${product.stock}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatCurrency.format(product.price),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TeakWoodPrimary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.addToCart(product.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                shape = RoundedCornerShape(8.dp),
                                enabled = product.stock > quantityInCart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("buy_${product.id}"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (product.stock > quantityInCart) "Beli" else "Habis",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCartDialogOpen) {
        CartCheckoutDialog(
            viewModel = viewModel,
            onDismiss = { isCartDialogOpen = false }
        )
    }
}

@Composable
fun CartCheckoutDialog(
    viewModel: BarberViewModel,
    onDismiss: () -> Unit
) {
    val cartItems by viewModel.cart.collectAsStateWithLifecycle()
    val productsList by viewModel.allProducts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    var paymentMethod by remember { mutableStateOf("CASH") } // "CASH", "QRIS", "TRANSFER", "SALDO"
    var isProcessing by remember { mutableStateOf(false) }

    // Map cart IDs to actual products with quantity
    val cartProducts = cartItems.mapNotNull { (productId, qty) ->
        val product = productsList.find { it.id == productId }
        if (product != null) product to qty else null
    }

    val totalPrice = cartProducts.sumOf { (product, qty) -> product.price * qty }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, TeakGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Keranjang",
                            tint = TeakGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Keranjang Belanja",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TeakGold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                if (cartProducts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Keranjang belanja Anda masih kosong.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Pilih Produk", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Cart List
                    Text(
                        text = "DAFTAR ITEM",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TeakHoney
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        cartProducts.forEach { (product, qty) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(TeakWoodPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = TeakGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatCurrency.format(product.price),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TeakHoney
                                        )
                                    }

                                    // Quantity Selector Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.removeFromCart(product.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Kurang",
                                                tint = TeakGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Text(
                                            text = qty.toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                if (product.stock > qty) {
                                                    viewModel.addToCart(product.id)
                                                } else {
                                                    android.widget.Toast.makeText(context, "Stok tidak mencukupi!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Tambah",
                                                tint = TeakGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeProductFromCartCompletely(product.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = StatusRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Payment Method selector
                    Text(
                        text = "METODE PEMBAYARAN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TeakHoney
                    )

                    val paymentMethods = listOf(
                        Triple("CASH", "Tunai di Kasir", Icons.Default.Payments),
                        Triple("QRIS", "QRIS Digital", Icons.Default.QrCodeScanner),
                        Triple("TRANSFER", "Transfer Bank", Icons.Default.AccountBalance),
                        Triple("SALDO", "Saldo Barber (${formatCurrency.format(currentUser?.balance ?: 0.0)})", Icons.Default.Wallet)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        paymentMethods.forEach { (key, label, icon) ->
                            val isSelected = paymentMethod == key
                            val hasBalance = key != "SALDO" || (currentUser?.balance ?: 0.0) >= totalPrice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) TeakGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TeakGold else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickableWithBounce {
                                        if (hasBalance) {
                                            paymentMethod = key
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) TeakGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TeakGold else if (hasBalance) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = StatusGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // QRIS scan area if QRIS chosen
                    if (paymentMethod == "QRIS") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "QRIS",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFF1B4E8F)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "GPN",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFD32F2F)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .background(Color.White)
                                        .border(1.dp, Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(110.dp)) {
                                        val size = this.size.width
                                        val cellSize = size / 7
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.3f, cellSize * 0.3f), size = androidx.compose.ui.geometry.Size(cellSize * 1.4f, cellSize * 1.4f))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.6f, cellSize * 0.6f), size = androidx.compose.ui.geometry.Size(cellSize * 0.8f, cellSize * 0.8f))
                                        
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size - cellSize * 2, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size - cellSize * 1.7f, cellSize * 0.3f), size = androidx.compose.ui.geometry.Size(cellSize * 1.4f, cellSize * 1.4f))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size - cellSize * 1.4f, cellSize * 0.6f), size = androidx.compose.ui.geometry.Size(cellSize * 0.8f, cellSize * 0.8f))
                                        
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, size - cellSize * 2), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize * 2))
                                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.3f, size - cellSize * 1.7f), size = androidx.compose.ui.geometry.Size(cellSize * 1.4f, cellSize * 1.4f))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 0.6f, size - cellSize * 1.4f), size = androidx.compose.ui.geometry.Size(cellSize * 0.8f, cellSize * 0.8f))
                                        
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 3, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 4, cellSize * 3), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize, cellSize * 4), size = androidx.compose.ui.geometry.Size(cellSize, cellSize * 2))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 3, cellSize * 5), size = androidx.compose.ui.geometry.Size(cellSize * 2, cellSize))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 5, cellSize * 5), size = androidx.compose.ui.geometry.Size(cellSize, cellSize * 2))
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(cellSize * 4, cellSize * 4), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                        
                                        drawRect(Color(0xFF1B4E8F), topLeft = androidx.compose.ui.geometry.Offset(size/2 - cellSize/2, size/2 - cellSize/2), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                                    }
                                }

                                Text(
                                    text = "BARBERTEAK OFFICIAL - PRODUK",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                    color = Color.Black
                                )
                                Text(
                                    text = "NMID: ID1020304050608",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    // Bank transfer description if bank transfer chosen
                    if (paymentMethod == "TRANSFER") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "INFORMASI REKENING TRANSFER",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TeakHoney)
                                )
                                Text(
                                    text = "Bank Mandiri\nNo. Rekening: 124-00-102030-4\na/n PT Barberteak Indonesia Jaya",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "Silakan transfer tepat sebesar total biaya di bawah ini.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Summary Total Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Pembayaran",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = formatCurrency.format(totalPrice),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = TeakHoney
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Confirm buy button
                    Button(
                        onClick = {
                            isProcessing = true
                            viewModel.checkoutCart(paymentMethod) { success, message ->
                                isProcessing = false
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_cart_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = "Konfirmasi & Bayar Sekarang",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: CAPSTERS LIST
// ---------------------------------------------------------------------------------

@Composable
fun CapstersListScreen(
    viewModel: BarberViewModel,
    onSelectCapsterForBooking: (Int, String) -> Unit
) {
    val capsters by viewModel.allCapsters.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviews.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Daftar Capster Ahli Barberteak",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = "Pilih penata rambut profesional andalan Anda untuk mendapatkan model potongan yang paling sesuai.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(capsters) { capster ->
                var showReviews by remember { mutableStateOf(false) }
                val capsterReviews = allReviews.filter { it.capsterId == capster.id }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(TeakWoodPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = capster.name,
                                    tint = TeakWoodPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = capster.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (capster.status == "Available") StatusGreen.copy(alpha = 0.15f) 
                                                             else StatusAmber.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = capster.status,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (capster.status == "Available") StatusGreen else StatusAmber
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Keahlian: ${capster.specialties}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Pengalaman: ${capster.experience}  •  ⭐ ${String.format(Locale.US, "%.1f", capster.rating)} (${capster.reviewsCount} ulasan)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onSelectCapsterForBooking(capster.id, "STORE") },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Booking Toko", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (capster.supportsHomeService) {
                                        Button(
                                            onClick = { onSelectCapsterForBooking(capster.id, "HOME") },
                                            colors = ButtonDefaults.buttonColors(containerColor = TeakGold),
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Home Service", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    TextButton(
                                        onClick = { showReviews = !showReviews },
                                        modifier = Modifier.height(30.dp).testTag("toggle_reviews_${capster.id}"),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (showReviews) "Tutup" else "Ulasan (${capsterReviews.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TeakHoney
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (showReviews) {
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (capsterReviews.isEmpty()) {
                                    Text(
                                        text = "Belum ada ulasan untuk capster ini.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    capsterReviews.forEach { review ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = review.userName,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = TeakWoodPrimary
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = TeakGold,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = String.format(Locale.US, "%.1f", review.rating),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }
                                                if (review.comment.isNotEmpty()) {
                                                    Text(
                                                        text = "\"${review.comment}\"",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: TANYA AI BARBER
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiScreen(viewModel: BarberViewModel) {
    var query by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<Pair<String, Boolean>>() } // Pair(message, isUser)
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Add initial welcome message if empty
    LaunchedEffect(Unit) {
        if (chatHistory.isEmpty()) {
            chatHistory.add(
                "Halo! Saya AI Barberteak, asisten kecerdasan buatan khusus Barberteak. ✂️\n\nAda yang bisa saya bantu? Saya dapat membantu Anda mengecek ketersediaan & jadwal capster, menginformasikan katalog produk, menjelaskan daftar paket layanan, serta memberikan saran gaya rambut pria yang cocok untuk Anda!" to false
            )
        }
    }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty() || isLoading) return
        chatHistory.add(text to true)
        query = ""
        isLoading = true

        coroutineScope.launch {
            val response = com.example.data.remote.GeminiHelper.askGemini(text)
            chatHistory.add(response to false)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Tanya AI Barberteak",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                color = TeakGold
            )
            Text(
                text = "Konsultasi gaya rambut & perawatan pria secara langsung didukung kecerdasan buatan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Suggestions when there is only the welcome message
        if (chatHistory.size == 1) {
            Text(
                text = "Rekomendasi Pertanyaan Cepat:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TeakHoney)
            )
            val suggestions = listOf(
                "Siapa saja capster yang tersedia saat ini?",
                "Berapa harga paket Royal Treatment & isinya apa saja?",
                "Apa keunggulan Teak & Clay Pomade Premium dan harganya?",
                "Rekomendasi model rambut yang cocok untuk wajah bulat?"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions) { suggestion ->
                    Card(
                        modifier = Modifier
                            .clickableWithBounce { sendMessage(suggestion) }
                            .testTag("ai_suggestion_${suggestion.take(10)}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatHistory) { (message, isUser) ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 2.dp,
                                bottomEnd = if (isUser) 2.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) TeakWoodPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Card(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.widthIn(max = 200.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = TeakGold
                                    )
                                    Text(
                                        text = "AI sedang mengetik...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Tanyakan rekomendasi rambut Anda...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeakGold,
                    focusedLabelColor = TeakGold
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            IconButton(
                onClick = { sendMessage(query) },
                enabled = query.trim().isNotEmpty() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (query.trim().isNotEmpty() && !isLoading) TeakWoodPrimary else MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (query.trim().isNotEmpty() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: BOOKING HISTORY / STATUS TRACKING
// ---------------------------------------------------------------------------------

@Composable
fun BookingHistoryScreen(viewModel: BarberViewModel) {
    val context = LocalContext.current
    val reservations by viewModel.userReservations.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var activeChatReservation by remember { mutableStateOf<ReservationEntity?>(null) }
    var activeReviewReservation by remember { mutableStateOf<ReservationEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Daftar Reservasi & Antrean Anda",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )

        if (reservations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "Empty",
                        tint = TeakWoodLight.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Belum Ada Riwayat Reservasi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Gunakan tombol Reservasi Toko atau Home Service untuk mulai potong rambut.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(reservations) { reservation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = if (reservation.serviceType == "HOME") Icons.Default.DirectionsCar else Icons.Default.Store,
                                        contentDescription = "Tipe",
                                        tint = TeakWoodPrimary
                                    )
                                    Text(
                                        text = if (reservation.serviceType == "HOME") "HOME SERVICE" else "DI TOKO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (reservation.status) {
                                            "PENDING" -> StatusAmber.copy(alpha = 0.15f)
                                            "APPROVED" -> StatusGreen.copy(alpha = 0.15f)
                                            "ON_THE_WAY" -> StatusBlue.copy(alpha = 0.15f)
                                            "IN_PROGRESS" -> TeakWoodPrimary.copy(alpha = 0.15f)
                                            "COMPLETED" -> StatusGreen.copy(alpha = 0.15f)
                                            else -> StatusRed.copy(alpha = 0.15f)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = reservation.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (reservation.status) {
                                                "PENDING" -> StatusAmber
                                                "APPROVED" -> StatusGreen
                                                "ON_THE_WAY" -> StatusBlue
                                                "IN_PROGRESS" -> TeakHoney
                                                "COMPLETED" -> StatusGreen
                                                else -> StatusRed
                                            }
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                            Text(
                                text = reservation.serviceName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            Text(
                                text = "Capster: ${reservation.capsterName}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text = "Jadwal: ${reservation.date} • ${reservation.time}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            if (reservation.serviceType == "HOME" && reservation.homeAddress != null) {
                                Text(
                                    text = "Alamat Kunjungan: ${reservation.homeAddress}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = "Metode Pembayaran",
                                        tint = TeakGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Pembayaran: ${when(reservation.paymentMethod) {
                                            "TRANSFER" -> "Transfer Bank"
                                            "QRIS" -> "QRIS"
                                            else -> "Tunai (Cash)"
                                        }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (reservation.paymentStatus == "PAID") StatusGreen.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Text(
                                        text = if (reservation.paymentStatus == "PAID") "LUNAS" else "BELUM BAYAR",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (reservation.paymentStatus == "PAID") StatusGreen else StatusAmber
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Harga Layanan",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = formatCurrency.format(reservation.servicePrice),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney
                                    )
                                }

                                if (reservation.queueNo != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Tiket Antrean: ${reservation.queueNo}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TeakWoodPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            if (reservation.paymentMethod == "TRANSFER" || reservation.paymentMethod == "QRIS") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Informasi Pembayaran ${reservation.paymentMethod}:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TeakHoney
                                        )
                                        if (reservation.paymentMethod == "TRANSFER") {
                                            Text(
                                                text = "Bank Mandiri: 123-00-0987654-3 a/n BarberTeak",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = TeakWoodPrimary)
                                                Text(
                                                    text = "QRIS Merchant ID: ID10203040506 a/n BarberTeak",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = {
                                                val shareText = """
                                                    === BUKTI RIWAYAT TRANSFER BARBERTEAK ===
                                                    Nama Pelanggan : ${currentUser?.name ?: "Pelanggan"}
                                                    Layanan        : ${reservation.serviceName}
                                                    Metode Bayar   : ${reservation.paymentMethod}
                                                    Status Bayar   : ${reservation.paymentStatus}
                                                    Total Bayar    : ${formatCurrency.format(reservation.servicePrice)}
                                                    Tanggal Booking: ${reservation.date} ${reservation.time}
                                                    =========================================
                                                    Terima kasih telah mempercayakan ketampanan Anda pada Barberteak Luxury Club!
                                                """.trimIndent()
                                                
                                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                }
                                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Bagikan Bukti Transfer")
                                                context.startActivity(shareIntent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("share_proof_${reservation.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Bagikan Bukti Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            if (reservation.serviceType == "HOME" && reservation.homeAddress != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val mapUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(reservation.homeAddress ?: ""))
                                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri)
                                        context.startActivity(mapIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("maps_route_${reservation.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Navigasi Google Maps Alamat",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (reservation.serviceType == "HOME" && (reservation.status == "PENDING" || reservation.status == "APPROVED" || reservation.status == "ON_THE_WAY" || reservation.status == "IN_PROGRESS")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { activeChatReservation = reservation },
                                    colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("contact_capster_${reservation.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hubungi Capster (Chat & Telepon)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            if (reservation.status == "COMPLETED" && !reservation.isReviewed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { activeReviewReservation = reservation },
                                    colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("review_capster_${reservation.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Beri Ulasan & Rating",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeChatReservation != null) {
        HomeServiceChatDialog(
            reservation = activeChatReservation!!,
            viewModel = viewModel,
            onDismiss = { activeChatReservation = null }
        )
    }

    if (activeReviewReservation != null) {
        val res = activeReviewReservation!!
        var rating by remember { mutableStateOf(5) }
        var comment by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { activeReviewReservation = null },
            title = {
                Text(
                    text = "Beri Ulasan & Rating",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                    color = TeakHoney
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bagaimana pengalaman Anda bersama Capster ${res.capsterName} untuk layanan ${res.serviceName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Star Rating selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val starNum = index + 1
                            val isSelected = starNum <= rating
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$starNum Bintang",
                                tint = TeakGold,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { rating = starNum }
                                    .testTag("star_rating_$starNum")
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Tulis Ulasan Anda (Opsional)") },
                        placeholder = { Text("Potongan sangat rapi, sangat ramah...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("review_comment_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReview(
                            reservationId = res.id,
                            capsterId = res.capsterId,
                            capsterName = res.capsterName,
                            userEmail = res.userEmail,
                            userName = res.userName,
                            rating = rating.toFloat(),
                            comment = comment.trim()
                        )
                        activeReviewReservation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_review_button")
                ) {
                    Text("Kirim Ulasan")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeReviewReservation = null }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ---------------------------------------------------------------------------------
// CLIENT VIEW: COMPLAINTS & FEEDBACK (MOBILE JKN STYLE)
// ---------------------------------------------------------------------------------

@Composable
fun ComplaintsScreen(viewModel: BarberViewModel) {
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pusat Layanan Pengaduan & Kritik",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            color = TeakGold
        )
        Text(
            text = "Kami sangat menghargai masukan Anda untuk meningkatkan kualitas layanan home service dan in-store Barberteak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kirim Pengaduan Baru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Tuliskan keluhan atau saran Anda di sini...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("complaint_input")
                )

                Button(
                    onClick = {
                        if (feedbackText.trim().isNotEmpty()) {
                            viewModel.addComplaint(feedbackText)
                            feedbackText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = feedbackText.trim().isNotEmpty(),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("submit_complaint_button")
                ) {
                    Text("Kirim Masukan", fontWeight = FontWeight.Bold)
                }
            }
        }

        // WhatsApp Complaints Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF25D366).copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, Color(0xFF25D366))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF25D366), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kritik & Aduan via WhatsApp",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E7E34)
                    )
                    Text(
                        text = "Kirim masukan atau keluhan langsung ke nomor WhatsApp resmi kami.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val phoneNumber = "6281234567890"
                        val prefilledText = "Halo Admin Barberteak, saya ingin memberikan kritik/saran/aduan mengenai layanan potong rambut."
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${java.net.URLEncoder.encode(prefilledText, "UTF-8")}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Tidak dapat membuka WhatsApp.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.size(36.dp).testTag("whatsapp_complaint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Buka WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Text(
            text = "Kritik & Saran Masuk Terbaru",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 10.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(complaints) { complaint ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = complaint.first,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TeakHoney
                            )
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Feedback",
                                tint = TeakWoodLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = complaint.second,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommissionAndTipCalculator(completedReservations: List<ReservationEntity>) {
    var customTipInput by remember { mutableStateOf("") }
    var commissionRateInput by remember { mutableStateOf("70") } // Default 70% commission
    
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Auto calculated values
    val totalServiceRevenue = completedReservations.sumOf { it.servicePrice }
    val parsedRate = commissionRateInput.toDoubleOrNull() ?: 70.0
    val calculatedCommission = totalServiceRevenue * (parsedRate / 100.0)
    
    val parsedTip = customTipInput.toDoubleOrNull() ?: 0.0
    val totalEarnings = calculatedCommission + parsedTip
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = TeakGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Kalkulator Komisi & Tip Hari Ini",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                    color = TeakHoney
                )
            }
            
            Text(
                text = "Hitung otomatis komisi berdasarkan layanan yang telah Anda selesaikan hari ini ditambah tip tunai dari pelanggan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Layanan Selesai", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${completedReservations.size} Kali",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TeakWoodPrimary
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Omset Layanan", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = formatCurrency.format(totalServiceRevenue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TeakHoney
                        )
                    }
                }
            }
            
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            
            // Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rate input
                OutlinedTextField(
                    value = commissionRateInput,
                    onValueChange = { commissionRateInput = it },
                    label = { Text("Bagi Hasil %") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // Tip input
                OutlinedTextField(
                    value = customTipInput,
                    onValueChange = { customTipInput = it },
                    label = { Text("Tip Tambahan (Rp)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // Quick Tip Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickTips = listOf(10000, 20000, 50000)
                quickTips.forEach { tipAmt ->
                    Box(
                        modifier = Modifier
                            .background(TeakHoney.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .clickable {
                                val current = customTipInput.toDoubleOrNull() ?: 0.0
                                customTipInput = (current + tipAmt).toInt().toString()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+Rp ${String.format("%,.0f", tipAmt.toDouble())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeakHoney,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { customTipInput = "" }) {
                    Text("Reset Tip", fontSize = 11.sp, color = StatusRed)
                }
            }
            
            // Calculation Results
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hak Komisi ($parsedRate%):", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Text(formatCurrency.format(calculatedCommission), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tips Pelanggan:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Text(formatCurrency.format(parsedTip), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TeakGold)
                    }
                    
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Pendapatan Anda:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(
                            text = formatCurrency.format(totalEarnings),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TeakGold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// CAPSTER ROLE: DASHBOARD
// ---------------------------------------------------------------------------------

@Composable
fun CapsterDashboardScreen(viewModel: BarberViewModel) {
    val context = LocalContext.current
    val activeCapster by viewModel.activeCapster.collectAsStateWithLifecycle()
    val reservations by viewModel.capsterReservations.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    var activeChatReservation by remember { mutableStateOf<ReservationEntity?>(null) }
    
    // Split into tabs: 0 = Tugas Hari Ini, 1 = Jadwal & Status
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = "Capster",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selamat Bekerja,",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = activeCapster?.name ?: "Capster Barberteak",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Rating Anda: ⭐ ${activeCapster?.rating ?: 4.8f}  • Keahlian: ${activeCapster?.specialties ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TeakGold
                    )
                }
            }
        }

        // Tab Navigation for incoming appointments vs daily schedule and status
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TeakWoodPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = TeakGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tugas Aktif (${reservations.filter { it.status != "COMPLETED" && it.status != "REJECTED" }.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(imageVector = Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Jadwal & Status", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        if (selectedTab == 0) {
            // Active incoming and processing appointments
            val activeTasks = reservations.filter { it.status != "COMPLETED" && it.status != "REJECTED" }
            
            Text(
                text = "Tugas Cukur Aktif (${activeTasks.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (activeTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "No tasks",
                            tint = TeakWoodLight.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada tugas cukur aktif saat ini",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Antrean baru dari pelanggan akan otomatis tampil di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeTasks) { reservation ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = if (reservation.serviceType == "HOME") Icons.Default.DirectionsCar else Icons.Default.Store,
                                            contentDescription = "Layanan",
                                            tint = TeakGold
                                        )
                                        Text(
                                            text = if (reservation.serviceType == "HOME") "HOME SERVICE (KUNJUNGAN)" else "DI TOKO / BARBER",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TeakHoney
                                        )
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = "Antrean: ${reservation.queueNo ?: "-"}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TeakWoodPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                                Text(
                                    text = reservation.serviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Pelanggan:", style = MaterialTheme.typography.labelSmall)
                                        Text(text = reservation.userName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(text = "HP: ${reservation.userPhone}", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Waktu Booking:", style = MaterialTheme.typography.labelSmall)
                                        Text(text = reservation.time, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(text = reservation.date, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (reservation.serviceType == "HOME" && reservation.homeAddress != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(text = "📍 ALAMAT RUMAH PELANGGAN:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TeakWoodPrimary)
                                            Text(text = reservation.homeAddress, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    val mapUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(reservation.homeAddress ?: ""))
                                                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri)
                                                    context.startActivity(mapIntent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("capster_route_${reservation.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Rute Navigasi Cepat (Google Maps)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                // Dynamic on-demand actions based on booking status
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status Tugas: ${reservation.status}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TeakHoney,
                                        modifier = Modifier.weight(1f)
                                    )

                                    when (reservation.status) {
                                        "PENDING" -> {
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "APPROVED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Terima Tugas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "REJECTED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Tolak", fontSize = 11.sp)
                                            }
                                        }
                                        "APPROVED" -> {
                                            Button(
                                                onClick = {
                                                    val nextStatus = if (reservation.serviceType == "HOME") "ON_THE_WAY" else "IN_PROGRESS"
                                                    viewModel.updateReservationStatus(reservation.id, nextStatus)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("start_trip_${reservation.id}")
                                            ) {
                                                Text(
                                                    text = if (reservation.serviceType == "HOME") "Mulai Perjalanan (Otw)" else "Mulai Cukur",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        "ON_THE_WAY" -> {
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "IN_PROGRESS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("start_cut_${reservation.id}")
                                            ) {
                                                Text("Tiba & Mulai Cukur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        "IN_PROGRESS" -> {
                                            Button(
                                                onClick = { viewModel.updateReservationStatus(reservation.id, "COMPLETED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("finish_cut_${reservation.id}")
                                            ) {
                                                Text("Selesai & Bayar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (reservation.serviceType == "HOME") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { activeChatReservation = reservation },
                                        colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("contact_customer_${reservation.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hubungi Pelanggan (Chat & Telepon)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Schedule & Status update view
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 0. Commission & Tip Calculator Section
                item {
                    val completedTasks = reservations.filter { it.status == "COMPLETED" }
                    CommissionAndTipCalculator(completedReservations = completedTasks)
                }

                // 1. Update Availability Status Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Circle, contentDescription = null, tint = TeakGold, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Status Ketersediaan Anda",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                
                                val statusText = activeCapster?.status ?: "Available"
                                val statusColor = when (statusText) {
                                    "Available" -> StatusGreen
                                    "Busy" -> StatusAmber
                                    else -> StatusRed
                                }
                                Card(colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f))) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Ubah status ketersediaan Anda agar pelanggan mengetahui apakah Anda dapat dipesan untuk reservasi toko atau kunjungan rumah.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statuses = listOf(
                                    "Available" to StatusGreen,
                                    "Busy" to StatusAmber,
                                    "Off" to StatusRed
                                )
                                statuses.forEach { (status, color) ->
                                    val isSelected = activeCapster?.status == status
                                    Button(
                                        onClick = { viewModel.updateActiveCapsterStatus(status) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = if (!isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Schedule timeline & completed tasks
                item {
                    Text(
                        text = "Jadwal Tugas Harian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                val dailySchedule = reservations
                if (dailySchedule.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada jadwal tugas tercatat untuk Anda.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(dailySchedule) { reservation ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (reservation.status == "COMPLETED") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                                 else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                                    Text(text = reservation.time, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TeakHoney))
                                    Text(text = reservation.date.take(6), style = MaterialTheme.typography.labelSmall)
                                }
                                
                                Divider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = reservation.serviceName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "Pelanggan: ${reservation.userName}", style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val isHome = reservation.serviceType == "HOME"
                                        Icon(
                                            imageVector = if (isHome) Icons.Default.DirectionsCar else Icons.Default.Store,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = TeakGold
                                        )
                                        Text(text = if (isHome) "Home Service" else "In-Store", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val color = when (reservation.status) {
                                        "COMPLETED" -> StatusGreen
                                        "PENDING" -> StatusAmber
                                        "REJECTED" -> StatusRed
                                        else -> StatusBlue
                                    }
                                    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
                                        Text(
                                            text = reservation.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color, fontSize = 9.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(text = formatCurrency.format(reservation.servicePrice), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeChatReservation != null) {
        HomeServiceChatDialog(
            reservation = activeChatReservation!!,
            viewModel = viewModel,
            onDismiss = { activeChatReservation = null }
        )
    }
}

// ---------------------------------------------------------------------------------
// ADMIN ROLE: DASHBOARD
// ---------------------------------------------------------------------------------

@Composable
fun FinanceStatCard(
    title: String,
    amount: Double,
    count: Int,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formatCurrency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickableWithBounce { onClick() }
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) TeakGold else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TeakWoodPrimary.copy(alpha = 0.2f) else SurfaceDark
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TeakGold)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AKTIF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = formatCurrency.format(amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isSelected) TeakGold else Color.White
                    )
                )
                Text(
                    text = "$count Selesai",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: BarberViewModel) {
    val reservations by viewModel.allReservations.collectAsStateWithLifecycle()
    val formatCurrency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    val now = System.currentTimeMillis()
    val calendar = java.util.Calendar.getInstance()

    // Current Day Midnight
    calendar.timeInMillis = now
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    val todayMidnight = calendar.timeInMillis

    // Yesterday Midnight
    val yesterdayMidnight = todayMidnight - 24 * 60 * 60 * 1000L

    // Start of 7 Days Ago
    val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L

    // Start of This Month
    calendar.timeInMillis = now
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    val startOfMonth = calendar.timeInMillis

    val completedReservations = reservations.filter { it.status == "COMPLETED" }

    // Today Stats
    val earningsToday = completedReservations.filter { it.createdAt >= todayMidnight }.sumOf { it.servicePrice }
    val countToday = completedReservations.count { it.createdAt >= todayMidnight }

    // Yesterday Stats
    val earningsYesterday = completedReservations.filter { it.createdAt >= yesterdayMidnight && it.createdAt < todayMidnight }.sumOf { it.servicePrice }
    val countYesterday = completedReservations.count { it.createdAt >= yesterdayMidnight && it.createdAt < todayMidnight }

    // This Week Stats
    val earningsThisWeek = completedReservations.filter { it.createdAt >= sevenDaysAgo }.sumOf { it.servicePrice }
    val countThisWeek = completedReservations.count { it.createdAt >= sevenDaysAgo }

    // This Month Stats
    val earningsThisMonth = completedReservations.filter { it.createdAt >= startOfMonth }.sumOf { it.servicePrice }
    val countThisMonth = completedReservations.count { it.createdAt >= startOfMonth }

    // Overall Total Stats
    val totalEarnings = completedReservations.sumOf { it.servicePrice }
    val totalCount = completedReservations.size

    var selectedPeriodTab by remember { mutableStateOf("SEMUA") } // "SEMUA", "HARI_INI", "KEMARIN", "MINGGU_INI", "BULAN_INI", "PENDING"

    val filteredReservations = when (selectedPeriodTab) {
        "HARI_INI" -> reservations.filter { it.status == "COMPLETED" && it.createdAt >= todayMidnight }
        "KEMARIN" -> reservations.filter { it.status == "COMPLETED" && it.createdAt >= yesterdayMidnight && it.createdAt < todayMidnight }
        "MINGGU_INI" -> reservations.filter { it.status == "COMPLETED" && it.createdAt >= sevenDaysAgo }
        "BULAN_INI" -> reservations.filter { it.status == "COMPLETED" && it.createdAt >= startOfMonth }
        "PENDING" -> reservations.filter { it.status == "PENDING" }
        else -> reservations
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upper Analysis Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "IKHTISAR ANALISIS KEUANGAN",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TeakHoney)
                )
                Text(
                    text = "Kelola omset & reservasi salon Anda",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = TeakGold,
                modifier = Modifier.size(24.dp)
            )
        }

        // Horizontal Grid of Financial Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FinanceStatCard(
                    title = "Hari Ini",
                    amount = earningsToday,
                    count = countToday,
                    icon = Icons.Default.Today,
                    color = StatusGreen,
                    isSelected = selectedPeriodTab == "HARI_INI",
                    onClick = { selectedPeriodTab = "HARI_INI" }
                )
            }
            item {
                FinanceStatCard(
                    title = "Kemarin",
                    amount = earningsYesterday,
                    count = countYesterday,
                    icon = Icons.Default.History,
                    color = StatusAmber,
                    isSelected = selectedPeriodTab == "KEMARIN",
                    onClick = { selectedPeriodTab = "KEMARIN" }
                )
            }
            item {
                FinanceStatCard(
                    title = "Minggu Ini",
                    amount = earningsThisWeek,
                    count = countThisWeek,
                    icon = Icons.Default.DateRange,
                    color = StatusBlue,
                    isSelected = selectedPeriodTab == "MINGGU_INI",
                    onClick = { selectedPeriodTab = "MINGGU_INI" }
                )
            }
            item {
                FinanceStatCard(
                    title = "Bulan Ini",
                    amount = earningsThisMonth,
                    count = countThisMonth,
                    icon = Icons.Default.CalendarMonth,
                    color = TeakHoney,
                    isSelected = selectedPeriodTab == "BULAN_INI",
                    onClick = { selectedPeriodTab = "BULAN_INI" }
                )
            }
            item {
                FinanceStatCard(
                    title = "Total Omset",
                    amount = totalEarnings,
                    count = totalCount,
                    icon = Icons.Default.AccountBalanceWallet,
                    color = TeakGold,
                    isSelected = selectedPeriodTab == "SEMUA",
                    onClick = { selectedPeriodTab = "SEMUA" }
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp))

        // Segmented filter capsules / chips
        Text(
            text = "Filter Daftar Reservasi",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        val tabs = listOf(
            "SEMUA" to "Semua",
            "PENDING" to "Persetujuan",
            "HARI_INI" to "Hari Ini",
            "KEMARIN" to "Kemarin",
            "MINGGU_INI" to "Minggu Ini",
            "BULAN_INI" to "Bulan Ini"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tabs) { (value, label) ->
                val isTabSelected = selectedPeriodTab == value
                val badgeCount = when (value) {
                    "PENDING" -> reservations.count { it.status == "PENDING" }
                    "HARI_INI" -> countToday
                    "KEMARIN" -> countYesterday
                    "MINGGU_INI" -> countThisWeek
                    "BULAN_INI" -> countThisMonth
                    else -> reservations.size
                }
                FilterChip(
                    selected = isTabSelected,
                    onClick = { selectedPeriodTab = value },
                    label = {
                        Text(
                            text = if (badgeCount > 0) "$label ($badgeCount)" else label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TeakGold,
                        selectedLabelColor = Color.Black,
                        containerColor = SurfaceDark,
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                )
            }
        }

        // Master Booking List Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedPeriodTab) {
                    "HARI_INI" -> "Transaksi Selesai Hari Ini"
                    "KEMARIN" -> "Transaksi Selesai Kemarin"
                    "MINGGU_INI" -> "Transaksi Selesai Minggu Ini"
                    "BULAN_INI" -> "Transaksi Selesai Bulan Ini"
                    "PENDING" -> "Reservasi Perlu Persetujuan"
                    else -> "Semua Riwayat Reservasi"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            // General statistics for filter
            Text(
                text = "${filteredReservations.size} Data",
                style = MaterialTheme.typography.labelSmall,
                color = TeakHoney
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredReservations.reversed()) { reservation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ORDER ID: #${reservation.id}",
                                fontWeight = FontWeight.Bold,
                                color = TeakGold,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (reservation.status) {
                                        "PENDING" -> StatusAmber.copy(alpha = 0.15f)
                                        "APPROVED" -> StatusGreen.copy(alpha = 0.15f)
                                        "ON_THE_WAY" -> StatusBlue.copy(alpha = 0.15f)
                                        "IN_PROGRESS" -> TeakWoodPrimary.copy(alpha = 0.15f)
                                        "COMPLETED" -> StatusGreen.copy(alpha = 0.15f)
                                        else -> StatusRed.copy(alpha = 0.15f)
                                    }
                                )
                            ) {
                                Text(
                                    text = reservation.status,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when (reservation.status) {
                                            "PENDING" -> StatusAmber
                                            "APPROVED" -> StatusGreen
                                            "ON_THE_WAY" -> StatusBlue
                                            "IN_PROGRESS" -> TeakHoney
                                            "COMPLETED" -> StatusGreen
                                            else -> StatusRed
                                        }
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                        Text(
                            text = "${reservation.serviceName} (${if (reservation.serviceType == "HOME") "Home Service" else "Store"})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Pelanggan:", style = MaterialTheme.typography.labelSmall)
                                Text(reservation.userName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(reservation.userEmail, style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Capster Terpilih:", style = MaterialTheme.typography.labelSmall)
                                Text(reservation.capsterName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("Tiket: ${reservation.queueNo ?: "-"}", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (reservation.status == "PENDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.updateReservationStatus(reservation.id, "APPROVED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp).testTag("approve_btn_${reservation.id}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Setujui", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.updateReservationStatus(reservation.id, "REJECTED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Tolak", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// FLOATING AI BUBBLE WIDGET
// ---------------------------------------------------------------------------------

@Composable
fun FloatingAiBubble(
    viewModel: BarberViewModel,
    isOpen: Boolean,
    onToggle: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val chatHistory = remember { mutableStateListOf<Pair<String, Boolean>>() } // Pair(message, isUser)
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Add initial welcome message if empty
    LaunchedEffect(Unit) {
        if (chatHistory.isEmpty()) {
            chatHistory.add(
                "Halo! Saya AI Barberteak. ✂️\n\nAda yang bisa saya bantu? Silakan tanyakan ketersediaan capster, katalog produk, harga layanan, atau saran model rambut!" to false
            )
        }
    }

    // Auto scroll to bottom
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isEmpty() || isLoading) return
        chatHistory.add(text to true)
        query = ""
        isLoading = true

        coroutineScope.launch {
            val response = com.example.data.remote.GeminiHelper.askGemini(text)
            chatHistory.add(response to false)
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (isOpen) {
            // Chat window card
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .height(440.dp)
                    .padding(bottom = 70.dp) // Leave room for the bubble button below
                    .border(1.dp, TeakGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .testTag("floating_ai_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TeakWoodPrimary, Color(0xFF1E1712))))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TeakGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = TeakGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "AI Barberteak",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusGreen)
                                    )
                                    Text(
                                        text = "Asisten Online",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatHistory) { (message, isUser) ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 2.dp,
                                        bottomEnd = if (isUser) 2.dp else 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) TeakWoodPrimary else SurfaceVariantDark
                                    ),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isUser) Color.White else OnBackgroundDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = TeakGold
                                    )
                                    Text(
                                        text = "AI sedang mengetik...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnBackgroundDark.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Suggestions row
                    if (chatHistory.size == 1) {
                        val suggestions = listOf("Cek Capster?", "Katalog Produk?", "Layanan & Harga?")
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                Card(
                                    modifier = Modifier.clickableWithBounce {
                                        val fullText = when(suggestion) {
                                            "Cek Capster?" -> "Siapa saja capster yang tersedia saat ini?"
                                            "Katalog Produk?" -> "Apa saja katalog produk premium Barberteak?"
                                            else -> "Berapa harga paket layanan potong rambut?"
                                        }
                                        sendMessage(fullText)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, TeakGold.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TeakGold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Tanya AI...", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TeakGold,
                                focusedLabelColor = TeakGold,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("floating_ai_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        IconButton(
                            onClick = { sendMessage(query) },
                            enabled = query.trim().isNotEmpty() && !isLoading,
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (query.trim().isNotEmpty()) TeakWoodPrimary else Color.Gray.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Kirim",
                                tint = if (query.trim().isNotEmpty()) Color.White else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Circular Bubble Button
        Card(
            modifier = Modifier
                .size(56.dp)
                .clickableWithBounce { onToggle() }
                .testTag("floating_ai_bubble_button"),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = TeakWoodPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(TeakWoodPrimary, Color(0xFF1E1712))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOpen) Icons.Default.Close else Icons.Default.SmartToy,
                    contentDescription = "Tanya AI",
                    tint = TeakGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// NEW WALLET, RECIPIENT & NOTIFICATION DIALOG COMPOSABLES
// ---------------------------------------------------------------------------------

@Composable
fun WalletAndTransactionsCard(
    user: UserEntity?,
    transactions: List<TransactionEntity>,
    onTopUpClick: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Saldo & Top Up Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = TeakGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "SALDO DOMPET",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        val formattedBalance = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(user?.balance ?: 0.0)
                        Text(
                            text = formattedBalance,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TeakGold
                            )
                        )
                    }
                }

                Button(
                    onClick = onTopUpClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("topup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Top Up",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Top Up",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Recent Financial Activity Header
            Text(
                text = "Aktivitas Keuangan Terbaru",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada riwayat transaksi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transactions.take(3).forEach { transaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableWithBounce { onTransactionClick(transaction) }
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (transaction.type == "TOPUP") StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (transaction.type == "TOPUP") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = transaction.type,
                                        tint = if (transaction.type == "TOPUP") StatusGreen else StatusRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (transaction.type == "TOPUP") "Top Up Saldo" else transaction.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${transaction.dateStr} • ${transaction.paymentMethod}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)
                            Text(
                                text = "${if (transaction.type == "TOPUP") "+" else "-"} $formattedAmount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (transaction.type == "TOPUP") StatusGreen else StatusRed
                            )
                        }
                    }
                    
                    if (transactions.size > 3) {
                        Text(
                            text = "* Klik transaksi di atas untuk melihat & membagikan bukti bayar",
                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    notifications: List<NotificationEntity>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pusat Notifikasi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TeakGold
                    )
                    if (notifications.any { !it.isRead }) {
                        TextButton(onClick = onMarkAllRead) {
                            Text(
                                text = "Baca Semua",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TeakHoney
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada notifikasi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notification ->
                            val color = when (notification.type) {
                                "RESERVATION" -> TeakGold
                                "PAYMENT" -> StatusGreen
                                else -> StatusBlue
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (notification.isRead) Color.Transparent else color.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (notification.isRead) Color.Transparent else color,
                                            shape = CircleShape
                                        )
                                        .align(Alignment.CenterVertically)
                                )
                                Column {
                                    Text(
                                        text = notification.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (notification.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = notification.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary)
                ) {
                    Text("Tutup", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TopUpDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("BCA TRANSFER") }
    val presetAmounts = listOf(50000.0, 100000.0, 200000.0, 500000.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Top Up Saldo Barberteak",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TeakGold
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Amount text input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Jumlah Top Up (Rp)") },
                    placeholder = { Text("Masukkan nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeakGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.take(2).forEach { amt ->
                        val formatted = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amt).substringBefore(",")
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { amountText = amt.toInt().toString() },
                            colors = CardDefaults.cardColors(
                                containerColor = if (amountText == amt.toInt().toString()) TeakGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, if (amountText == amt.toInt().toString()) TeakGold else Color.Transparent)
                        ) {
                            Text(
                                text = formatted,
                                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.drop(2).forEach { amt ->
                        val formatted = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amt).substringBefore(",")
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { amountText = amt.toInt().toString() },
                            colors = CardDefaults.cardColors(
                                containerColor = if (amountText == amt.toInt().toString()) TeakGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, if (amountText == amt.toInt().toString()) TeakGold else Color.Transparent)
                        ) {
                            Text(
                                text = formatted,
                                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Payment Method
                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "BCA TRANSFER" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMethod == "BCA TRANSFER") TeakGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (selectedMethod == "BCA TRANSFER") TeakGold else Color.Transparent)
                    ) {
                        Text(
                            text = "BCA Transfer",
                            modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "QRIS" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMethod == "QRIS") TeakGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, if (selectedMethod == "QRIS") TeakGold else Color.Transparent)
                    ) {
                        Text(
                            text = "QRIS Digital",
                            modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, selectedMethod)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                        modifier = Modifier.weight(1f),
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Konfirmasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    user: UserEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, TeakGold)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Luxury Stamp Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = StatusGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "TRANSAKSI BERHASIL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = StatusGreen
                    )
                }

                Divider(color = TeakGold.copy(alpha = 0.3f), thickness = 2.dp)

                // Invoice Styling Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BUKTI TRANSAKSI DIGITAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                        color = TeakHoney,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("No. Referensi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(transaction.referenceNo, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Waktu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(transaction.dateStr, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nama", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(user?.name ?: "Pelanggan", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tipe Transaksi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(if (transaction.type == "TOPUP") "Top Up Saldo" else "Pembayaran Layanan", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Metode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(transaction.paymentMethod, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL JUMLAH",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)
                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = TeakGold)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TeakGold),
                        border = BorderStroke(1.dp, TeakGold)
                    ) {
                        Text("Tutup")
                    }
                    
                    Button(
                        onClick = {
                            val shareText = """
                                === BUKTI TRANSAKSI BARBERTEAK ===
                                No. Referensi : ${transaction.referenceNo}
                                Tanggal       : ${transaction.dateStr}
                                Pelanggan     : ${user?.name ?: "Pelanggan Barberteak"}
                                Tipe          : ${if (transaction.type == "TOPUP") "Top Up Saldo" else "Pembayaran"}
                                Jumlah        : ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(transaction.amount)}
                                Metode        : ${transaction.paymentMethod}
                                Status        : SUKSES
                                
                                Terima kasih telah menggunakan layanan Barberteak Luxury & Gold!
                            """.trimIndent()
                            
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Bagikan Bukti Transaksi")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1.2f).testTag("share_invoice_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSettingsDialog(
    currentUser: UserEntity?,
    viewModel: BarberViewModel? = null,
    onDismiss: () -> Unit,
    onSwitchAccount: (String, String?, String?) -> Unit,
    onLogout: () -> Unit
) {
    var isAddingAccount by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("CLIENT") } // "CLIENT", "CAPSTER", "ADMIN"

    val coroutineScope = rememberCoroutineScope()

    var isChangingPassword by remember { mutableStateOf(false) }
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordSuccessMsg by remember { mutableStateOf("") }
    var passwordErrorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, TeakGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = TeakGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Pengaturan Akun",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TeakGold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                if (!isAddingAccount) {
                    // Current Active Account Info
                    Text(
                        text = "AKUN AKTIF SAAT INI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TeakHoney
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(TeakWoodPrimary.copy(alpha = 0.2f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (currentUser?.role) {
                                        "ADMIN" -> Icons.Default.AdminPanelSettings
                                        "CAPSTER" -> Icons.Default.ContentCut
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = TeakGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.name ?: "Pengguna",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = currentUser?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = TeakGold.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = currentUser?.role ?: "CLIENT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TeakGold, fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Account Switching List
                    Text(
                        text = "BERALIH AKUN CEPAT (UJI COBA)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TeakHoney
                    )

                    val testAccounts = listOf(
                        Triple("budi@gmail.com", "Budi (Capster Senior)", "CAPSTER"),
                        Triple("agus@gmail.com", "Agus (Capster Junior)", "CAPSTER"),
                        Triple("yudhaactaffian007@gmail.com", "Yudha Actaffian (VIP Client)", "CLIENT"),
                        Triple("admin@barberteak.com", "Yudha Actaffian (Admin)", "ADMIN")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        testAccounts.forEach { (email, name, role) ->
                            val isCurrent = currentUser?.email?.lowercase() == email.lowercase()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isCurrent) TeakGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) TeakGold else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickableWithBounce {
                                        if (!isCurrent) {
                                            onSwitchAccount(email, name, role)
                                            onDismiss()
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (role) {
                                        "ADMIN" -> Icons.Default.AdminPanelSettings
                                        "CAPSTER" -> Icons.Default.ContentCut
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = if (isCurrent) TeakGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) TeakGold else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = StatusGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons
                    Button(
                        onClick = { isAddingAccount = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambahkan Akun Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (!isChangingPassword) {
                        Button(
                            onClick = { isChangingPassword = true },
                            modifier = Modifier.fillMaxWidth().testTag("show_change_password_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TeakWoodPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ubah Kata Sandi Akun", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "UBAH KATA SANDI",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TeakHoney
                                )
                                OutlinedTextField(
                                    value = oldPasswordInput,
                                    onValueChange = { oldPasswordInput = it },
                                    label = { Text("Kata Sandi Lama") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TeakGold, focusedLabelColor = TeakGold)
                                )
                                OutlinedTextField(
                                    value = newPasswordInput,
                                    onValueChange = { newPasswordInput = it },
                                    label = { Text("Kata Sandi Baru") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TeakGold, focusedLabelColor = TeakGold)
                                )
                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = { Text("Konfirmasi Kata Sandi Baru") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TeakGold, focusedLabelColor = TeakGold)
                                )

                                if (passwordSuccessMsg.isNotEmpty()) {
                                    Text(text = passwordSuccessMsg, color = StatusGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                if (passwordErrorMsg.isNotEmpty()) {
                                    Text(text = passwordErrorMsg, color = StatusRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (oldPasswordInput.isEmpty() || newPasswordInput.isEmpty() || confirmPasswordInput.isEmpty()) {
                                                passwordErrorMsg = "Semua kolom harus diisi!"
                                                passwordSuccessMsg = ""
                                            } else if (newPasswordInput != confirmPasswordInput) {
                                                passwordErrorMsg = "Konfirmasi kata sandi baru tidak cocok!"
                                                passwordSuccessMsg = ""
                                            } else {
                                                val email = currentUser?.email ?: ""
                                                coroutineScope.launch {
                                                    val success = viewModel?.resetPassword(email, newPasswordInput) ?: false
                                                    if (success) {
                                                        passwordSuccessMsg = "Kata sandi berhasil diperbarui!"
                                                        passwordErrorMsg = ""
                                                        oldPasswordInput = ""
                                                        newPasswordInput = ""
                                                        confirmPasswordInput = ""
                                                        isChangingPassword = false
                                                    } else {
                                                        passwordErrorMsg = "Gagal memperbarui kata sandi!"
                                                        passwordSuccessMsg = ""
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                        modifier = Modifier.weight(1f).testTag("save_password_button")
                                    ) {
                                        Text("Simpan", color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            isChangingPassword = false
                                            oldPasswordInput = ""
                                            newPasswordInput = ""
                                            confirmPasswordInput = ""
                                            passwordSuccessMsg = ""
                                            passwordErrorMsg = ""
                                        },
                                        modifier = Modifier.weight(1f).testTag("cancel_password_button")
                                    ) {
                                        Text("Batal")
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, StatusRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Keluar (Log Out)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                } else {
                    // Add Account Form
                    Text(
                        text = "TAMBAH AKUN BARU",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = TeakHoney
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Baru (Gmail/Barberteak)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TeakGold, focusedLabelColor = TeakGold)
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TeakGold, focusedLabelColor = TeakGold)
                    )

                    Text(
                        text = "Pilih Peran Akun:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CLIENT" to "Pelanggan", "CAPSTER" to "Capster", "ADMIN" to "Admin").forEach { (roleKey, label) ->
                            val isSelected = newRole == roleKey
                            Button(
                                onClick = { newRole = roleKey },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) TeakWoodPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isAddingAccount = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (newEmail.trim().isNotEmpty()) {
                                    onSwitchAccount(newEmail.trim(), newName.trim(), newRole)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = TeakGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            enabled = newEmail.trim().isNotEmpty()
                        ) {
                            Text("Simpan & Beralih", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// HOME SERVICE COMMUNICATION: CHAT, WHATSAPP & APP CALL (SIMULATED VOIP)
// ---------------------------------------------------------------------------------

fun openWhatsApp(context: android.content.Context, phone: String, message: String) {
    var formattedPhone = phone.trim()
    if (formattedPhone.startsWith("0")) {
        formattedPhone = "62" + formattedPhone.substring(1)
    } else if (!formattedPhone.startsWith("+") && !formattedPhone.startsWith("62")) {
        formattedPhone = "62" + formattedPhone
    }
    val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${android.net.Uri.encode(message)}"
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Aplikasi WhatsApp tidak terpasang.", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SimulatedVoipCallDialog(
    recipientName: String,
    onDismiss: () -> Unit
) {
    var callStatus by remember { mutableStateOf("Menghubungi...") }
    var durationSeconds by remember { mutableStateOf(0) }
    var isCallActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        callStatus = "Tersambung"
        while (isCallActive) {
            kotlinx.coroutines.delay(1000)
            durationSeconds++
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2124)),
            border = BorderStroke(1.dp, TeakGold.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "PANGGILAN SUARA APLIKASI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TeakGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (callStatus == "Tersambung") 1.2f else 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .background(TeakGold.copy(alpha = 0.1f), shape = CircleShape)
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(TeakWoodPrimary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Text(
                    text = recipientName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (callStatus == "Tersambung") "Tersambung ($durationText)" else callStatus,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (callStatus == "Tersambung") StatusGreen else Color.LightGray,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                IconButton(
                    onClick = {
                        isCallActive = false
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(StatusRed, shape = CircleShape)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Telepon",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeServiceChatDialog(
    reservation: ReservationEntity,
    viewModel: BarberViewModel,
    onDismiss: () -> Unit
) {
    val messages by viewModel.getChatMessages(reservation.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeRole by viewModel.currentActiveRole.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showCallDialog by remember { mutableStateOf(false) }

    val recipientName = if (activeRole == "CAPSTER") {
        reservation.userName
    } else {
        reservation.capsterName
    }

    val recipientPhone = if (activeRole == "CAPSTER") {
        reservation.userPhone
    } else {
        "08129876543"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, TeakGold)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TeakWoodPrimary)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = recipientName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Home Service • Online",
                                style = MaterialTheme.typography.labelSmall.copy(color = TeakGold, fontSize = 9.sp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showCallDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                                .testTag("call_capster_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Panggilan Aplikasi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val greeting = "Halo $recipientName, saya ${if (activeRole == "CAPSTER") "Capster" else "Pelanggan"} dari pesanan Barber Teak Home Service Anda."
                                openWhatsApp(context, recipientPhone, greeting)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF25D366), shape = CircleShape)
                                .testTag("whatsapp_capster_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                val listState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mulai obrolan untuk koordinasi alamat & kedatangan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                val isMyMessage = if (activeRole == "CAPSTER") {
                                    msg.senderEmail == "capster@barberteak.com"
                                } else if (activeRole == "ADMIN") {
                                    msg.senderEmail == "admin@barberteak.com"
                                } else {
                                    msg.senderEmail == (currentUser?.email ?: "")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMyMessage) 12.dp else 0.dp,
                                            bottomEnd = if (isMyMessage) 0.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMyMessage) TeakWoodPrimary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            if (!isMyMessage) {
                                                Text(
                                                    text = msg.senderName,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TeakHoney),
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = msg.messageText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp,
                                                    color = if (isMyMessage) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ketik pesan...", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TeakGold,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(reservation.id, inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(TeakGold, shape = CircleShape)
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCallDialog) {
        SimulatedVoipCallDialog(
            recipientName = recipientName,
            onDismiss = { showCallDialog = false }
        )
    }
}

