package com.here.android.api

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiFactory{

    private val okHttpClient = OkHttpClient().newBuilder()
            .addInterceptor(loggingInterceptor())
            .build()



    fun retrofit() : Retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://100.100.200.51:2222/rest/api/v1/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    private fun loggingInterceptor() : Interceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }


    val zumtobelApi : ZumtobelApi = retrofit().create(ZumtobelApi::class.java)

}