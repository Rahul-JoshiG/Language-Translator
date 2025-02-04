package com.languagetranslator.languagetranslator.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object DialogUtils {
    fun showNoInternetDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection.")
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ ->
                if (NetworkUtils.isInternetAvailable(context)) {
                    Toast.makeText(context, "Internet is now available!", Toast.LENGTH_SHORT).show()
                } else {
                    showNoInternetDialog(context) // Retry if still no internet
                }
            }
            .setNegativeButton("Close App") { _, _ ->
                if (context is Activity) {
                    context.finish() // Close the activity properly
                }
            }
            .create()
        builder.show()
    }
}
