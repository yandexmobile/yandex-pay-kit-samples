package com.yandex.pay.kit.sample.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.yandex.pay.facade.api.YPay
import com.yandex.pay.kit.sample.SecureHardware
import com.yandex.pay.quickpay.api.QuickPayAuthorizationState
import com.yandex.pay.quickpay.api.YandexPaymentMethodsWidget
import com.yandex.pay.quickpay.api.facade.quickPay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val QR_SIZE_PX = 512

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CpqrScreen(
    sessionIdFlow: MutableStateFlow<String?>,
    paymentEnabledFlow: MutableStateFlow<Boolean?>,
    resultFlow: MutableStateFlow<String?>,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val sessionId by sessionIdFlow.collectAsState()
    val paymentEnabled by paymentEnabledFlow.collectAsState()
    val result by resultFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val quickPaySupported = remember { YPay.quickPay.isSupported }

    LaunchedEffect(quickPaySupported) {
        if (quickPaySupported) {
            runCatching { YPay.quickPay.isQuickPaymentEnabled() }
                .onSuccess { paymentEnabledFlow.value = it.value }
        } else {
            paymentEnabledFlow.value = false
        }
    }

    val authorized = remember(paymentEnabled, result) {
        val state = YPay.quickPay.getAuthorizationState()
        state is QuickPayAuthorizationState.SecurityCheckCompleted ||
            state is QuickPayAuthorizationState.AccountAuthorized ||
            state is QuickPayAuthorizationState.SecurityCheckRequired
    }

    val qrBitmap = remember(sessionId) { sessionId?.let(::generateQrBitmap) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quick Pay (CPQR)",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Customer-Presented QR: SDK issues a payment session, encodes it into QR, " +
                    "cashier scans — payment is processed on Yandex Pay side. Requires " +
                    "a physical device with TEE/StrongBox (DPoP).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PaymentMethodsCard()

            QrPreviewCard(qrBitmap = qrBitmap, enabled = paymentEnabled == true)

            CpqrStatusSection(paymentEnabled = paymentEnabled, result = result)

            ActionsSection(
                quickPaySupported = quickPaySupported,
                paymentEnabled = paymentEnabled,
                onEnable = {
                    coroutineScope.launch {
                        runCatching { YPay.quickPay.enableQuickPayment() }
                            .onSuccess { paymentEnabledFlow.value = true }
                    }
                },
                onRefresh = {
                    coroutineScope.launch {
                        runCatching { YPay.quickPay.getPaymentSessionId() }
                            .onSuccess { sessionIdFlow.value = it }
                    }
                },
                onDisable = {
                    coroutineScope.launch {
                        runCatching { YPay.quickPay.disableQuickPayment() }
                            .onSuccess {
                                paymentEnabledFlow.value = false
                                sessionIdFlow.value = null
                            }
                    }
                },
            )

            if (authorized) {
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Payment methods widget",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AndroidView(
                factory = { ctx -> YandexPaymentMethodsWidget(ctx) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QrPreviewCard(qrBitmap: Bitmap?, enabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                qrBitmap != null -> Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR code for payment",
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp),
                    )
                    Text(
                        text = if (enabled) {
                            "Click 'Refresh QR' to start a session"
                        } else {
                            "Enable quick payment to generate QR"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CpqrStatusSection(paymentEnabled: Boolean?, result: String?) {
    when (paymentEnabled) {
        true -> CpqrBanner(
            icon = Icons.Filled.CheckCircle,
            text = "Quick payment enabled",
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        false -> CpqrBanner(
            icon = Icons.Filled.Info,
            text = "Quick payment is not enabled",
            container = MaterialTheme.colorScheme.surfaceVariant,
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        null -> Unit
    }
    when (result) {
        "Success" -> CpqrBanner(
            icon = Icons.Filled.CheckCircle,
            text = "Last payment: Success",
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        "Failure" -> CpqrBanner(
            icon = Icons.Filled.Warning,
            text = "Last payment: Failure",
            container = MaterialTheme.colorScheme.errorContainer,
            onContainer = MaterialTheme.colorScheme.onErrorContainer,
        )
        null -> Unit
    }
}

@Composable
private fun ActionsSection(
    quickPaySupported: Boolean,
    paymentEnabled: Boolean?,
    onEnable: () -> Unit,
    onRefresh: () -> Unit,
    onDisable: () -> Unit,
) {
    SectionCard(title = "Actions") {
        if (!quickPaySupported) {
            Text(
                text = SecureHardware.UNAVAILABLE_MESSAGE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (paymentEnabled != true) {
            Button(
                onClick = onEnable,
                enabled = quickPaySupported,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = "Enable quick payment",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (paymentEnabled == true) {
            Button(
                onClick = onRefresh,
                enabled = quickPaySupported,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Refresh QR",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(
                onClick = onDisable,
                enabled = quickPaySupported,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = "Disable quick payment",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun CpqrBanner(
    icon: ImageVector,
    text: String,
    container: Color,
    onContainer: Color,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = onContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer,
            )
        }
    }
}

private fun generateQrBitmap(sessionId: String): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    val matrix = MultiFormatWriter().encode(
        sessionId,
        BarcodeFormat.QR_CODE,
        QR_SIZE_PX,
        QR_SIZE_PX,
        hints,
    )
    return BarcodeEncoder().createBitmap(matrix)
}
