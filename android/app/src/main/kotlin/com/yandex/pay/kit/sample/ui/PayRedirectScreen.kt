package com.yandex.pay.kit.sample.ui

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.pay.kit.sample.BuildConfig
import com.yandex.pay.kit.sample.network.PaymentUrlCreator
import com.yandex.pay.payment.PaymentData
import com.yandex.pay.session.PaymentMethodType
import com.yandex.pay.withredirect.api.button.YPayButton
import com.yandex.pay.withredirect.api.launcher.YPayContractParams
import com.yandex.pay.withredirect.api.launcher.YPayLauncher
import com.yandex.pay.withredirect.api.session.PaymentSession
import com.yandex.pay.withredirect.api.session.SessionListenerArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayRedirectScreen(
    paymentSession: PaymentSession,
    yPayLauncher: YPayLauncher,
    payResultFlow: MutableStateFlow<String?>,
    onBack: () -> Unit,
) {
    val payResult by payResultFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var paymentUrl by remember { mutableStateOf("") }
    var orderAmount by remember { mutableStateOf("100") }
    var isCreatingUrl by remember { mutableStateOf(false) }
    var cardChecked by remember { mutableStateOf(true) }
    var splitChecked by remember { mutableStateOf(true) }
    var cornerRadius by remember { mutableFloatStateOf(DEFAULT_CORNER_RADIUS) }
    var widthFixedDp by remember { mutableFloatStateOf(DEFAULT_WIDTH_DP) }

    val paymentUrlCreator = remember { PaymentUrlCreator() }

    val selectedMethods = remember(cardChecked, splitChecked) {
        buildList {
            if (cardChecked) add(PaymentMethodType.CARD)
            if (splitChecked) add(PaymentMethodType.SPLIT)
        }
    }

    fun createPaymentUrl() {
        if (isCreatingUrl) return
        isCreatingUrl = true
        coroutineScope.launch(Dispatchers.IO) {
            val result = paymentUrlCreator.createPaymentUrl(
                baseUrl = BuildConfig.PAYMENT_BASE_URL,
                merchantApiKey = BuildConfig.MERCHANT_API_KEY,
                amount = orderAmount,
                paymentMethods = selectedMethods.map { it.name },
            )
            withContext(Dispatchers.Main) {
                isCreatingUrl = false
                result.onSuccess { url ->
                    paymentUrl = url
                    payResultFlow.value = "Payment URL created"
                }.onFailure { error ->
                    payResultFlow.value = "Error: ${error.message}"
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pay with Redirect",
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
            SectionCard(title = "Result") {
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
            }

            SectionCard(title = "YPayButton Preview") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BUTTON_CONTAINER_HEIGHT_DP.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        key(selectedMethods) {
                            AndroidView(
                                modifier = Modifier.width(widthFixedDp.dp),
                                factory = { ctx ->
                                    YPayButton(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            Gravity.CENTER,
                                        )
                                        bindTo(
                                            paymentSession = paymentSession,
                                            args = SessionListenerArgs(selectedPaymentMethods = selectedMethods),
                                        )
                                        setOnClickListener(
                                            yPayLauncher = yPayLauncher,
                                            listener = { buttonLauncher ->
                                                if (paymentUrl.isBlank()) {
                                                    payResultFlow.value = "Enter payment URL first"
                                                    return@setOnClickListener
                                                }
                                                buttonLauncher.launch(
                                                    YPayContractParams(
                                                        paymentSession = paymentSession,
                                                        paymentData = PaymentData(paymentUrl = paymentUrl),
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                },
                                update = { button ->
                                    button.cornerRadius = cornerRadius
                                },
                            )
                        }
                    }
                }
            }

            PaymentUrlCard(
                value = paymentUrl,
                onValueChange = { paymentUrl = it },
                orderAmount = orderAmount,
                onOrderAmountChange = { orderAmount = it },
                isCreatingUrl = isCreatingUrl,
                onCreateUrl = { createPaymentUrl() },
            )

            SectionCard(title = "Button Appearance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Corner radius",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Slider(
                        value = cornerRadius,
                        onValueChange = { cornerRadius = it },
                        valueRange = 0f..MAX_CORNER_RADIUS,
                        steps = 99,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    )
                    Text(
                        text = "${cornerRadius.toInt()} px",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Width",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Slider(
                        value = widthFixedDp,
                        onValueChange = { widthFixedDp = it },
                        valueRange = MIN_WIDTH_DP..MAX_WIDTH_DP,
                        steps = 35,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    )
                    Text(
                        text = "${widthFixedDp.toInt()} dp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            SectionCard(title = "Payment methods") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CheckboxRow(
                        label = "Card",
                        checked = cardChecked,
                        onCheckedChange = { cardChecked = it },
                    )
                    CheckboxRow(
                        label = "Split",
                        checked = splitChecked,
                        onCheckedChange = { splitChecked = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentUrlCard(
    value: String,
    onValueChange: (String) -> Unit,
    orderAmount: String,
    onOrderAmountChange: (String) -> Unit,
    isCreatingUrl: Boolean,
    onCreateUrl: () -> Unit,
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
                text = "PAYMENT URL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("https://pay.yandex.ru/...") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "ORDER AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = orderAmount,
                    onValueChange = { new -> onOrderAmountChange(new.filter(Char::isDigit)) },
                    placeholder = { Text("100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onCreateUrl,
                    enabled = !isCreatingUrl && orderAmount.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    if (isCreatingUrl) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Create URL")
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailingContent: @Composable (() -> Unit)? = null,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (trailingContent != null) {
                    trailingContent()
                }
            }
            content()
        }
    }
}

private const val DEFAULT_CORNER_RADIUS = 32f
private const val MAX_CORNER_RADIUS = 100f
private const val DEFAULT_WIDTH_DP = 320f
private const val MIN_WIDTH_DP = 40f
private const val MAX_WIDTH_DP = 400f
private const val BUTTON_CONTAINER_HEIGHT_DP = 100
