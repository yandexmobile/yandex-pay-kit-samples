package com.yandex.pay.kit.sample

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yandex.pay.assistant.api.assistant.assistant
import com.yandex.pay.auth.YPayAuthResult
import com.yandex.pay.auth.api.auth
import com.yandex.pay.facade.api.YPay
import com.yandex.pay.inapp.api.payInApp
import com.yandex.pay.payment.YPayResult
import com.yandex.pay.quickpay.api.IsPaymentEnabled
import com.yandex.pay.quickpay.api.QuickPayResult
import com.yandex.pay.quickpay.api.QuickPaymentStateListener
import com.yandex.pay.quickpay.api.facade.quickPay
import com.yandex.pay.withredirect.api.facade.payWithRedirect
import com.yandex.pay.withredirect.api.launcher.YPayLauncher
import com.yandex.pay.withredirect.api.session.PaymentSession
import com.yandex.pay.kit.sample.ui.AssistantScreen
import com.yandex.pay.kit.sample.ui.AuthScreen
import com.yandex.pay.kit.sample.ui.CpqrScreen
import com.yandex.pay.kit.sample.ui.InventoryScreen
import com.yandex.pay.kit.sample.ui.MainScreen
import com.yandex.pay.kit.sample.ui.PayRedirectScreen
import com.yandex.pay.kit.sample.ui.PayWidgetScreen
import com.yandex.pay.kit.sample.ui.ProfileScreen
import com.yandex.pay.kit.sample.ui.theme.YaPayKitSampleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.core.content.edit

class MainActivity : FragmentActivity() {

    private lateinit var paymentSession: PaymentSession
    private lateinit var yPayLauncher: YPayLauncher
    private val payResultFlow = MutableStateFlow<String?>(null)
    private val isAuthorized = MutableStateFlow(false)
    private val authIcon = MutableStateFlow<ImageVector?>(null)

    private val cpqrSessionId = MutableStateFlow<String?>(null)
    private val cpqrPaymentEnabled = MutableStateFlow<Boolean?>(null)
    private val cpqrResult = MutableStateFlow<String?>(null)

