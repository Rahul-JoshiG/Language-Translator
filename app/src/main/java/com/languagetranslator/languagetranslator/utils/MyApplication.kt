package com.languagetranslator.languagetranslator.utils

import android.app.Application
import android.util.Log

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: $TAG")
        ToastHelper.initToast(this)
        NetworkUtils.initNetworkUtils(this)
    }
    companion object{
        private const val TAG = "MyApplication"
    }
}