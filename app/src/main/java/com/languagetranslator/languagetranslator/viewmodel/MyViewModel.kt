package com.languagetranslator.languagetranslator.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.languagetranslator.languagetranslator.model.Repository
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {
    private val repository = Repository()

    private val _translationResult = MutableLiveData<String?>()
    private val _detectLanguage = MutableLiveData<String?>()

    val translationResult: MutableLiveData<String?> get() = _translationResult
    val detectLanguage: MutableLiveData<String?> get() = _detectLanguage

    /**
    * It takes two parameter target language and data which have to translate
    * From here Repository class method called and provide it same parameter we get
    * using MutableLiveData<String?> we receive response
    * */
    fun translateText(target: String, data: String) {
        Log.d(TAG, "translateText: target = $target and data = $data")
        viewModelScope.launch {
            val result = repository.getTranslateDataFromGemini(target, data)
            _translationResult.postValue(result.toString())
        }
    }

    /**
    * It takes actual data from the fragment or activity
    * It send the data to repository class to detect the language of the data content*/
    fun detectLanguageOfText(data: String) {
        Log.d(TAG, "detectLanguageOfText: detecting language into MyViewModel class")
        viewModelScope.launch {
            val language = repository.getDetectLanguage(data)
            _detectLanguage.postValue(language.toString())
        }
    }

    companion object {
        private const val TAG = "MyViewModel"
    }
}