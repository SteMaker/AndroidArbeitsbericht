package com.stemaker.arbeitsbericht.editor_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentWorkItemEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkItemLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.view_models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WorkItemEditorFragment(
    private val report: ReportData,
    private val definedWorkItems: LiveData<Set<String>>
)
    : ReportEditorSectionFragment()
{
    lateinit var dataBinding: FragmentWorkItemEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentWorkItemEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.workitems))

        dataBinding.lifecycleOwner = viewLifecycleOwner
        val workItemContainerData = report.workItemContainer
        val viewModelContainer = ViewModelProvider(
            this,
            WorkItemContainerViewModelFactory(viewLifecycleOwner, workItemContainerData, definedWorkItems))
            .get(WorkItemContainerViewModel::class.java)
        dataBinding.viewModelContainer = viewModelContainer

        for (viewModel in viewModelContainer) {
            addWorkItemView(viewModel, viewModelContainer)
        }

        dataBinding.workItemAddButton.setOnClickListener {
            val viewModel = viewModelContainer.addWorkItem()
            val v = addWorkItemView(viewModel, viewModelContainer)
            listener?.scrollTo(v)
        }
        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.workItemContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.workItemContentContainer.visibility != View.GONE
    }

    private fun addWorkItemView(viewModel: WorkItemViewModel, viewModelContainer: WorkItemContainerViewModel): View {
        val inflater = layoutInflater
        val container = dataBinding.workItemContentContainer
        val workItemDataBinding: WorkItemLayoutBinding = WorkItemLayoutBinding.inflate(inflater, null, false)
        workItemDataBinding.viewModel = viewModel
        workItemDataBinding.viewModelContainer = viewModelContainer
        workItemDataBinding.lifecycleOwner = activity
        workItemDataBinding.workItemDelButton.setOnClickListener { btn ->
            GlobalScope.launch(Dispatchers.Main) {
                val answer =
                    showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                if (answer == AlertDialog.BUTTON_POSITIVE) {
                    container.removeView(workItemDataBinding.root)
                    viewModelContainer.removeWorkItem(viewModel)
                } else {
                }
            }
        }

        val pos = container.childCount
        container.addView(workItemDataBinding.root, pos)
        return workItemDataBinding.root
    }

}
