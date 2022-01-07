package com.stemaker.arbeitsbericht.helpers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.stemaker.arbeitsbericht.R

private const val TAG = "HelperDialog"

class HelperDialogFragment: DialogFragment() {
    var url: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val savedUrl = savedInstanceState?.getString("URL")?:""
        if(savedUrl != "") url = savedUrl
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_helper_dialog, container, false)
        val btn: Button = v.findViewById(R.id.ok_button)
        btn.setOnClickListener { dismiss() }
        if(url != "") {
            val wv = v.findViewById<WebView>(R.id.version_webview)
            wv.loadUrl(url)
        } else {
            Log.e(TAG, "No URL defined")
        }
        return v
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("URL", url)
    }

    override fun onResume() {
        super.onResume()
        val params = dialog!!.window!!.attributes!!
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params
    }
}