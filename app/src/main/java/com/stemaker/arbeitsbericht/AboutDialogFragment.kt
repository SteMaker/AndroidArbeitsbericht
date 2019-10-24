package com.stemaker.arbeitsbericht

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import android.webkit.WebView

class AboutDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_about_dialog, container, false)
        val btn: Button = v.findViewById(R.id.ok_button)
        btn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                dismiss()
            }
        })
        Log.d("Arbeitsbericht.AboutDialogFragment.onCreateView", "loading about.html into WebView")
        val wv = v.findViewById<WebView>(R.id.about_webview)
        //wv.loadDataWithBaseURL("", "<!DOCTYPE html><html><body><h1>Lizenz</h1></body></html>", "text/html", "UTF-8", "")
        wv.loadUrl("file:///android_asset/about.html")
        return v
    }
}
