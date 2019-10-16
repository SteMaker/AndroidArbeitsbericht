package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.stemaker.arbeitsbericht.R

abstract class ReportEditorSectionFragment : Fragment() {
    private var listener: OnExpandChange? = null
    var visible: Boolean = false
    lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_report_editor_section, container, false)
        val btn: ImageButton = rootView.findViewById(R.id.resh_expand_content_button)
        btn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                onClickExpandContentButton(btn)
            }
        })
        return rootView
    }

    fun onAttach(context: Context, child: Fragment) {
        super.onAttach(context)
        if (child is OnExpandChange) {
            listener = child
        } else {
            throw RuntimeException(context.toString() + " must implement OnExpandChange")
        }
    }

    fun setHeadline(t: String) {
        rootView.findViewById<TextView>(R.id.resh_headline_textview).text = t
    }

    fun onClickExpandContentButton(btn: View) {
        Log.d("Arbeitsbericht.ReportEditorSectionFragment.onClickExpandContentButton", "called")
        if (!visible) {
            btn.rotation = 180.toFloat()
            listener?.setVisibility(true)
            visible = true
        } else {
            btn.rotation = 0.toFloat()
            listener?.setVisibility(false)
            visible = false
        }

    }

    interface OnExpandChange {
        fun setVisibility(vis: Boolean)
    }

}
