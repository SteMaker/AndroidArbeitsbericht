package com.stemaker.arbeitsbericht

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import com.stemaker.arbeitsbericht.databinding.FragmentWorkTimeEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkTimeLayoutBinding

class WorkTimeEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnWorkTimeEditorInteractionListener? = null
    var workTimeContainerData: WorkTimeContainerData? = null
    lateinit var dataBinding: FragmentWorkTimeEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","WorkTimeEditorFragment.onCreate called")
        workTimeContainerData = listener!!.getWorkTimeContainerData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","WorkTimeEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentWorkTimeEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline("Arbeitszeiten")

        dataBinding.lifecycleOwner = this
        dataBinding.workTimeContainerData = workTimeContainerData!!

        storageHandler().getReport()

        dataBinding.root.findViewById<ImageButton>(R.id.work_time_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wt = storageHandler().getReport().addWorkTime()
                addWorkTimeView(wt)
            }
        })

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context, this)
        if (context is OnWorkTimeEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnWorkTimeEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.work_time_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    interface OnWorkTimeEditorInteractionListener {
        fun getWorkTimeContainerData(): WorkTimeContainerData
    }

    fun addWorkTimeView(wt: WorkTimeData) {
        val inflater = layoutInflater
        Log.d("Arbeitsbericht.WorkTimeEditorFragment.addWorkTimeView", "inflater is ${inflater.toString()}")
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.work_time_content_container) as LinearLayout
        val workTimeDataBinding: WorkTimeLayoutBinding = WorkTimeLayoutBinding.inflate(inflater, null, false)
        workTimeDataBinding.workTime = wt

        val pos = container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work time card $pos to UI")
        container.addView(workTimeDataBinding.root, pos)
    }

}
