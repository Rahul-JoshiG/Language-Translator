@file:Suppress("DEPRECATION")

package com.languagetranslator.languagetranslator.view.fragments

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.languagetranslator.languagetranslator.R
import com.languagetranslator.languagetranslator.databinding.FragmentTextBinding
import com.languagetranslator.languagetranslator.datamodel.supportedLanguages
import com.languagetranslator.languagetranslator.utils.Constant
import com.languagetranslator.languagetranslator.utils.DialogUtils
import com.languagetranslator.languagetranslator.utils.NetworkUtils
import com.languagetranslator.languagetranslator.viewmodel.MyViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TextFragment : Fragment() {

    @Inject
    lateinit var mSpeech: TextToSpeech

    private var _binding: FragmentTextBinding? = null
    private val mBinding get() = _binding!!

    private lateinit var mMyViewModel: MyViewModel

    //view is creating
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: ")
        _binding = FragmentTextBinding.inflate(inflater, container, false)
        mMyViewModel = ViewModelProvider(this)[MyViewModel::class.java]
        return mBinding.root
    }

    /**
     * Handles speaking the text input by the user, toggling between play and pause states,
     * and monitoring the speaking progress. Set-upping toolbar, source language spinner,
     * target language spinner and all click listener.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: view created main of text fragment")

        setUpToolbar()
        setUpSpinner()
        setOnClickListener()
    }

    override fun onResume() {
        super.onResume()
        if (!NetworkUtils.isInternetAvailable(requireActivity())) {
            DialogUtils.showNoInternetDialog(requireActivity())
        }
    }

    /**
     * All setOnClickListener calling are inside below method
     * **/
    private fun setOnClickListener() {
        Log.d(TAG, "setOnClickListener: ")

        mBinding.translateButton.setOnClickListener {
            Log.d(TAG, "setOnClickListener: ")
            val inputText = mBinding.sourceText.text.toString()
            if (inputText.isEmpty()) {
                ToastHelper.toast("No text available!!!")
                return@setOnClickListener
            } else {
                mBinding.progressBar.visibility = VISIBLE
                detectTheLanguage(inputText)
                translateText(inputText)
            }
        }

        mBinding.speakOriginalText.setOnClickListener {
            Log.d(TAG, "setOnClickListener: speak original text")
            monitorSpeakingState(Constant.ORIGINAL_TEXT)
            val originalText = mBinding.sourceText.text.toString()
            speakOriginalText(originalText)
        }

        mBinding.speakTranslatedText.setOnClickListener {
            Log.d(TAG, "setOnClickListener: speak translated text")
            monitorSpeakingState(Constant.TRANSLATED_TEXT)
            val translatedText = mBinding.targetText.text.toString()
            speakTheTranslatedText(translatedText)
        }

    }

    /**
     * Speak the translated text and toggle the play and pause states.
     * **/
    var isTranslatedSpeaking = false
    private fun speakTheTranslatedText(text: String) {
        Log.d(TAG, "speakTheTranslatedText: Text=$text")
        if (text.isEmpty()) {
            ToastHelper.toast("No text available for speech!")
            return
        }

        val selectedLanguage = supportedLanguages[mBinding.targetLang.selectedItemPosition]
        val locale = Locale.forLanguageTag(selectedLanguage.code)

        val result = mSpeech.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.d(TAG, "Language not supported for TTS: ${selectedLanguage.displayName}")
            ToastHelper.toast("Selected language is not supported for speech")
            return
        }

        if (isTranslatedSpeaking) {
            if (mSpeech.isSpeaking) {
                mSpeech.stop()
            }
            resetToPlayState(Constant.TRANSLATED_TEXT)
        } else {
            mBinding.speakTranslatedText.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_24)
            )
            speakText(text)
            isTranslatedSpeaking = true
        }
    }

    /**
     * Speaks the provided text and toggles between play and pause states.
     */
    private var isOriginalSpeaking = false // Tracks the speaking state
    private fun speakOriginalText(originalText: String) {
        Log.d(TAG, "speakOriginalText: Text to speak = $originalText")

        // Validate the text input
        if (originalText.isEmpty()) {
            ToastHelper.toast("No text available to speak!!!")
            return
        }

        if (isOriginalSpeaking) {
            // Stop speaking and switch to play state
            if (mSpeech.isSpeaking) mSpeech.stop()
            resetToPlayState(Constant.ORIGINAL_TEXT)
        } else {
            // Start speaking and switch to pause state
            mBinding.speakOriginalText.setImageDrawable(
                getDrawable(requireContext(), R.drawable.ic_pause_24)
            )
            speakText(originalText)
            isOriginalSpeaking = true
        }
    }

    /**
     * This method check the speech. It divides 3 parts starts, done and error
     * @param resource helps to differentiate between original and translated text.
     * After completing speech it reset the state of UI image button
     * **/
    private fun monitorSpeakingState(resource: String) {
        mSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "onStart: speaking starts")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "onDone: speaking finish")
                if (resource == Constant.ORIGINAL_TEXT) {
                    Log.d(TAG, "onDone: reset state for original text")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Constant.ORIGINAL_TEXT)
                    }
                } else {
                    Log.d(TAG, "onDone: reset state for translate text")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Constant.TRANSLATED_TEXT)
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                Log.d(TAG, "onError: speaking error")
                if (resource == Constant.ORIGINAL_TEXT) {
                    Log.d(TAG, "onError: error in original")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Constant.ORIGINAL_TEXT)
                    }
                } else {
                    Log.d(TAG, "onError: error in translated")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Constant.TRANSLATED_TEXT)
                    }
                }
            }
        })
    }

    /**
     * This method reset the state of UI image button to play image
     * @param resource helps to differentiate between original
     * and translate text
     * **/
    private fun resetToPlayState(resource: String) {
        Log.d(TAG, "resetToPlayState: resource = $resource")
        if (resource == Constant.ORIGINAL_TEXT) {
            Log.d(TAG, "resetToPlayState: reset state for original")
            requireActivity().runOnUiThread {
                mBinding.speakOriginalText.setImageDrawable(
                    getDrawable(requireContext(), R.drawable.ic_play_24)
                )
                isOriginalSpeaking = false
            }
        } else {
            Log.d(TAG, "resetToPlayState: reset state for translated")
            requireActivity().runOnUiThread {
                mBinding.speakTranslatedText.setImageDrawable(
                    getDrawable(requireContext(), R.drawable.ic_play_24)
                )
                isTranslatedSpeaking = false
            }
        }
    }

    /**
     * Uses TextToSpeech to speak the provided text.
     * @param text The text to be spoken.
     * @param text contains original text and translated text
     */
    private fun speakText(text: String) {
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID")
        }
        mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "UniqueID")
    }

    private fun setUpToolbar() {
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(mBinding.textToolbar)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mSpeech.isSpeaking) {
            mSpeech.stop()
        }
    }

    /**
     * Cleans up TextToSpeech resources when the fragment/activity is destroyed.
     */
    private fun setUpSpinner() {
        val sourceAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_layout,
            supportedLanguages.map { it.displayName }
        )
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        mBinding.sourceLang.adapter = sourceAdapter
        mBinding.sourceLang.setSelection(18)

        val targetAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_layout,
            supportedLanguages.map { it.displayName }
        )
        targetAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        mBinding.targetLang.adapter = targetAdapter
        mBinding.targetLang.setSelection(29)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mSpeech.isSpeaking) {
            mSpeech.stop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (mSpeech.isSpeaking) {
            mSpeech.stop()
            mSpeech.shutdown()
        }
    }

    /**
     *Translate the written text
     *
     * @param data goes for translation.
     * */
    private fun translateText(data: String) {
        Log.d(TAG, "translateText: data = $data")
        if (data.isEmpty()) {
            ToastHelper.toast("No Text is available")
            return
        }
        val targetLanguage =
            supportedLanguages[mBinding.targetLang.selectedItemPosition].displayName

        mMyViewModel.translateText(targetLanguage, data)

        mMyViewModel.isTranslating.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                mBinding.progressBar.visibility = VISIBLE
            } else {
                mBinding.progressBar.visibility = INVISIBLE
            }
        }

        mMyViewModel.translationResult.observe(viewLifecycleOwner, Observer { result ->
            mBinding.progressBar.visibility = INVISIBLE
            mBinding.targetText.text = result ?: "Translation failed"
        })
    }

    /**
     * detecting the language of receive data
     *
     * check the language of @param data
     * **/
    private fun detectTheLanguage(data: String) {
        Log.d(TAG, "detectTheLanguage: Detecting language of $data")

        // Trigger language detection in the ViewModel
        mMyViewModel.detectLanguageOfText(data)

        // Observe the result and update the Spinner selection
        mMyViewModel.detectLanguage.observe(viewLifecycleOwner, Observer { result ->
            result?.let { detectedLanguage ->
                Log.d(TAG, "detectTheLanguage: Detected language is $detectedLanguage")

                // Find the position of the detected language in the Spinner's adapter
                val position =
                    supportedLanguages.indexOfFirst {
                        it.displayName.trim().equals(detectedLanguage.trim(), ignoreCase = true)
                    }

                Log.d(TAG, "detectTheLanguage: position = $position")
                if (position != -1) {
                    // Set the Spinner's selection to the detected language
                    mBinding.sourceLang.setSelection(position)
                } else {
                    // Handle case where detected language is not in the list
                    Log.d(TAG, "detectTheLanguage: Language not found in supported languages.")
                    mBinding.sourceLang.setSelection(0)
                }
            } ?: run {
                Log.d(TAG, "detectTheLanguage: Detection failed, setting default value.")
                mBinding.sourceLang.setSelection(0)
            }
        })
    }

    companion object {
        private const val TAG = "TextFragment"
    }
}
