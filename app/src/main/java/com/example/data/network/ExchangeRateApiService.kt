package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Response model from the ExchangeRate-API (open.er-api.com/v6/latest/{base}).
 */
@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    @property:Json(name = "result") val result: String,
    @property:Json(name = "provider") val provider: String? = null,
    @property:Json(name = "base_code") val baseCode: String,
    @property:Json(name = "time_last_update_utc") val timeLastUpdateUtc: String? = null,
    @property:Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @property:Json(name = "rates") val rates: Map<String, Double> = emptyMap()
)

/**
 * Retrofit service interface for real-time exchange rates.
 */
interface ExchangeRateApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") baseCurrency: String
    ): ExchangeRateApiResponse
}

/**
 * Singleton network client for Exchange Rate API operations.
 */
object NetworkClient {
    private const val BASE_URL = "https://open.er-api.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val exchangeRateApi: ExchangeRateApiService by lazy {
        retrofit.create(ExchangeRateApiService::class.java)
    }
}
