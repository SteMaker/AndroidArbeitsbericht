package com.stemaker.arbeitsbericht.editor_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentProjectEditorBinding
import com.stemaker.arbeitsbericht.view_models.ProjectViewModel
import com.stemaker.arbeitsbericht.view_models.ProjectViewModelFactory

class ProjectEditorFragment(private val report: ReportData):
    ReportEditorSectionFragment()
{
    lateinit var dataBinding: FragmentProjectEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","ProjectEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_project_editor, null, false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline("Projekt / Kunde")

        dataBinding.lifecycleOwner = viewLifecycleOwner
        val viewModel = ViewModelProvider(this, ProjectViewModelFactory(viewLifecycleOwner, report.project)).get(ProjectViewModel::class.java)
        dataBinding.viewModel = viewModel
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
