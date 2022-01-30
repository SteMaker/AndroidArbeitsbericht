package com.stemaker.arbeitsbericht.helpers

import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.configuration

class OrientationNotificationDialogFragment: DialogFragment() {
    private var listener: ForcePortraitListener? = null

    interface ForcePortraitListener {
        fun forcePortrait()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_orientation_notification_dialog, container, false)
        v.findViewById<SwitchMaterial>(R.id.always_landscape).isChecked = configuration().lockScreenOrientation
        v.findViewById<Button>(R.id.close_button).setOnClickListener { dismiss() }
        return v
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        configuration().lockScreenOrientation = view?.findViewById<SwitchMaterial>(R.id.always_landscape)?.isChecked?:false
        configuration().lockScreenOrientationNoInfo = view?.findViewById<SwitchMaterial>(R.id.no_show)?.isChecked?:false
        configuration().save()
        if(configuration().lockScreenOrientation)
            listener?.forcePortrait()
    }

    fun setForcePortraitListener(l: ForcePortraitListener) {
        listener = l
    }
}