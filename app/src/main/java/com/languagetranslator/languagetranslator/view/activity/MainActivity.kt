package com.languagetranslator.languagetranslator.view.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.languagetranslator.languagetranslator.R
import com.languagetranslator.languagetranslator.databinding.ActivityMainBinding
import com.languagetranslator.languagetranslator.utils.NetworkUtils
import com.languagetranslator.languagetranslator.view.fragments.ImageFragment
import com.languagetranslator.languagetranslator.view.fragments.SpeechFragment
import com.languagetranslator.languagetranslator.view.fragments.TextFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mBinding: ActivityMainBinding
    private val mTextFragment by lazy { TextFragment() }
    private val mImageFragment by lazy { ImageFragment() }
    private val mSpeechFragment by lazy { SpeechFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: create activity")
        enableEdgeToEdge()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        //checking the internet connection
        checkInternetConnection()

        //chang the color of status bar
        changeStatusBarColor()

        // Apply system bars padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //By default fragment
        openFragment(mTextFragment)

        //bottom navigation bar item selection
        mBinding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.text -> {
                    openFragment(mTextFragment)
                    true
                }

                R.id.image -> {
                    openFragment(mImageFragment)
                    true
                }

                R.id.speech -> {
                    openFragment(mSpeechFragment)
                    true
                }

                else -> false
            }
        }

    }


    //check the internet is available or not
    private fun checkInternetConnection() {
        Log.d(TAG, "checkInternet: checking internet connection")
        if (NetworkUtils.isInternetAvailable()) {
            Log.d(TAG, "checkInternetConnection: internet connection is available")
        } else {
            showNoInternetDialog()
        }
    }

    //If internet is not available then shows the dialog box
    fun showNoInternetDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection.")
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ ->
                if (NetworkUtils.isInternetAvailable()) {
                    ToastHelper.toast("Internet is now available!")
                } else {
                    showNoInternetDialog() // Retry if still no internet
                }
            }
            .setNegativeButton("Close App") { _, _ ->
                finish()
            }
            .create()
        dialog.show()
    }


    //status bar color change
    @Suppress("DEPRECATION")
    private fun changeStatusBarColor() {
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.color_primary)
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

    }

    //All clicks are here
    override fun onClick(view: View?) {
        Log.d(TAG, "onClick: $view clicked")
        when (view?.id) {
            R.id.text -> openFragment(mTextFragment)
            R.id.image -> openFragment(mImageFragment)
            R.id.speech -> openFragment(mSpeechFragment)
        }
    }

    //open the fragment
    private fun openFragment(fragment: Fragment) {
        Log.d(TAG, "openFragment: opening fragment $fragment")
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_view, fragment)
        transaction.commit()
        Log.d(TAG, "openFragment: ${fragment::class.java.simpleName}")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
