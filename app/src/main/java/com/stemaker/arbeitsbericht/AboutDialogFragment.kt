package com.stemaker.arbeitsbericht

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.DialogFragment

private const val TAG = "AboutDialogFragment"

class AboutDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_about_dialog, container, false)
        val btn: Button = v.findViewById(R.id.ok_button)
        btn.setOnClickListener { dismiss() }
        Log.d(TAG, "loading about.html into WebView")
        val wv = v.findViewById<WebView>(R.id.about_webview)
        wv.loadUrl("file:///android_asset/about.html")
        return v
    }

    override fun onResume() {
        super.onResume()
        val params = dialog!!.window!!.attributes!!
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params
    }
}

