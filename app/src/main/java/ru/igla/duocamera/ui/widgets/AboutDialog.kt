package ru.igla.duocamera.ui.widgets

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import ru.igla.duocamera.BuildConfig
import ru.igla.duocamera.R
import timber.log.Timber
import java.util.*

class AboutDialog : DialogFragment() {

    companion object {
        private const val TAG = "[ABOUT_DIALOG]"

        fun show(context: FragmentActivity) =
            AboutDialog().show(context.supportFragmentManager, TAG)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val body = String.format(
            Locale.US,
            getString(R.string.about_body)
        )
        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.about_title, BuildConfig.VERSION_NAME))
            .setMessage(fromHtml(body))
            .setCancelable(true)
            .create()
    }

    @Suppress("DEPRECATION")
    @Nullable
    private fun fromHtml(source: String): Spanned? {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(source)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }
}