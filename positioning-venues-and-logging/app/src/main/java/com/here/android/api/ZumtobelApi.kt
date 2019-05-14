package com.here.android.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ZumtobelApi {
        @GET("devices")
        fun getDevicesAsync(): Deferred<Response<List<Device>>>

        @POST("devices/{id}/lightingcapability/setintensity/{intensity}/0")
        fun changeLightIntensityAsync(@Path("id") id: String, @Path("intensity") intensity: Double): Deferred<Response<Unit>>
}

data class HumidityCapability(
        val humidity: Double
)

data class TemperatureCapability(
    val temperature: Double
)

data class LightingCapability(
        val intensity: Double
)

data class NoiseCapability(
        val noise: Int
)

data class VocCapability(
        val voc: Int
)

data class BrightnessCapability(
        val brightness: Int
)

data class PresenceCapability(
        val presence: Boolean
)

data class Co2Capability(
        val co2: Int
)

data class Device(
    val id: String,
    val type: String,
    val name: String,
    val temperatureCapability: TemperatureCapability?,
    val humidityCapability: HumidityCapability?,
    val lightingCapability: LightingCapability?,
    val noiseCapability: NoiseCapability?,
    val vocCapability: VocCapability?,
    val brightnessCapability: BrightnessCapability?,
    val presenceCapability: PresenceCapability?,
    val co2Capability: Co2Capability?
)

data class DevicesResponse(
        val devices: List<Device>
)