package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.WorkTimeContainerData
import com.stemaker.arbeitsbericht.data.WorkTimeData
import com.stemaker.arbeitsbericht.databinding.EmployeeEntryLayoutBinding
import com.stemaker.arbeitsbericht.databinding.FragmentWorkTimeEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkTimeLayoutBinding
import com.stemaker.arbeitsbericht.helpers.DatePickerFragment
import com.stemaker.arbeitsbericht.helpers.TimePickerFragment
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WorkTimeEditorFragment : ReportEditorSectionFragment() {
    lateinit var dataBinding: FragmentWorkTimeEditorBinding
    val workTimeViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentWorkTimeEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.worktimes))

        dataBinding.lifecycleOwner =viewLifecycleOwner
        GlobalScope.launch(Dispatchers.Main) {
            listener?.let {
                val report = it.getReportData()
                val workTimeContainerData = report.workTimeContainer
                dataBinding.workTimeContainerData = workTimeContainerData

                for (wt in workTimeContainerData.items) {
                    addWorkTimeView(wt, workTimeContainerData)
                }

                dataBinding.workTimeAddButton.setOnClickListener {
                    val wt = workTimeContainerData.addWorkTime(report.defaultValues)
                    val text = when {
                        report.defaultValues.useDefaultDriveTime && report.defaultValues.useDefaultDistance ->
                            "Vorgabe für Fahrzeit (${report.defaultValues.defaultDriveTime}) und Entfernung (${report.defaultValues.defaultDistance}km) von Kundendaten übernommen"
                        report.defaultValues.useDefaultDriveTime ->
                            "Vorgabe für Fahrzeit (${report.defaultValues.defaultDriveTime}) von Kundendaten übernommen"
                        report.defaultValues.useDefaultDistance ->
                            "Vorgabe für Entfernung (${report.defaultValues.defaultDistance}km) von Kundendaten übernommen"
                        else -> ""
                    }
                    if (text != "") {
                        val toast = Toast.makeText(root.context, text, Toast.LENGTH_LONG)
                        toast.show()
                    }
                    addWorkTimeView(wt, workTimeContainerData)
                }

                dataBinding.workTimeSortButton.setOnClickListener {
                    val comparator = Comparator { a: WorkTimeData, b: WorkTimeData ->
                        a.date.value?.let { ita -> b.date.value?.let { itb ->
                            ita.time.compareTo(itb.time)
                        } }?:0
                    }
                    workTimeContainerData.items.sortWith(comparator)
                    val c = dataBinding.workTimeContentContainer
                    for(v in workTimeViews)
                        c.removeView(v)
                    workTimeViews.clear()
                    for (wt in workTimeContainerData.items) {
                        addWorkTimeView(wt, workTimeContainerData)
                    }
                }
            }
        }

        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.workTimeContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.workTimeContentContainer.visibility != View.GONE
    }

    fun addWorkTimeView(wt: WorkTimeData, workTimeContainerData: WorkTimeContainerData) {
        val inflater = layoutInflater
        val container = dataBinding.workTimeContentContainer
        val workTimeDataBinding: WorkTimeLayoutBinding = WorkTimeLayoutBinding.inflate(inflater, null, false)
        workTimeDataBinding.workTime = wt
        workTimeDataBinding.lifecycleOwner = activity
        workTimeViews.add(workTimeDataBinding.root)

        for(empl in wt.employees) {
            addEmployeeView(workTimeDataBinding.root, wt, empl)
        }

        workTimeDataBinding.dateContainer.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = DatePickerFragment(wt.date, btn.context)
                newFragment.show(parentFragmentManager, "datePicker")
            }
        })
        workTimeDataBinding.startContainer.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.startTime)
                newFragment.show(parentFragmentManager, "timePicker")
            }
        })
        workTimeDataBinding.endContainer.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.endTime)
                newFragment.show(parentFragmentManager, "timePicker")
            }
        })
        workTimeDataBinding.pausetimeContainer.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.pauseDuration)
                newFragment.show(parentFragmentManager, "timePicker")
            }
        })
        workTimeDataBinding.drivetimeContainer.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val newFragment = TimePickerFragment(wt.driveTime)
                newFragment.show(parentFragmentManager, "timePicker")
            }
        })
        workTimeDataBinding.workTimeAddEmployee.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val empl = wt.addEmployee()
                addEmployeeView(workTimeDataBinding.root, wt, empl)
            }
        })
        workTimeDataBinding.workTimeCopyButton.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wt2 = workTimeContainerData.addWorkTime(wt)
                addWorkTimeView(wt2, workTimeContainerData)
            }
        })
        workTimeDataBinding.workTimeDelButton.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        container.removeView(workTimeDataBinding.root)
                        workTimeViews.remove(workTimeDataBinding)
                        workTimeContainerData.removeWorkTime(wt)
                    } else {
                    }
                }
            }
        })

        val pos = container.getChildCount()
        container.addView(workTimeDataBinding.root, pos)
    }

    fun addEmployeeView(root: View, wt: WorkTimeData, empl: MutableLiveData<String>) {
        val inflater = layoutInflater
        val employeeDataBinding: EmployeeEntryLayoutBinding = EmployeeEntryLayoutBinding.inflate(inflater, null, false)
        employeeDataBinding.employee = empl
        employeeDataBinding.lifecycleOwner = activity
        val container = root.findViewById<LinearLayout>(R.id.work_time_container)

        employeeDataBinding.workTimeDelEmployee.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        container.removeView(employeeDataBinding.root)
                        wt.removeEmployee(empl)
                    } else {
                    }
                }
            }
        })

        val pos = container.getChildCount()
        container.addView(employeeDataBinding.root, pos)
    }

}
