package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.stemaker.arbeitsbericht.R

abstract class ReportEditorSectionFragment : Fragment() {
    private var listener: OnExpandChange? = null
    lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_report_editor_section, container, false)
        val pseudoBtn: LinearLayout = rootView.findViewById(R.id.resh_headline_container)
        val imgV: ImageView = rootView.findViewById(R.id.resh_expand_content_pic)
        pseudoBtn.setOnClickListener(object: View.OnClickListener {
            override fun onClick(cV: View) {
                onClickExpandContentButton(imgV)
            }
        })
        rootView.findViewById<TextView>(R.id.resh_headline_textview).textSize =
            when(resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_NORMAL ->  20.toFloat()
            Configuration.SCREENLAYOUT_SIZE_SMALL -> 16.toFloat()
            else -> 26.toFloat()
        }
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

    fun onClickExpandContentButton(imgV: View) {
        Log.d("Arbeitsbericht.ReportEditorSectionFragment.onClickExpandContentButton", "called")
        listener?.let {
            if (it.getVisibility()) {
                imgV.rotation = 0.toFloat()
                it.setVisibility(false)
            } else {
                imgV.rotation = 180.toFloat()
                it.setVisibility(true)
            }
        }

    }

    interface OnExpandChange {
        fun setVisibility(vis: Boolean)
        fun getVisibility(): Boolean
    }

}
