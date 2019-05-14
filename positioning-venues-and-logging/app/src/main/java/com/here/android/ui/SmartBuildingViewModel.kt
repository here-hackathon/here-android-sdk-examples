package com.here.android.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.here.android.api.ApiFactory
import com.here.android.api.BoschApiFactory
import com.here.android.api.BoschRepository
import com.here.android.api.Device
import com.here.android.api.ZumtobelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SmartBuildingViewModel: ViewModel() {
    private val parentJob = Job()

    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Default

    private val scope = CoroutineScope(coroutineContext)

    private val repository = ZumtobelRepository(ApiFactory.zumtobelApi)

    val devicesLiveData = MutableLiveData<MutableList<Device>>()

    fun fetchDevices(){
        scope.launch {
            val devices = repository.getDevices()
            devicesLiveData.postValue(devices)
        }
    }

    fun changeLightIntensity(id: String, intensity: Double){
        scope.launch {
            repository.changeLightIntensity(id, intensity)
        }
    }

    private val boschRepository = BoschRepository(BoschApiFactory.boschApi)

    fun setTemperatureHeating(value: Int){
        scope.launch {
            boschRepository.setTemperatureHeating(value)
        }
    }

    fun cancelAllRequests() = coroutineContext.cancel()
}