package com.languagetranslator.languagetranslator.model

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject

class Repository @Inject constructor(private val model : GenerativeModel) {
    /*
    *It takes two parameter first is target language and text which has to translate
    * prompt contains what actually gemini do
    * after generate content it gives the response
    * this response return to method call
    * If error comes than it return null
    */
    suspend fun getTranslateDataFromGemini(target: String, data: String): String? {
        Log.d(TAG, "getTranslateDataFromGemini: data = $data and target = $target")
        val prompt = "translate this '$data' to '$target' language. Give only one option. Give only translated language part."
        return try {
            val response = model.generateContent(prompt = prompt)
            Log.d(TAG, "getData: response = ${response.text}")
            response.text
        } catch (e: Exception) {
            Log.d(TAG, "Error in getTranslateDataFromGemini: ${e.message}")
            null
        }
    }


    /*
    * It send the data content to gemini to detect the language of data*/
    suspend fun getDetectLanguage(data: String): String? {
        Log.d(TAG, "getDetectLanguage: detect language for $data")
        val detectLanguagePrompt = "Detect the '$data' language. Give only name of language."
        return try {
            val responseLanguage = model.generateContent(prompt = detectLanguagePrompt)
            Log.d(TAG, "getDetectLanguage: responseLanguage = ${responseLanguage.text}")
            responseLanguage.text
        } catch (e: Exception) {
            Log.d(TAG, "getDetectLanguage: Error to detect language ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "Repository"
    }
}