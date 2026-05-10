package com.messaging.service.online.api

import com.messaging.service.BuildConfig
import com.messaging.service.kpi.KpiTracker
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkClient @Inject constructor(
    private val kpiTracker: KpiTracker,
    private val tokenProvider: TokenProvider
) {

    val messagingApi: MessagingApi by lazy { buildRetrofit().create(MessagingApi::class.java) }

    private fun buildRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.ONLINE_BASE_URL)
        .client(buildOkHttp())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun buildOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor())
            .addInterceptor(kpiInterceptor())
            .addInterceptor(logging)
            .build()
    }

    /** Attaches Bearer token to every request. */
    private fun authInterceptor() = Interceptor { chain ->
        val token = tokenProvider.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        chain.proceed(request)
    }

    /** Records HTTP latency and success/failure counts as KPI metrics. */
    private fun kpiInterceptor() = Interceptor { chain ->
        val start = System.currentTimeMillis()
        val response = try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            kpiTracker.increment(KpiTracker.Key.ONLINE_REQUEST_ERRORS)
            throw e
        }
        val latencyMs = System.currentTimeMillis() - start
        kpiTracker.recordLatency(KpiTracker.Key.ONLINE_LATENCY_MS, latencyMs)
        if (response.isSuccessful) {
            kpiTracker.increment(KpiTracker.Key.ONLINE_REQUESTS_SUCCESS)
        } else {
            kpiTracker.increment(KpiTracker.Key.ONLINE_REQUEST_ERRORS)
        }
        response
    }
}

/** Provide the JWT token – replace with your auth mechanism. */
interface TokenProvider {
    fun getToken(): String?
}
