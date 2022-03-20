package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.WorkItemContainerData
import com.stemaker.arbeitsbericht.data.WorkItemData
import com.stemaker.arbeitsbericht.databinding.FragmentWorkItemEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkItemLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WorkItemEditorFragment : ReportEditorSectionFragment() {
    lateinit var dataBinding: FragmentWorkItemEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentWorkItemEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.workitems))

        dataBinding.lifecycleOwner = viewLifecycleOwner
        GlobalScope.launch(Dispatchers.Main) {
            listener?.let { listener ->
                val report = listener.getReportData()
                report?.let { report ->
                    val workItemContainerData = report.workItemContainer
                    dataBinding.workItemContainerData = workItemContainerData

                    for (wi in workItemContainerData.items) {
                        addWorkItemView(wi, workItemContainerData)
                    }

                    dataBinding.workItemAddButton.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(btn: View) {
                            val wi = workItemContainerData.addWorkItem()
                            addWorkItemView(wi, workItemContainerData)
                        }
                    })
                }
            }
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

    fun addWorkItemView(wi: WorkItemData, workItemContainerData: WorkItemContainerData) {
        val inflater = layoutInflater
        val container = dataBinding.workItemContentContainer
        val workItemDataBinding: WorkItemLayoutBinding = WorkItemLayoutBinding.inflate(inflater, null, false)
        workItemDataBinding.workItem = wi
        workItemDataBinding.workItemContainer = workItemContainerData
        workItemDataBinding.lifecycleOwner = activity
        workItemDataBinding.workItemDelButton.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        container.removeView(workItemDataBinding.root)
                        workItemContainerData.removeWorkItem(wi)
                    } else {
                    }
                }
            }
        })

        val pos = container.getChildCount()
        container.addView(workItemDataBinding.root, pos)
    }

}
