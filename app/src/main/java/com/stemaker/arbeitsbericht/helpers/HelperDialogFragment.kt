package com.stemaker.arbeitsbericht.helpers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.stemaker.arbeitsbericht.R

class HelperDialogFragment(val url: String): DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_helper_dialog, container, false)
        val btn: Button = v.findViewById(R.id.ok_button)
        btn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                dismiss()
            }
        })
        val wv = v.findViewById<WebView>(R.id.version_webview)
        wv.loadUrl(url)
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