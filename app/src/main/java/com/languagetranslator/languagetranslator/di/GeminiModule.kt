package com.languagetranslator.languagetranslator.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.languagetranslator.languagetranslator.BuildConfig
import com.languagetranslator.languagetranslator.model.Repository
import com.languagetranslator.languagetranslator.utils.Constant
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    fun provideGeminiModule(): GenerativeModel {
        return GenerativeModel(
            modelName = Constant.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 10000
                responseMimeType = "text/plain"
            },
        )
    }

    @Provides
    @Singleton
    fun provideRepository(model : GenerativeModel): Repository{
        return Repository(model)
    }
}