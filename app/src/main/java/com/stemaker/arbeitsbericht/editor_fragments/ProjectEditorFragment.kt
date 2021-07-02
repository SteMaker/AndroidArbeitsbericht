package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.ProjectData
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentProjectEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProjectEditorFragment : ReportEditorSectionFragment() {
    private var listener: OnProjectEditorInteractionListener? = null
    lateinit var dataBinding: FragmentProjectEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","ProjectEditorFragment.onCreate called")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","ProjectEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_project_editor, null, false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline("Projekt / Kunde")

        dataBinding.lifecycleOwner = this

        GlobalScope.launch(Dispatchers.Main) {
            dataBinding.projectData = listener!!.getProjectData()
            dataBinding.reportData = listener!!.getReportData()
        }
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnProjectEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnProjectEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.project_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    override fun getVisibility(): Boolean {
        return dataBinding.root.findViewById<LinearLayout>(R.id.project_content_container).visibility != View.GONE
    }

    interface OnProjectEditorInteractionListener {
        suspend fun getProjectData(): ProjectData
        suspend fun getReportData(): ReportData
    }

}
