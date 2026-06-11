package com.yandex.pay.kit.sample.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.pay.auth.YPayAuthResult
import com.yandex.pay.auth.api.auth
import com.yandex.pay.auth.api.state.YPayAuthorizationState
import com.yandex.pay.facade.api.YPay
import com.yandex.pay.inapp.api.YPayInAppWidget
import com.yandex.pay.inapp.api.payInApp
import com.yandex.pay.kit.sample.BuildConfig
import com.yandex.pay.kit.sample.SecureHardware
import com.yandex.pay.kit.sample.network.PaymentUrlCreator
import com.yandex.pay.payment.PayOrder
import com.yandex.pay.payment.PaymentData
import com.yandex.pay.session.PaymentMethodType
import com.yandex.pay.withredirect.api.launcher.YPayContractParams
import com.yandex.pay.withredirect.api.launcher.YPayLauncher
import com.yandex.pay.withredirect.api.session.PaymentSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayWidgetScreen(
    isAuthorized: StateFlow<Boolean>,
    paymentSession: PaymentSession,
    yPayLauncher: YPayLauncher,
    payResultFlow: MutableStateFlow<String?>,
    onBack: () -> Unit,
) {
    val authorized by isAuthorized.collectAsState()
    val payAuthState by YPay.auth.payAuthState.collectAsState()
    val payResult by payResultFlow.collectAsState()
    val context = LocalContext.current
    val inAppSupported = remember { YPay.payInApp.isSupported }

    var inAppEnabled by remember { mutableStateOf(YPay.payInApp.isActive) }
    var orderAmount by remember { mutableStateOf(DEFAULT_AMOUNT) }
    var currencyCode by remember { mutableStateOf(DEFAULT_CURRENCY) }
    var widgetEpoch by remember { mutableStateOf(0) }
    var isPaying by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val paymentUrlCreator = remember { PaymentUrlCreator() }

    val signInLauncher = rememberLauncherForActivityResult(YPay.auth.getAuthContract()) {}

    // Surface every auth result event as a snackbar - same observability pattern
    // as the reference PayActivity's observeAuthState().
    LaunchedEffect(Unit) {
        YPay.auth.authResultEvents.collect { result ->
            val label = when (result) {
                is YPayAuthResult.Success -> "Auth: Success"
                is YPayAuthResult.Failure -> "Auth: Failure" +
                    (result.message?.let { " ($it)" } ?: "")
                YPayAuthResult.Cancel -> "Auth: Cancelled"
            }
            snackbarHostState.showSnackbar(label)
        }
    }

    fun signIn() {
        if (inAppSupported) {
            signInLauncher.launch(Unit)
        } else {
            Toast.makeText(context, SecureHardware.UNAVAILABLE_MESSAGE, Toast.LENGTH_LONG).show()
        }
    }

    fun pay() {
        if (isPaying) return
        if (orderAmount.isBlank()) {
            payResultFlow.value = "Enter an order amount first"
            return
        }
        isPaying = true
        payResultFlow.value = "Creating payment URL..."
        coroutineScope.launch(Dispatchers.IO) {
            val result = paymentUrlCreator.createPaymentUrl(
                baseUrl = BuildConfig.PAYMENT_BASE_URL,
                merchantApiKey = BuildConfig.MERCHANT_API_KEY,
                amount = orderAmount,
                currencyCode = currencyCode,
                paymentMethods = DEFAULT_PAYMENT_METHODS.map { it.name },
            )
            withContext(Dispatchers.Main) {
                isPaying = false
                result.onSuccess { url ->
                    yPayLauncher.launch(
                        YPayContractParams(
                            paymentSession = paymentSession,
                            paymentData = PaymentData(paymentUrl = url),
                        ),
                    )
                }.onFailure { error ->
                    payResultFlow.value = "Error: ${error.message}"
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pay Widget (In-App)",
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionCard(title = "Quick Pay Online") {
                StatusRow(
                    label = "isSupported",
                    value = inAppSupported.toString(),
                    hint = "Whether this device can run in-app payments at all " +
                        "(requires secure hardware keystore). Decided once at SDK init.",
                )
                StatusRow(
                    label = "isActive",
                    value = inAppEnabled.toString(),
                    hint = "Whether in-app pay is currently turned on. " +
                        "Controlled by the switch below.",
                )
                StatusRow(
                    label = "payAuthState",
                    value = payAuthState.toShortName(),
                    hint = "Reactive auth state from YPay.auth.payAuthState. " +
                        "Widget content (cashback, saved cards) depends on it.",
                )
                if (!authorized) {
                    FilledTonalButton(
                        onClick = { signIn() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Sign in via Yandex") }
                } else {
                    AuthorizedIndicator()
                }
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(maxWidth = constraints.maxWidth + 32),
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        },
                    // widgetEpoch is read to make AndroidView re-create when toggled
                    factory = { ctx ->
                        @Suppress("UNUSED_EXPRESSION") widgetEpoch
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                            addView(
                                YPayInAppWidget(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                    )
                                    setOrder(
                                        PayOrder(
                                            amount = orderAmount.toBigDecimalOrNull()
                                                ?: BigDecimal(DEFAULT_AMOUNT),
                                            currencyCode = currencyCode,
                                        ),
                                    )
                                },
                            )
                            if (!inAppSupported) {
                                // hack for blocking click on emulator
                                addView(
                                    View(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                        )
                                        setBackgroundColor(0x03000000)
                                        setOnClickListener {
                                            Toast.makeText(
                                                context,
                                                SecureHardware.UNAVAILABLE_MESSAGE,
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    },
                                )
                            }
                        }
                    },
                    update = { container ->
                        // Push current order into the existing widget without re-creation.
                        val widget = (0 until container.childCount)
                            .map { container.getChildAt(it) }
                            .filterIsInstance<YPayInAppWidget>()
                            .firstOrNull()
                        widget?.setOrder(
                            PayOrder(
                                amount = orderAmount.toBigDecimalOrNull()
                                    ?: BigDecimal(DEFAULT_AMOUNT),
                                currencyCode = currencyCode,
                            ),
                        )
                    },
                )
            }

            OrderCard(
                amount = orderAmount,
                onAmountChange = { orderAmount = it },
                currency = currencyCode,
                onCurrencyChange = { currencyCode = it },
                onReset = {
                    orderAmount = DEFAULT_AMOUNT
                    currencyCode = DEFAULT_CURRENCY
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Order reset to $DEFAULT_AMOUNT $DEFAULT_CURRENCY",
                        )
                    }
                },
            )

            PayCard(
                payResult = payResult,
                isPaying = isPaying,
                canPay = orderAmount.isNotBlank() && !isPaying,
                onPay = { pay() },
            )

            SectionCard(title = "In-App Pay") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (inAppEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (inAppEnabled) {
                                "Widget is shown and merchant marked in-app flow as available."
                            } else {
                                "Widget will hide. SDK will use simple redirect payment."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = inAppEnabled,
                        enabled = inAppSupported,
                        onCheckedChange = { enabled ->
                            inAppEnabled = enabled
                            if (enabled) {
                                YPay.payInApp.enable()
                            } else {
                                YPay.payInApp.disable()
                            }
                            // Force-recreate the widget so it picks up the new state.
                            widgetEpoch++
                        },
                    )
                }
                if (!inAppSupported) {
                    Text(
                        text = "In-app pay is not supported on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    onReset: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "ORDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = onReset,
                    shape = MaterialTheme.shapes.small,
                ) { Text("Reset") }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { new -> onAmountChange(new.filter(Char::isDigit)) },
                placeholder = { Text(DEFAULT_AMOUNT) },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "CURRENCY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CURRENCIES.forEach { code ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { onCurrencyChange(code) },
                        label = { Text(code) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
            Text(
                text = "Each change calls YPayInAppWidget.setOrder() — the widget re-fetches " +
                    "its layout from the backend with the new order.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PayCard(
    payResult: String?,
    isPaying: Boolean,
    canPay: Boolean,
    onPay: () -> Unit,
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
                text = "PAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = null,
                    tint = if (payResult == null) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    text = payResult ?: "No transaction yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Button(
                onClick = onPay,
                enabled = canPay,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                if (isPaying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Pay")
                }
            }
            Text(
                text = "Creates a payment URL, then launches " +
                    "YPayLauncher with the current order amount and currency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    hint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append(label)
                }
                append(": ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(value)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuthorizedIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = "Signed in",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

private fun YPayAuthorizationState.toShortName(): String = when (this) {
    is YPayAuthorizationState.Authorized -> "Authorized"
    is YPayAuthorizationState.Unauthorized -> "Unauthorized"
}

private const val DEFAULT_AMOUNT = "100"
private const val DEFAULT_CURRENCY = "RUB"
private val CURRENCIES = listOf("RUB", "USD", "EUR")
private val DEFAULT_PAYMENT_METHODS = listOf(PaymentMethodType.CARD, PaymentMethodType.SPLIT)
