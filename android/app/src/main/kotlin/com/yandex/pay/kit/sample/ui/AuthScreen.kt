package com.yandex.pay.kit.sample.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yandex.pay.assistant.api.assistant.assistant
import com.yandex.pay.auth.YPayAuthResult
import com.yandex.pay.auth.api.auth
import com.yandex.pay.facade.api.YPay
import com.yandex.pay.inapp.api.payInApp
import com.yandex.pay.kit.sample.SecureHardware
import com.yandex.pay.quickpay.api.facade.quickPay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthScreen(
    isAuthorized: StateFlow<Boolean>,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    val authorized by isAuthorized.collectAsState()
    var state by remember { mutableStateOf<YPayAuthResult?>(null) }
    val context = LocalContext.current
    val sdkSupported = remember {
        YPay.payInApp.isSupported ||
            YPay.assistant.isSupported ||
            YPay.quickPay.isSupported
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = YPay.auth.getAuthContract(),
    ) { /* result delivered via YPay.auth.authResultEvents */ }

    LaunchedEffect(Unit) {
        YPay.auth.authResultEvents.collectLatest { event -> state = event }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Yandex ID",
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
            if (!authorized) {
                Text(
                    text = "The button below launches Yandex ID authorization vis SDK. " +
                        "On success SDK returns a merchant token that " +
                        "should be used to connect Yandex ID session to user.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        if (sdkSupported) {
                            signInLauncher.launch(Unit)
                        } else {
                            Toast.makeText(
                                context,
                                SecureHardware.UNAVAILABLE_MESSAGE,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = "Sign in with Yandex ID",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                Text(
                    text = "You are already authorized via Yandex ID. " +
                        "Use the button below to log out of your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (val r = state) {
                is YPayAuthResult.Success -> SuccessCard(token = r.token)
                is YPayAuthResult.Cancel -> InfoBanner(
                    icon = Icons.Filled.Info,
                    text = "Cancelled by user",
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is YPayAuthResult.Failure -> InfoBanner(
                    icon = Icons.Filled.Warning,
                    text = "Failure: ${r.message ?: "unknown"}",
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                )
                null -> Unit
            }

            if (authorized) {
                TextButton(
                    onClick = {
                        onLogout()
                        state = null
                    },
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
private fun SuccessCard(token: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Success",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "MERCHANT TOKEN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = token,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Text(
                text = "→ POST this token to your backend, validate it against Yandex Pay API, " +
                    "issue your own session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
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
