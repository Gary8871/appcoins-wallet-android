package com.asfoundation.wallet.di

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class CustomInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request: Request = chain.request()
    var newRequest: Request

    newRequest = try {
      request.newBuilder()
          .addHeader("Content-Type", "application/json; format=product_token")
          .build()
    } catch (e: Exception) {
      e.printStackTrace()
      return chain.proceed(request)
    }

    return chain.proceed(newRequest)
  }
}
