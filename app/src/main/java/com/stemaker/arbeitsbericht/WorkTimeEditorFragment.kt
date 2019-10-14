package com.stemaker.arbeitsbericht

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.stemaker.arbeitsbericht.data.WorkTimeContainerData
import com.stemaker.arbeitsbericht.data.WorkTimeData
import com.stemaker.arbeitsbericht.databinding.FragmentWorkTimeEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkTimeLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

        setHeadline(getString(R.string.worktimes))

        dataBinding.lifecycleOwner = this
        dataBinding.workTimeContainerData = workTimeContainerData!!

        for(wt in workTimeContainerData!!.items) {
            addWorkTimeView(wt)
        }

        dataBinding.root.findViewById<ImageButton>(R.id.work_time_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wt = workTimeContainerData!!.addWorkTime()
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
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.work_time_content_container) as LinearLayout
        val workTimeDataBinding: WorkTimeLayoutBinding = WorkTimeLayoutBinding.inflate(inflater, null, false)
        workTimeDataBinding.workTime = wt
        workTimeDataBinding.lifecycleOwner = activity
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_date_change).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = DatePickerFragment(wt.date)
                newFragment.show(fragmentManager, "datePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_work_duration_change).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.duration)
                newFragment.show(fragmentManager, "timePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_drive_duration_change).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.driveTime)
                newFragment.show(fragmentManager, "timePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer = showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_button.onClick", "deleting work time element")
                        container.removeView(workTimeDataBinding.root)
                        workTimeContainerData!!.removeWorkTime(wt)
                    } else {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_button.onClick", "cancelled deleting work time element")
                    }
                }
            }
        })

        val pos = container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work time card $pos to UI")
        container.addView(workTimeDataBinding.root, pos)
    }

}
