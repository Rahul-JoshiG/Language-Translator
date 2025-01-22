package com.languagetranslator.languagetranslator

import android.app.Application
import android.util.Log
import com.languagetranslator.languagetranslator.utils.NetworkUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LanguageApplication : Application() {
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