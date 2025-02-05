package com.languagetranslator.languagetranslator.di

import android.app.Activity
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object TextToSpeechModule {

    @Provides
    @ActivityScoped
    fun provideTextToSpeech(activity: Activity): TextToSpeech {
        return TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("TTS Module", "TextToSpeech initialized successfully.")
            } else {
                Log.e("TTS Module", "Failed to initialize TextToSpeech.")
            }
        }
    }
}

