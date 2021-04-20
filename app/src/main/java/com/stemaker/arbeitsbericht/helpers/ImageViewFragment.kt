package com.stemaker.arbeitsbericht.helpers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import android.widget.ImageView
import com.stemaker.arbeitsbericht.GlideApp
import com.stemaker.arbeitsbericht.R

private const val TAG = "ImageViewFragment"

class ImageViewFragment(val file: String) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_image_view, container, false)
        val imgV: ImageView = v.findViewById(R.id.photo_view)
        imgV.setOnClickListener(object: View.OnClickListener {
            override fun onClick(imgV: View) {
                dismiss()
            }
        })
        GlideApp.with(this).load(file).into(imgV)
        return v
    }
}