    private val authPrefs by lazy {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    private val onLogout: () -> Unit = {
        isAuthorized.value = false
        authIcon.value = null
        cpqrSessionId.value = null
        cpqrPaymentEnabled.value = null
        cpqrResult.value = null
        payResultFlow.value = null
        YPay.auth.setPartnerAuthState(isUserAuthorized = false)
        YPay.auth.logout()
        YPay.quickPay.logout()
        authPrefs.edit { putBoolean(KEY_AUTHORIZED, false) }
    }

    private val quickPaymentStateListener = object : QuickPaymentStateListener {
        override fun onPaymentEnabledStateChanged(isEnabled: IsPaymentEnabled) {
            cpqrPaymentEnabled.value = isEnabled.value
        }

        override fun onSessionExpired() {
            cpqrSessionId.value = null
        }

        override fun onPaymentResult(quickpayResult: QuickPayResult) {
            cpqrResult.value = when (quickpayResult) {
                QuickPayResult.Success -> "Success"
                QuickPayResult.Failure -> "Failure"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // -- Auth --
        val wasAuthorized = authPrefs.getBoolean(KEY_AUTHORIZED, false)
        isAuthorized.value = wasAuthorized
        if (wasAuthorized) {
            val iconIndex = authPrefs.getInt(KEY_AUTH_ICON_INDEX, 0)
            authIcon.value = AUTH_ICONS.getOrNull(iconIndex) ?: AUTH_ICONS.first()
        }
        lifecycleScope.launch {
            YPay.auth.authResultEvents.collectLatest { event ->
                if (event is YPayAuthResult.Success) {
                    val iconIndex = Random.nextInt(AUTH_ICONS.size)
                    isAuthorized.value = true
                    authIcon.value = AUTH_ICONS[iconIndex]
                    YPay.auth.setPartnerAuthState(isUserAuthorized = true)
                    authPrefs.edit {
                        putBoolean(KEY_AUTHORIZED, true)
                            .putInt(KEY_AUTH_ICON_INDEX, iconIndex)
                    }
                }
            }
        }

        // -- In-app --
        YPay.payInApp.initUi(activity = this)

        // -- Assistant --
        YPay.assistant.setUiDeps(activity = this, fragmentManager = supportFragmentManager)

        // -- Pay with redirect --
        paymentSession = YPay.payWithRedirect.getYandexPaymentSession()
        yPayLauncher = YPayLauncher(
            activityResultCaller = this,
            paymentProcessCallback = { result: YPayResult ->
                payResultFlow.value = when (result) {
                    is YPayResult.Success -> "Success: orderId=${result.orderId.value}"
                    is YPayResult.Cancelled -> "Cancelled"
                    is YPayResult.Failure -> "Failure: ${result.errorMsg}"
                }
            },
        )

        // -- Quick Pay --
        lifecycleScope.launch {
            YPay.quickPay.initUi(
                activity = this@MainActivity,
                fragmentManager = supportFragmentManager,
                quickPaymentStateListener = quickPaymentStateListener,
            )
        }

        // -- UI --
        setContent {
            YaPayKitSampleTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Routes.MAIN,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(Routes.MAIN) {
                        MainScreen(
                            onAuth = { navController.navigate(Routes.AUTH) },
                            onPayRedirect = { navController.navigate(Routes.PAY_REDIRECT) },
                            onPayWidget = { navController.navigate(Routes.PAY_WIDGET) },
                            onAssistant = { navController.navigate(Routes.ASSISTANT) },
                            onCpqr = { navController.navigate(Routes.CPQR) },
                            onInventory = { navController.navigate(Routes.INVENTORY) },
                            onProfile = { navController.navigate(Routes.SETTINGS) },
                        )
                    }
                    composable(Routes.SETTINGS) {
                        ProfileScreen(
                            isAuthorized = isAuthorized,
                            authIcon = authIcon,
                            onLogout = onLogout,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.AUTH) {
                        AuthScreen(
                            isAuthorized = isAuthorized,
                            onLogout = onLogout,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.PAY_REDIRECT) {
                        PayRedirectScreen(
                            paymentSession = paymentSession,
                            yPayLauncher = yPayLauncher,
                            payResultFlow = payResultFlow,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.PAY_WIDGET) {
                        PayWidgetScreen(
                            isAuthorized = isAuthorized,
                            paymentSession = paymentSession,
                            yPayLauncher = yPayLauncher,
                            payResultFlow = payResultFlow,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.ASSISTANT) {
                        AssistantScreen(
                            isAuthorized = isAuthorized,
                            onLogout = onLogout,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.CPQR) {
                        CpqrScreen(
                            sessionIdFlow = cpqrSessionId,
                            paymentEnabledFlow = cpqrPaymentEnabled,
                            resultFlow = cpqrResult,
                            onLogout = onLogout,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.INVENTORY) {
                        InventoryScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // -- Assistant --
        YPay.assistant.clearUiDeps()
        // -- Pay with redirect --
        YPay.payWithRedirect.removePaymentSession(sessionKey = paymentSession.sessionKey)
        super.onDestroy()
    }
}

internal object Routes {
    const val MAIN = "main"
    const val AUTH = "auth"
    const val PAY_REDIRECT = "pay_redirect"
    const val PAY_WIDGET = "pay_widget"
    const val ASSISTANT = "assistant"
    const val CPQR = "cpqr"
    const val INVENTORY = "inventory"
    const val SETTINGS = "settings"
}

private const val KEY_AUTHORIZED = "is_authorized"
private const val KEY_AUTH_ICON_INDEX = "auth_icon_index"

private val AUTH_ICONS = listOf(
    Icons.Filled.CheckCircle,
    Icons.Filled.Verified,
    Icons.Filled.AccountCircle,
    Icons.Filled.EmojiEmotions,
    Icons.Filled.Celebration,
    Icons.Filled.Favorite,
    Icons.Filled.Star,
    Icons.Filled.ThumbUp,
    Icons.Filled.Accessibility,
    Icons.Filled.Pets,
    Icons.Filled.Biotech,
)
