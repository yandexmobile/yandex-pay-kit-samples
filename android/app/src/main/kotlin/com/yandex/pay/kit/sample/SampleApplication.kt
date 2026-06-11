package com.yandex.pay.kit.sample

import android.app.Application
import com.yandex.pay.assistant.api.assistant.assistantFlow
import com.yandex.pay.auth.api.authFlow
import com.yandex.pay.configuration.YPayEnvironment
import com.yandex.pay.configuration.YPayLocale
import com.yandex.pay.configuration.YPayTheme
import com.yandex.pay.facade.api.YPay
import com.yandex.pay.inapp.api.payInAppFlow
import com.yandex.pay.inventory.api.inventory.inventoryFlow
import com.yandex.pay.kit.sample.BuildConfig
import com.yandex.pay.quickpay.api.facade.quickPayFlow
import com.yandex.pay.withredirect.api.facade.payWithRedirectFlow

internal class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val merchantId = BuildConfig.MERCHANT_ID
        YPay.init(
            context = this,
            flows = listOf(
                assistantFlow(merchantId = merchantId),
                authFlow(merchantId = merchantId),
                payWithRedirectFlow(merchantId = merchantId),
                payInAppFlow(merchantId = merchantId),
                quickPayFlow(merchantId = merchantId),
                inventoryFlow(merchantId = merchantId),
            ),
        ) {
            environment = YPayEnvironment.SANDBOX
            theme = YPayTheme.SYSTEM
            locale = YPayLocale.SYSTEM
        }
    }
}
