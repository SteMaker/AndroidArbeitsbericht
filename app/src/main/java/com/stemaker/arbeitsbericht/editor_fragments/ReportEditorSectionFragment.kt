package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.BillData
import com.stemaker.arbeitsbericht.data.ProjectData
import com.stemaker.arbeitsbericht.data.ReportData

private const val TAG = "ReportEditSectFragment"

abstract class ReportEditorSectionFragment : Fragment() {
    protected var listener: OnReportEditorInteractionListener? = null
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnReportEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnReportEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun setHeadline(t: String) {
        rootView.findViewById<TextView>(R.id.resh_headline_textview).text = t
    }

    fun onClickExpandContentButton(imgV: View) {
        if (getVisibility()) {
            imgV.rotation = 0.toFloat()
            setVisibility(false)
        } else {
            imgV.rotation = 180.toFloat()
            setVisibility(true)
        }
    }

    abstract fun setVisibility(vis: Boolean)
    abstract fun getVisibility(): Boolean

    interface OnReportEditorInteractionListener {
        suspend fun getReportData(): ReportData?
    }
}
