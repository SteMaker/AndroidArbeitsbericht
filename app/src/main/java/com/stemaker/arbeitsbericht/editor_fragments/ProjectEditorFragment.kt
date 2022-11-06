package com.stemaker.arbeitsbericht.editor_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.databinding.FragmentProjectEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProjectEditorFragment : ReportEditorSectionFragment() {
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

        dataBinding.lifecycleOwner =viewLifecycleOwner

        GlobalScope.launch(Dispatchers.Main) {
            listener?.let { listener ->
                // There can be a race with onDetach so that the listener is already null
                val report = listener.getReportData()
                report?.let { report ->
                    dataBinding.projectData = report.project
                }
            }
        }
        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.projectContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.projectContentContainer.visibility != View.GONE
    }
}
