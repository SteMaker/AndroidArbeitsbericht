package com.stemaker.arbeitsbericht.helpers

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences

// @TODO(Change to data binding)
class OrientationNotificationDialogFragment(private val prefs: AbPreferences): DialogFragment() {
    private var listener: ForcePortraitListener? = null

    interface ForcePortraitListener {
        fun forcePortrait()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_orientation_notification_dialog, container, false)
        v.findViewById<SwitchMaterial>(R.id.always_landscape).isChecked = prefs.lockScreenOrientation.value
        v.findViewById<Button>(R.id.close_button).setOnClickListener { dismiss() }
        return v
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        prefs.lockScreenOrientation.value = view?.findViewById<SwitchMaterial>(R.id.always_landscape)?.isChecked?:false
        prefs.lockScreenOrientationNoInfo.value = view?.findViewById<SwitchMaterial>(R.id.no_show)?.isChecked?:false
        if(prefs.lockScreenOrientation.value)
            listener?.forcePortrait()
    }

    fun setForcePortraitListener(l: ForcePortraitListener) {
        listener = l
    }
}