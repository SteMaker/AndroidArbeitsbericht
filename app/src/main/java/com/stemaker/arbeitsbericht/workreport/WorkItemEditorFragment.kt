package com.stemaker.arbeitsbericht.workreport

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.workreport.WorkItemContainerData
import com.stemaker.arbeitsbericht.data.workreport.WorkItemData
import com.stemaker.arbeitsbericht.databinding.FragmentWorkItemEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkItemLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WorkItemEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnWorkItemEditorInteractionListener? = null
    var workItemContainerData: WorkItemContainerData? = null
    lateinit var dataBinding: FragmentWorkItemEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","WorkItemEditorFragment.onCreate called")
        workItemContainerData = listener!!.getWorkItemContainerData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","WorkItemEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentWorkItemEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.workitems))

        dataBinding.lifecycleOwner = this
        dataBinding.workItemContainerData = workItemContainerData!!

        for(wt in workItemContainerData!!.items) {
            addWorkItemView(wt)
        }

        dataBinding.root.findViewById<ImageButton>(R.id.work_item_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wi = workItemContainerData!!.addWorkItem()
                addWorkItemView(wi)
            }
        })

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context, this)
        if (context is OnWorkItemEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnWorkItemEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.work_item_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    override fun getVisibility(): Boolean {
        return dataBinding.root.findViewById<LinearLayout>(R.id.work_item_content_container).visibility != View.GONE
    }

    interface OnWorkItemEditorInteractionListener {
        fun getWorkItemContainerData(): WorkItemContainerData
    }

    fun addWorkItemView(wi: WorkItemData) {
        val inflater = layoutInflater
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.work_item_content_container) as LinearLayout
        val workItemDataBinding: WorkItemLayoutBinding = WorkItemLayoutBinding.inflate(inflater, null, false)
        workItemDataBinding.workItem = wi
        workItemDataBinding.workItemContainer = workItemContainerData
        workItemDataBinding.lifecycleOwner = activity
        workItemDataBinding.root.findViewById<ImageButton>(R.id.work_item_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.WorkItemEditorFragment.work_item_del_button.onClick", "deleting work item element")
                        container.removeView(workItemDataBinding.root)
                        workItemContainerData!!.removeWorkItem(wi)
                    } else {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_button.onClick", "cancelled deleting work item element")
                    }
                }
            }
        })

        val pos = container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work item card $pos to UI")
        container.addView(workItemDataBinding.root, pos)
    }

}
