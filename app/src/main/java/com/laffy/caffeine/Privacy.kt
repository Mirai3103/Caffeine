package com.laffy.caffeine

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView

/**
 * Play requires the privacy policy to be reachable from inside the app, not only from the
 * store listing. The app has no INTERNET permission, so the policy opens in the user's browser.
 */
object Privacy {
    const val PRIVACY_POLICY_URL = "https://share.web.huuhoang.tech/privacy/caffeine.html"

    /** Opens [PRIVACY_POLICY_URL] in a browser; falls back to a dialog with a copyable URL. */
    fun openPrivacyPolicy(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showUrlDialog(context)
        }
    }

    private fun showUrlDialog(context: Context) {
        val message = TextView(context).apply {
            setText(R.string.privacy_unavailable)
            append("\n\n")
            append(PRIVACY_POLICY_URL)
            Linkify.addLinks(this, Linkify.WEB_URLS)
            movementMethod = LinkMovementMethod.getInstance()
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.privacy)
            .setView(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
