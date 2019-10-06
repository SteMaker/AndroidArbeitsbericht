package com.stemaker.arbeitsbericht

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.databinding.FragmentWorkTimeEditorBinding

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
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_work_time_editor, null, false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline("Arbeitszeiten")

        dataBinding.lifecycleOwner = this
        dataBinding.workTimeContainerData = workTimeContainerData!!

        dataBinding.root.findViewById<ImageButton>(R.id.work_time_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wt = storageHandler().getReport().addWorkTime()
                //addWorkTimeView(wt, true) // TODO
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

    fun onClickAddWorkTime(btn: View) {
        Log.d("Arbeitsbericht", "Clicked")
    }

    interface OnWorkTimeEditorInteractionListener {
        fun getWorkTimeContainerData(): WorkTimeContainerData
    }
}
