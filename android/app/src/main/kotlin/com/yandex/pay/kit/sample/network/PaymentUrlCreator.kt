package com.yandex.pay.kit.sample.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class PaymentUrlCreator {

    private val client = OkHttpClient.Builder().build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun createPaymentUrl(
        baseUrl: String,
        merchantApiKey: String,
        amount: String,
        currencyCode: String = "RUB",
        paymentMethods: List<String>,
    ): Result<String> = runCatching {
        require(amount.isNotBlank()) { "Amount is required" }
        require(merchantApiKey.isNotBlank()) { "Merchant API key is required. Set MERCHANT_API_KEY in local.properties" }

        val requestDto = CreateOrderRequest(
            cart = CartDto(
                items = listOf(
                    CartItemDto(
                        productId = "sample_product",
                        quantity = QuantityDto(count = "1"),
                        title = "Sample Product",
                        total = amount,
                    ),
                ),
                total = TotalDto(amount = amount),
            ),
            currencyCode = currencyCode,
            orderId = "order_${Date().time}",
            paymentMethods = paymentMethods,
            redirectUrls = RedirectUrlsDto(
                onSuccess = "", // for demo empty redirects are ok, but you should use real ones in production code (:
                onError = "",
            ),
            ttl = 3600,
        )

        val body = json.encodeToString(
            serializer = CreateOrderRequest.serializer(),
            value = requestDto,
        ).toRequestBody(contentType = "application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl$CREATE_ORDER_PATH")
            .post(body = body)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Api-Key $merchantApiKey")
            .build()

        val response = executeRequest(request = request)
        val responseBody = response.body?.string() ?: error("Empty response body")

        if (!response.isSuccessful) {
            error("HTTP ${response.code}: ${responseBody.take(500)}")
        }

        // Check if response is actually JSON
        if (!responseBody.trimStart().startsWith("{")) {
            error("Server returned non-JSON response: ${responseBody.take(200)}")
        }

        val parsed = json.decodeFromString(
            deserializer = CreateOrderResponse.serializer(),
            string = responseBody,
        )
        parsed.data.paymentUrl
    }

    private suspend fun executeRequest(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request = request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(exception = e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(value = response)
            }
        })
        continuation.invokeOnCancellation { call.cancel() }
    }

    private companion object {
        const val CREATE_ORDER_PATH = "/merchant/v1/orders"
    }
}

@Serializable
internal data class CreateOrderRequest(
    @SerialName("cart")
    val cart: CartDto,

    @SerialName("currencyCode")
    val currencyCode: String,

    @SerialName("orderId")
    val orderId: String,

    @SerialName("availablePaymentMethods")
    val paymentMethods: List<String>,

    @SerialName("redirectUrls")
    val redirectUrls: RedirectUrlsDto,

    @SerialName("ttl")
    val ttl: Int,
)

@Serializable
internal data class CartDto(
    @SerialName("items")
    val items: List<CartItemDto>,

    @SerialName("total")
    val total: TotalDto,
)

@Serializable
internal data class CartItemDto(
    @SerialName("productId")
    val productId: String,

    @SerialName("quantity")
    val quantity: QuantityDto,

    @SerialName("title")
    val title: String,

    @SerialName("total")
    val total: String,
)

@Serializable
internal data class QuantityDto(
    @SerialName("count")
    val count: String,
)

@Serializable
internal data class TotalDto(
    @SerialName("amount")
    val amount: String,
)

@Serializable
internal data class RedirectUrlsDto(
    @SerialName("onError")
    val onError: String,

    @SerialName("onSuccess")
    val onSuccess: String,
)

@Serializable
internal data class CreateOrderResponse(
    @SerialName("data")
    val data: PaymentUrlData,
)

@Serializable
internal data class PaymentUrlData(
    @SerialName("paymentUrl")
    val paymentUrl: String,
)
