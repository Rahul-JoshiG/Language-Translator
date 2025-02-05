package com.languagetranslator.languagetranslator.view.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.languagetranslator.languagetranslator.R
import com.languagetranslator.languagetranslator.databinding.FragmentSpeechBinding
import com.languagetranslator.languagetranslator.datamodel.supportedLanguages
import com.languagetranslator.languagetranslator.utils.Constant
import com.languagetranslator.languagetranslator.utils.DialogUtils
import com.languagetranslator.languagetranslator.utils.NetworkUtils
import com.languagetranslator.languagetranslator.viewmodel.MyViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SpeechFragment : Fragment(){

    @Inject
    lateinit var mSpeech: TextToSpeech

    private var _binding: FragmentSpeechBinding? = null

    private val mBinding get() = _binding!!
    private lateinit var mMyViewModel: MyViewModel

    // Declare the ActivityResultLauncher for speech recognition
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // Initialize the ActivityResultLauncher
    private fun initActivityResultLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val resultData =
                        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenData = resultData?.get(0)
                    mBinding.speechText.text = spokenData
                    mBinding.progressBar.visibility = VISIBLE
                    translateSpokenData(spokenData)
                }
            }
    }

    private fun translateSpokenData(text: String?) {
        Log.d(TAG, "translateSpokenData: spoken text = $text")
        val targetLanguage =
            supportedLanguages[mBinding.spinnerTargetLanguage.selectedItemPosition].displayName
        mMyViewModel.translateText(target = targetLanguage, data = text.orEmpty())

        mMyViewModel.isTranslating.observe(viewLifecycleOwner){isLoading->
            if(isLoading){
                mBinding.progressBar.visibility = VISIBLE
            }else{
                mBinding.progressBar.visibility = INVISIBLE
            }
        }
        mMyViewModel.translationResult.observe(viewLifecycleOwner, Observer { result ->
            mBinding.speechTranslateText.text = result ?: "Translation Failed"
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: creating view of speech fragment")
        _binding = FragmentSpeechBinding.inflate(inflater, container, false)
        mMyViewModel = ViewModelProvider(this)[MyViewModel::class.java]
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: view created for speech fragment")

        // Initialize the TextToSpeech and ActivityResultLauncher
        initActivityResultLauncher()  // Initialize ActivityResultLauncher
        setUpToolbar()
        setUpSpinner()
        setOnClickListener()
    }

    private fun setOnClickListener() {
        Log.d(TAG, "setOnClickListener: Setting listeners for views")

        mBinding.addSpeechView.setOnClickListener {
            mBinding.info.visibility = INVISIBLE
            showTextFromSpeech()
        }

        mBinding.translateButton.setOnClickListener {
            val speechData = mBinding.speechText.text.toString()
            if (speechData.isEmpty()) {
                Toast.makeText(context, "Please provide the text to translate.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                mBinding.progressBar.visibility = VISIBLE
                translateSpokenData(speechData)
            }
        }

        mBinding.speakOriginalText.setOnClickListener {
            val data = mBinding.speechText.text.toString()
            monitorPlayState(Constant.ORIGINAL_TEXT)
            speakOriginalText(originalText = data)
        }

        mBinding.speakTranslatedText.setOnClickListener {
            val data = mBinding.speechTranslateText.text.toString()
            monitorPlayState(Constant.TRANSLATED_TEXT)
            speakTheTranslatedText(data)
        }
    }

    var isOriginalSpeaking = false
    private fun speakOriginalText(originalText: String) {
        if (originalText.isEmpty()) {
            Toast.makeText(context, "No text available to speak!", Toast.LENGTH_SHORT).show()
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
        if (text.isEmpty()) {
            Toast.makeText(context, "No text available for speech!", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedLanguage =
            supportedLanguages[mBinding.spinnerTargetLanguage.selectedItemPosition]
        val locale = Locale.forLanguageTag(selectedLanguage.code)

        if (mSpeech.setLanguage(locale) in arrayOf(
                TextToSpeech.LANG_MISSING_DATA,
                TextToSpeech.LANG_NOT_SUPPORTED
            )
        ) {
            Toast.makeText(context, "Selected language is not supported for speech", Toast.LENGTH_SHORT).show()
            return
        }

        if (isTranslatedSpeaking) {
            if (mSpeech.isSpeaking) mSpeech.stop()
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
        mSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                resetToPlayState(resource)
            }

            override fun onError(utteranceId: String?) {
                resetToPlayState(resource)
            }
        })
    }

    private fun resetToPlayState(resource: String) {
        when (resource) {
            Constant.ORIGINAL_TEXT -> mBinding.speakOriginalText.setImageDrawable(
                getDrawable(requireContext(), R.drawable.ic_play_24)
            )

            Constant.TRANSLATED_TEXT -> mBinding.speakTranslatedText.setImageDrawable(
                getDrawable(requireContext(), R.drawable.ic_play_24)
            )
        }
        isOriginalSpeaking = false
        isTranslatedSpeaking = false
    }

    private fun speakText(text: String) {
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID")
        }
        mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "UniqueID")
    }

    private fun showTextFromSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Something...")
        }
        activityResultLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!NetworkUtils.isInternetAvailable(requireActivity())) {
            DialogUtils.showNoInternetDialog(requireActivity())
        }
    }

    private fun setUpSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_layout,
            supportedLanguages.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        mBinding.spinnerTargetLanguage.adapter = adapter
        mBinding.spinnerTargetLanguage.setSelection(18)
    }

    private fun setUpToolbar() {
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(mBinding.speechToolbar)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SpeechFragment"
    }
}
