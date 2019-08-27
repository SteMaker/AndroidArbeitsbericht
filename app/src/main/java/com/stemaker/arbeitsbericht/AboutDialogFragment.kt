package com.stemaker.arbeitsbericht

import android.os.Bundle
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
        v.findViewById<WebView>(R.id.about_webview).loadUrl("file:///android_asset/about.html")
        return v
    }
}
