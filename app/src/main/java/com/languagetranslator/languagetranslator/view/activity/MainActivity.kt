package com.languagetranslator.languagetranslator.view.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.languagetranslator.languagetranslator.R
import com.languagetranslator.languagetranslator.databinding.ActivityMainBinding
import com.languagetranslator.languagetranslator.utils.DialogUtils
import com.languagetranslator.languagetranslator.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: create activity")
        enableEdgeToEdge()

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        applyWindowInsets()

        // Change the color of the status bar
        changeStatusBarColor()

        setUpNavigation() // Setup navigation

        // Set default fragment using the navigation graph
        if (savedInstanceState == null) {
            mNavController.navigate(R.id.textFragment) // or whichever is the initial fragment in the graph
        }

        // Set up Bottom Navigation View
        mBinding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.text -> {
                    mNavController.navigate(R.id.textFragment)
                    true
                }
                R.id.image -> {
                    mNavController.navigate(R.id.imageFragment)
                    true
                }
                R.id.speech -> {
                    mNavController.navigate(R.id.speechFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun setUpNavigation() {
        Log.d(TAG, "setUpNavigation: ")
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        mNavController = navHostFragment.navController
        NavigationUI.setupWithNavController(mBinding.bottomNav, mNavController)
    }

    private fun applyWindowInsets() {
        Log.d(TAG, "applyWindowInsets: ")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @Suppress("DEPRECATION")
    private fun changeStatusBarColor() {
        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.color_primary_variant)
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }

    override fun onResume() {
        super.onResume()
        if (!NetworkUtils.isInternetAvailable(this)) {
            DialogUtils.showNoInternetDialog(this)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}