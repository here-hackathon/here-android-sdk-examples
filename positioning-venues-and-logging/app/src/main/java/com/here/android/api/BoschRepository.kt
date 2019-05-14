package com.here.android.api

class BoschRepository (private val api : BoschApi) : BaseRepository() {
    companion object {
        val TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJwb2ludHRfY2FzdHQiLCJzdWIiOiJjaWFtX3MtMS01LTIxLTE5Mzc4NTU2OTUtMzk2NDc5MzYzNy04Nzk2NDQ0MDEtNzA0NjMzIiwiZXhwIjoxNTU3ODUxMDgwLCJqdGkiOiJjNzQ0ODkxMS05NTJiLTQ1OGQtYTFhNi1iYzQ5NTdjZWRkYWMiLCJjbGllbnRfaWQiOiJuY2MtNDMxYjhiZTY0MmNhNzE3ZiIsInNjb3BlIjpbInBvaW50dF9iYXNpYyIsInBvaW50dF91bnJlc3RyaWN0ZWQiXX0.hN0EVrEiHE-I1M8OwugvWWTzPzACsHGvAd0fK1blGpByuYYKCUDXBtPCLio9eJP1XVRebjOm5LrluWqNCfCF1wyO2SH4HYyalXPbD1alAhWtmg9VY0iU-P2o2pA0S_VV087mRtXE9xJrCD3wurGcN_09QfcpDRrvmppKHQ9RoCqHZ7an2uQpLdPaQLaFsN96NcNPaE4MUv7XE-hzZ4Ee9NAP7dh6nqhBKy-rxX8YaXgoBFs8zey1TpwDtTQpI0K8lhPyo_Sa2Mzm-jFTocArC7aC_nQngIZe0ryz852qWjAnfgka1msgtAsziIYj87H0MEktP7-k8W5TLaOfd6h7Ww"
    }

    suspend fun setTemperatureHeating(value: Int){

        //safeApiCall is defined in BaseRepository.kt (https://gist.github.com/navi25/67176730f5595b3f1fb5095062a92f15)
        safeApiCall(
                call = {api.setTemperatureHeatingAsync(TOKEN, "application/json",
                        "application/json", Temperature(value)
                ).await()},
                errorMessage = "Error Fetching Popular Movies"
        )
    }

}