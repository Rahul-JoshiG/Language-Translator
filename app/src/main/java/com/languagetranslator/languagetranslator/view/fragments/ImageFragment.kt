@file:Suppress("DEPRECATION")

package com.languagetranslator.languagetranslator.view.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.languagetranslator.languagetranslator.R
import com.languagetranslator.languagetranslator.databinding.FragmentImageBinding
import com.languagetranslator.languagetranslator.datamodel.supportedLanguages
import com.languagetranslator.languagetranslator.utils.Constant
import com.languagetranslator.languagetranslator.utils.DialogUtils
import com.languagetranslator.languagetranslator.utils.NetworkUtils
import com.languagetranslator.languagetranslator.viewmodel.MyViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ImageFragment : Fragment(), View.OnClickListener {

    @Inject
    lateinit var mSpeech: TextToSpeech // Injected by Hilt

    private var _binding: FragmentImageBinding? = null
    private val mBinding get() = _binding!!

    private lateinit var mMyViewModel: MyViewModel

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var imgBitmap: Bitmap

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

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

        // Set the OnInitListener

        initCameraLauncher()
        initGalleryLauncher()
        setUpSpinner()
        seOnClickListener()
    }

    private fun initCameraLauncher() {
        Log.d(TAG, "initCameraLauncher: initial launcher ")
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                imgBitmap = imageBitmap
                mBinding.apply {
                    imageView.visibility = VISIBLE
                    addImageView.isEnabled = false
                    addImageView.visibility = INVISIBLE
                    info.visibility = INVISIBLE
                    imageView.load(imageBitmap)
                }
            }
        }
    }

    private fun initGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val imageBitmap = BitmapFactory.decodeStream(inputStream)
                        imgBitmap = imageBitmap
                        mBinding.apply {
                            imageView.visibility = VISIBLE
                            addImageView.isEnabled = false
                            addImageView.visibility = INVISIBLE
                            info.visibility = INVISIBLE
                            imageView.load(imageBitmap)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load image from gallery: ${e.message}")
                        ToastHelper.toast("Failed to load image from gallery")
                    }
                }
            }
        }
    }

    private fun setUpSpinner() {
        Log.d(TAG, "setUpSpinner: set upping spinner")
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_layout,
            supportedLanguages.map { it.displayName }
        )

        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        mBinding.targetLanguage.adapter = adapter
        mBinding.targetLanguage.setSelection(29)
    }

    override fun onResume() {
        super.onResume()
        if (!NetworkUtils.isInternetAvailable(requireActivity())) {
            DialogUtils.showNoInternetDialog(requireActivity())
        }
    }

    private fun seOnClickListener() {
        Log.d(TAG, "seOnClickListener: set on click listener clicked")
        mBinding.addImageView.setOnClickListener(this)
        mBinding.translateImageBtn.setOnClickListener(this)
        mBinding.speakOriginalText.setOnClickListener(this)
        mBinding.speakTranslatedText.setOnClickListener(this)
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

    private fun openGallery() {
        if (checkPermission()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        } else {
            requestPermission()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Camera option
                        if (checkPermission()) {
                            openCamera()
                        } else {
                            requestPermission()
                        }
                    }

                    1 -> { // Gallery option
                        openGallery()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onClick(p0: View?) {
        Log.d(TAG, "onClick: clicking $p0")
        when (p0?.id) {
            mBinding.addImageView.id -> {
                if (checkPermission()) {
                    showImageSourceDialog()
                } else {
                    requestPermission()
                }
            }

            mBinding.translateImageBtn.id -> {
                if (mBinding.imageView.isShown) {
                    extractTextAndTranslate()
                } else {
                    ToastHelper.toast("No image found")
                }
            }

            mBinding.speakOriginalText.id -> {
                monitorPlayState(Constant.ORIGINAL_TEXT)
                val imageText = mBinding.imageResult.text.toString()
                if (imageText.isEmpty()) {
                    Log.d(TAG, "onClick: No text to speak")
                    ToastHelper.toast("No text to speak")
                } else {
                    speakOriginalText(imageText)
                }
            }

            mBinding.speakTranslatedText.id -> {
                monitorPlayState(Constant.TRANSLATED_TEXT)
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

    private var isOriginalSpeaking = false
    private fun speakOriginalText(originalText: String) {
        Log.d(TAG, "speakOriginalText: Text to speak = $originalText")

        if (originalText.isEmpty()) {
            ToastHelper.toast("No text available to speak!!!")
            return
        }

        if (isOriginalSpeaking) {
            if (mSpeech.isSpeaking) mSpeech.stop()
            resetToPlayState(Constant.ORIGINAL_TEXT)
        } else {
            mBinding.speakOriginalText.setImageDrawable(
                getDrawable(
                    requireContext(),
                    R.drawable.ic_pause_24
                )
            )
            speakText(originalText)
            isOriginalSpeaking = true
        }
    }

    private var isTranslatedSpeaking = false
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
            resetToPlayState(Constant.TRANSLATED_TEXT)
        } else {
            mBinding.speakTranslatedText.setImageDrawable(
                getDrawable(requireContext(), R.drawable.ic_pause_24)
            )
            speakText(text)
            isTranslatedSpeaking = true
        }
    }

    private fun monitorPlayState(resource: String) {
        Log.d(TAG, "monitorPlayState: resource = $resource")
        mSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "onStart: speaking starts")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "onDone: speaking done for $resource")
                requireActivity().runOnUiThread {
                    resetToPlayState(resource)
                }
            }

            override fun onError(utteranceId: String?) {
                Log.d(TAG, "onError: error on $resource")
                requireActivity().runOnUiThread {
                    resetToPlayState(resource)
                }
            }
        })
    }

    private fun resetToPlayState(resource: String) {
        Log.d(TAG, "resetToPlayState: resource = $resource")
        when (resource) {
            Constant.ORIGINAL_TEXT -> {
                mBinding.speakOriginalText.setImageDrawable(
                    getDrawable(
                        requireContext(),
                        R.drawable.ic_play_24
                    )
                )
                isOriginalSpeaking = false
            }

            Constant.TRANSLATED_TEXT -> {
                mBinding.speakTranslatedText.setImageDrawable(
                    getDrawable(
                        requireContext(),
                        R.drawable.ic_play_24
                    )
                )
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

                    // Trigger translation
                   translateItUsingGemini(resultText)
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

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: view destroy")
        _binding = null
    }

    companion object {
        private const val TAG = "ImageFragment"
    }

    @SuppressLint("SetTextI18n")
    private fun translateItUsingGemini(text: String) {
        Log.d(TAG, "translateItUsingGemini: ")

        val selectLanguage =
            supportedLanguages[mBinding.targetLanguage.selectedItemPosition].displayName
        Log.d(TAG, "translateText: Target language code: $selectLanguage")

        // Trigger translation
        mMyViewModel.translateText(selectLanguage, text)

        mMyViewModel.isTranslating.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                mBinding.progressBarTrs.visibility = VISIBLE
            } else {
                mBinding.progressBarTrs.visibility = INVISIBLE
            }
        }
        // Observe the result
        mMyViewModel.translationResult.observe(viewLifecycleOwner, Observer { result ->
            if (result.isNullOrEmpty()) {
                mBinding.translatedTextView.text = "Translation failed"
            } else {
                mBinding.translatedTextView.text = result
            }
        })
    }
}