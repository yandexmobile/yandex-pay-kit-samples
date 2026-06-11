package com.yandex.pay.kit.sample.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.pay.assistant.api.assistant.assistant
import com.yandex.pay.assistant.api.widget.benefits.YPayBenefitsWidget
import com.yandex.pay.assistant.api.widget.benefits.YPayBenefitsWidgetClickability
import com.yandex.pay.assistant.api.widget.benefits.YPayBenefitsWidgetScreen
import com.yandex.pay.auth.api.auth
import com.yandex.pay.facade.api.YPay
import com.yandex.pay.kit.sample.SecureHardware
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssistantScreen(
    isAuthorized: StateFlow<Boolean>,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val authorized by isAuthorized.collectAsState()
    val context = LocalContext.current
    val assistantSupported = remember { YPay.assistant.isSupported }
    var alwaysClickable by remember { mutableStateOf(true) }

    LaunchedEffect(authorized) {
        YPay.auth.setPartnerAuthState(isUserAuthorized = authorized)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assistant",
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
                .padding(bottom = PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            key(alwaysClickable) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(PADDING.dp))

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    constraints.copy(maxWidth = constraints.maxWidth + PADDING * 2),
                                )
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            },
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                )
                                addView(
                                    YPayBenefitsWidget(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                        )
                                        setScreen(screen = YPayBenefitsWidgetScreen.PRODUCT)
                                        setClickability(
                                            if (alwaysClickable) YPayBenefitsWidgetClickability.ALWAYS
                                            else YPayBenefitsWidgetClickability.ONLY_AUTHORIZED
                                        )
                                        setMetaInfo(
                                            metaInfo = mapOf(
                                                "amount" to "100.0",
                                                "currency" to "RUB",
                                                "product_id" to "<product_id>",
                                            ),
                                        )
                                    },
                                )
                                if (!assistantSupported) {
                                    addView(
                                        android.view.View(ctx).apply {
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
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PADDING.dp),
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
                        text = "Widget Settings",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alwaysClickable = !alwaysClickable }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = alwaysClickable, onCheckedChange = { alwaysClickable = it })
                        Text(
                            text = "Always clickable",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

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

private const val PADDING = 24
