package com.here.android.api

class ZumtobelRepository (private val api : ZumtobelApi) : BaseRepository() {

    suspend fun getDevices() : MutableList<Device>?{

        //safeApiCall is defined in BaseRepository.kt (https://gist.github.com/navi25/67176730f5595b3f1fb5095062a92f15)
        val devicesResponse = safeApiCall(
                call = {api.getDevicesAsync().await()},
                errorMessage = "Error Fetching Popular Movies"
        )

        return devicesResponse?.toMutableList();

    }

    suspend fun changeLightIntensity(id: String, intensity: Double) {
        safeApiCall(
                call = {api.changeLightIntensityAsync(id, intensity).await()},
                errorMessage = "Error changing light intensity"
        )
    }

}