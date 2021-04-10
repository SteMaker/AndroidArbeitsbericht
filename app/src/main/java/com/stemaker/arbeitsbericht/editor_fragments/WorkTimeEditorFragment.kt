package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.helpers.DatePickerFragment
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.helpers.TimePickerFragment
import com.stemaker.arbeitsbericht.data.WorkTimeContainerData
import com.stemaker.arbeitsbericht.data.WorkTimeData
import com.stemaker.arbeitsbericht.databinding.EmployeeEntryLayoutBinding
import com.stemaker.arbeitsbericht.databinding.FragmentWorkTimeEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkTimeLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WorkTimeEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnWorkTimeEditorInteractionListener? = null
    lateinit var dataBinding: FragmentWorkTimeEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","WorkTimeEditorFragment.onCreate called")
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
        GlobalScope.launch(Dispatchers.Main) {
            val workTimeContainerData = listener!!.getWorkTimeContainerData()
            dataBinding.workTimeContainerData = workTimeContainerData

            for (wt in workTimeContainerData.items) {
                addWorkTimeView(wt, workTimeContainerData)
            }

            dataBinding.root.findViewById<ImageButton>(R.id.work_time_add_button).setOnClickListener(object : View.OnClickListener {
                override fun onClick(btn: View) {
                    val wt = workTimeContainerData.addWorkTime()
                    addWorkTimeView(wt, workTimeContainerData)
                }
            })
        }

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

    override fun getVisibility(): Boolean {
        return dataBinding.root.findViewById<LinearLayout>(R.id.work_time_content_container).visibility != View.GONE
    }

    interface OnWorkTimeEditorInteractionListener {
        suspend fun getWorkTimeContainerData(): WorkTimeContainerData
    }

    fun addWorkTimeView(wt: WorkTimeData, workTimeContainerData: WorkTimeContainerData) {
        val inflater = layoutInflater
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.work_time_content_container) as LinearLayout
        val workTimeDataBinding: WorkTimeLayoutBinding = WorkTimeLayoutBinding.inflate(inflater, null, false)
        workTimeDataBinding.workTime = wt
        workTimeDataBinding.lifecycleOwner = activity

        for(empl in wt.employees) {
            addEmployeeView(workTimeDataBinding.root, wt, empl)
        }

        workTimeDataBinding.root.findViewById<ConstraintLayout>(R.id.date_container).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = DatePickerFragment(wt.date, btn.context)
                newFragment.show(fragmentManager!!, "datePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ConstraintLayout>(R.id.start_container).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.startTime)
                newFragment.show(fragmentManager!!, "timePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ConstraintLayout>(R.id.end_container).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.endTime)
                newFragment.show(fragmentManager!!, "timePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ConstraintLayout>(R.id.pausetime_container).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.pauseDuration)
                newFragment.show(fragmentManager!!, "timePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ConstraintLayout>(R.id.drivetime_container).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.driveTime)
                newFragment.show(fragmentManager!!, "timePicker")
            }
        })
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_add_employee).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val empl = wt.addEmployee()
                addEmployeeView(workTimeDataBinding.root, wt, empl)
            }
        })
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_copy_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wt2 = workTimeContainerData.addWorkTime(wt)
                addWorkTimeView(wt2, workTimeContainerData)
            }
        })
        workTimeDataBinding.root.findViewById<ImageButton>(R.id.work_time_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_button.onClick", "deleting work time element")
                        container.removeView(workTimeDataBinding.root)
                        workTimeContainerData.removeWorkTime(wt)
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

    fun addEmployeeView(root: View, wt: WorkTimeData, empl: MutableLiveData<String>) {
        val inflater = layoutInflater
        val employeeDataBinding: EmployeeEntryLayoutBinding = EmployeeEntryLayoutBinding.inflate(inflater, null, false)
        employeeDataBinding.employee = empl
        employeeDataBinding.lifecycleOwner = activity
        val container = root.findViewById<LinearLayout>(R.id.work_time_container)

        employeeDataBinding.root.findViewById<ImageButton>(R.id.work_time_del_employee).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_employee.onClick", "deleting work time employee")
                        container.removeView(employeeDataBinding.root)
                        wt.removeEmployee(empl)
                    } else {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_employee.onClick", "cancelled deleting work time employee")
                    }
                }
            }
        })

        val pos = container.getChildCount()
        container.addView(employeeDataBinding.root, pos)
    }

}
