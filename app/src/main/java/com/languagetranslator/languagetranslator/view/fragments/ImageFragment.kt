@file:Suppress("DEPRECATION")

package com.languagetranslator.languagetranslator.view.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.languagetranslator.languagetranslator.R
import com.languagetranslator.languagetranslator.databinding.FragmentImageBinding
import com.languagetranslator.languagetranslator.datamodel.supportedLanguages
import com.languagetranslator.languagetranslator.utils.Keys
import com.languagetranslator.languagetranslator.viewmodel.MyViewModel
import java.util.Locale

class ImageFragment : Fragment(), View.OnClickListener, TextToSpeech.OnInitListener {
    private var _binding: FragmentImageBinding? = null
    private val mBinding get() = _binding!!
    private lateinit var mMyViewModel: MyViewModel
    private val mTranslationUsingGemini by lazy { TranslationUsingGemini() }
    private lateinit var mSpeech: TextToSpeech

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var imgBitmap: Bitmap

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            imgBitmap = imageBitmap
            mBinding.imageCardView.visibility = VISIBLE
            mBinding.info.visibility = INVISIBLE
            mBinding.imageView.load(imageBitmap)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Log.d(TAG, "Permission denied")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: create view for image fragment")
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        mMyViewModel = ViewModelProvider(this)[MyViewModel::class.java]
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: view created for fragment image")
        mSpeech = TextToSpeech(requireContext(), this)
        setUpToolbar()
        setUpSpinner()
        seOnClickListener()
    }


    private fun setUpSpinner() {
        Log.d(TAG, "setUpSpinner: set upping spinner")
        val adapter = ArrayAdapter(requireContext(),
            R.layout.spinner_layout,
            supportedLanguages.map { it.displayName }
        )

        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        mBinding.targetLanguage.adapter = adapter
        mBinding.targetLanguage.setSelection(29)
    }

    private fun seOnClickListener() {
        Log.d(TAG, "seOnClickListener: set on click listener clicked")
        mBinding.addImageView.setOnClickListener(this)
        mBinding.translateImageBtn.setOnClickListener(this)
        mBinding.speakOriginalText.setOnClickListener(this)
        mBinding.speakTranslatedText.setOnClickListener(this)
    }

    private fun setUpToolbar() {
        Log.d(TAG, "setUpToolbar: toolbar set upping at image fragment")
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(mBinding.imageToolbar)
        }
    }


    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        Log.d(TAG, "requestPermission: requesting for permission")
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        Log.d(TAG, "openCamera: opening camera")
        val takeImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            Log.d(TAG, "openCamera: $takeImage")
            cameraLauncher.launch(takeImage)
        } catch (e: Exception) {
            Log.d(TAG, "openCamera: activity not found exception occur")
            Log.d(TAG, "openCamera: exception ${e.message}")
        }
    }

    override fun onClick(p0: View?) {
        Log.d(TAG, "onClick: clicking $p0")
        when (p0?.id) {
            mBinding.addImageView.id -> {
                if (checkPermission()) {
                    openCamera()
                } else {
                    requestPermission()
                }
            }

            mBinding.translateImageBtn.id -> {
                if (mBinding.imageView.isShown) {
                    extractTextAndTranslate()
                } else
                    ToastHelper.toast("No image found")
            }

            mBinding.speakOriginalText.id -> {
                monitorPlayState(Keys.ORIGINAL_TEXT)
                val imageText = mBinding.imageResult.text.toString()
                if (imageText.isEmpty()) {
                    Log.d(TAG, "onClick: No text to speak")
                    ToastHelper.toast("No text to speak")
                } else {
                    speakOriginalText(imageText)
                }
            }

            mBinding.speakTranslatedText.id -> {
                monitorPlayState(Keys.TRANSLATED_TEXT)
                val translatedText = mBinding.translatedTextView.text.toString()
                if (translatedText.isEmpty()) {
                    Log.d(TAG, "onClick: No translated text to speak")
                    ToastHelper.toast("No translated text to speak")
                } else {
                    speakTheTranslatedText(translatedText)
                }
            }

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
            resetToPlayState(Keys.ORIGINAL_TEXT)
        } else {
            // Start speaking and switch to pause state
            mBinding.speakOriginalText.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_pause_24))
            speakText(originalText)
            isOriginalSpeaking = true
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

        val selectedLanguage = supportedLanguages[mBinding.targetLanguage.selectedItemPosition]
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
            resetToPlayState(Keys.TRANSLATED_TEXT)
        } else {
            mBinding.speakTranslatedText.setImageDrawable(
                getDrawable(requireContext(), R.drawable.ic_pause_24)
            )
            speakText(text)
            isTranslatedSpeaking = true
        }
    }
    private fun monitorPlayState(resource: String){
        Log.d(TAG, "monitorPlayState: resource = $resource")
        mSpeech.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "onStart: speaking starts")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "onDone: speaking done for $resource")
                if(resource == Keys.ORIGINAL_TEXT){
                    Log.d(TAG, "onDone: done for original text")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Keys.ORIGINAL_TEXT)
                    }
                }
                else{
                    Log.d(TAG, "onDone: done for translated text")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Keys.TRANSLATED_TEXT)
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                if(resource == Keys.ORIGINAL_TEXT){
                    Log.d(TAG, "onError: error on original text")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Keys.ORIGINAL_TEXT)
                    }
                }
                else{
                    Log.d(TAG, "onError: error on translated text")
                    requireActivity().runOnUiThread {
                        resetToPlayState(Keys.TRANSLATED_TEXT)
                    }
                }
            }
        })
    }

    private fun resetToPlayState(resource: String){
        Log.d(TAG, "resetToPlayState: resource = $resource")
        if(resource == Keys.ORIGINAL_TEXT){
            Log.d(TAG, "resetToPlayState: state change for original text")
            requireActivity().runOnUiThread {
                mBinding.speakOriginalText.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_play_24))
                isOriginalSpeaking = false
            }
        } else{
            Log.d(TAG, "resetToPlayState: state change for translate text")
            requireActivity().runOnUiThread {
                mBinding.speakTranslatedText.setImageDrawable(getDrawable(requireContext(), R.drawable.ic_play_24))
                isTranslatedSpeaking = false
            }
        }
    }

    private fun speakText(text: String) {
        Log.d(TAG, "speakOriginalText: speak the $text")
        val param = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID")
        }
        mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, param, "UniqueID")
    }

    override fun onPause() {
        super.onPause()
        if (mSpeech.isSpeaking) {
            mSpeech.stop()
        }
    }

    private fun extractTextAndTranslate() {
        Log.d(TAG, "extractTextAndTranslate: starting text extraction")
        val image = InputImage.fromBitmap(imgBitmap, 0)
        mBinding.progressBarOrg.visibility = VISIBLE

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val resultText = result.text
                if (resultText.isNotEmpty()) {
                    Log.d(TAG, "Text extracted: $resultText")
                    mBinding.progressBarOrg.visibility = INVISIBLE
                    mBinding.imageResult.text = resultText

                    //identifyingLanguage(resultText)
                    mTranslationUsingGemini.translateItUsingGemini(resultText)
                } else {
                    ToastHelper.toast("No text found in the image")
                    mBinding.progressBarOrg.visibility = INVISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to extract text: ${e.message}")
                ToastHelper.toast("Failed to extract the text")
                mBinding.progressBarOrg.visibility = INVISIBLE

            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        mSpeech.shutdown()
        mSpeech.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: view destroy")
        _binding = null
        if (::mSpeech.isInitialized) {
            mSpeech.stop()
            mSpeech.shutdown()
        }
        mSpeech.stop()
        mSpeech.shutdown()
    }

    override fun onInit(status: Int) {
        Log.d(TAG, "onInit: status $status")
        if (status == TextToSpeech.SUCCESS) {
            // Set the language
            val result = mSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Language not supported or missing data")
            } else {
                Log.d(TAG, "TextToSpeech initialized successfully")
            }
        } else {
            Log.d(TAG, "TextToSpeech initialization failed")
        }
    }

    companion object {
        private const val TAG = "ImageFragment"
    }


    inner class TranslationUsingGemini {
        @SuppressLint("SetTextI18n")
        fun translateItUsingGemini(text: String) {
            Log.d(TAG, "translateItUsingGemini: ")
            mBinding.progressBarTrs.visibility = VISIBLE

            val selectLanguage =
                supportedLanguages[mBinding.targetLanguage.selectedItemPosition].displayName
            Log.d(TAG, "translateText: Target language code: $selectLanguage")

            // Trigger translation
            mMyViewModel.translateText(selectLanguage, text)

            // Observe the result
            mMyViewModel.translationResult.observe(viewLifecycleOwner, Observer { result ->
                mBinding.progressBarTrs.visibility = INVISIBLE
                if(result.isNullOrEmpty()){
                    mBinding.progressBarTrs.visibility = INVISIBLE
                    mBinding.translatedTextView.text = "Translation failed"
                }else{
                    mBinding.progressBarTrs.visibility = INVISIBLE
                    mBinding.translatedTextView.text = result
                }
            })
        }
    }

    inner class TranslationUsingFirebaseMlKit {
        private fun identifyingLanguage(text: String) {
            Log.d(TAG, "identifyingLanguage: identifying the language of $text")

            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    if (languageCode == "und") {
                        Log.d(TAG, "identifyingLanguage: Can't identify language")
                        Toast.makeText(
                            requireContext(),
                            "Can't identify language",
                            Toast.LENGTH_SHORT
                        )
                            .show()

                    } else {
                        Log.d(TAG, "identifyingLanguage: language found : $languageCode")
                        translateText(text, languageCode)
                    }
                }
                .addOnFailureListener {
                    Log.d(TAG, "identifyingLanguage: failed...")
                }
        }

        private fun translateText(originalText: String, sourceLanguageCode: String) {
            Log.d(TAG, "translateText: Translating text")

            // Get the selected target language code
            val selectLanguageCode =
                supportedLanguages[mBinding.targetLanguage.selectedItemPosition].code
            Log.d(TAG, "translateText: Source language code: $sourceLanguageCode")
            Log.d(TAG, "translateText: Target language code: $selectLanguageCode")

            // Validate input
            if (originalText.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "No text provided to translate",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            if (selectLanguageCode.isEmpty()) {
                Toast.makeText(requireContext(), "No target language selected", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            if (sourceLanguageCode == "und") {
                Toast.makeText(requireContext(), "Language unidentified", Toast.LENGTH_SHORT).show()
                return
            }

            // Create TranslatorOptions
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(selectLanguageCode)
                .build()

            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()

            val progressDialog = ProgressDialog(requireContext()).apply {
                setMessage("Downloading translation model...")
                setCancelable(false)
                show()
            }

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Model successfully Downloaded",
                        Toast.LENGTH_SHORT
                    ).show()
                    //translating source to target
                    translator.translate(originalText)
                        .addOnSuccessListener { result ->
                            Log.d(TAG, "translateText: original text = $originalText")
                            Log.d(TAG, "translateText: result = $result")
                            mBinding.translatedTextView.text = result
                        }
                        .addOnFailureListener { e ->
                            Log.d(TAG, "Error during translation: ${e.message}")
                            Toast.makeText(
                                requireContext(),
                                "Failed to translate text",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Error downloading model: ${e.message}")
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Failed to download translation model",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}

