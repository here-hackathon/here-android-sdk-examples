package com.here.android.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT


interface BoschApi {
        @PUT("gateways/101080486/resource/zones/zn1/manualTemperatureHeating")
        fun setTemperatureHeatingAsync(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String,
        @Header("Accept") accept: String,
        @Body body: Temperature): Deferred<Response<Unit>>
}

data class Temperature(
      val  value: Int
)