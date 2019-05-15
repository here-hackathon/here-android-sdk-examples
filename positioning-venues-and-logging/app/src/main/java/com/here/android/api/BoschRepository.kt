package com.here.android.api

class BoschRepository (private val api : BoschApi) : BaseRepository() {
    companion object {
        val TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJwb2ludHRfY2FzdHQiLCJzdWIiOiJjaWFtX3MtMS01LTIxLTE5Mzc4NTU2OTUtMzk2NDc5MzYzNy04Nzk2NDQ0MDEtNzA0NjMzIiwiZXhwIjoxNTU3OTE1NjMyLCJqdGkiOiJkOTM3MzMzZC05YWNjLTRiNzctYWEzMi0xNzY0NzY0YWQ5NmMiLCJjbGllbnRfaWQiOiJuY2MtNDMxYjhiZTY0MmNhNzE3ZiIsInNjb3BlIjpbInBvaW50dF9iYXNpYyIsInBvaW50dF91bnJlc3RyaWN0ZWQiXX0.jfsLMaK0FTa08xxx9gH-H40ZPSsJgRGUONSKfG3pL8n-WGbDDQ9w0n-07Dh74-O8IttAzhG7uDSA7sObuS8ZBFUQ1cuB-BzA9aoRE8RW9TgpFw6-VBhIXkhqt1GFxpyIn1wK-0lb_u1o2oO5WyGHl5PTRj1H5wPC66BPIUL_O3svsEC4aCFhrMgelqi_EOQYYr2q76IMs6JCsIPds7jpX6cHNv8_WU5WQRT-xTGWZezuDXz-vo0qdFB6w2VLO8Jw2NyQENPSi3lIlEceKAEpYFe7Gr8wU_m57RDr7RWEgCaB5AYVGWoDi-L8cdlqrgv2vH5_UvIC2bjIlpUDfeTFAQ"
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