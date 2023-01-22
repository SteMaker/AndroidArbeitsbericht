package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.base.DataModificationEvent
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.EmployeeEntryLayoutBinding
import com.stemaker.arbeitsbericht.databinding.FragmentWorkTimeEditorBinding
import com.stemaker.arbeitsbericht.databinding.WorkTimeLayoutBinding
import com.stemaker.arbeitsbericht.helpers.DatePickerFragment
import com.stemaker.arbeitsbericht.helpers.TimePickerFragment
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.view_models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WorkTimeEditorFragment(private val report: ReportData):
    ReportEditorSectionFragment(),
    WorkTimeInteractionFragment
{
    lateinit var dataBinding: FragmentWorkTimeEditorBinding
    private val workTimeViews = mutableListOf<View>()
    lateinit var viewModelContainer: WorkTimeContainerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentWorkTimeEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.worktimes))

        dataBinding.lifecycleOwner =viewLifecycleOwner
        val workTimeContainerData = report.workTimeContainer
        viewModelContainer = ViewModelProvider(this, WorkTimeContainerViewModelFactory(viewLifecycleOwner, this, workTimeContainerData, report.defaultValues)).get(WorkTimeContainerViewModel::class.java)
        dataBinding.viewModelContainer = viewModelContainer

        for (viewModel in viewModelContainer) {
            addWorkTimeView(viewModel, viewModelContainer)
        }

        dataBinding.workTimeAddButton.setOnClickListener {
            val ret = viewModelContainer.addWorkTime(requireActivity() as Context)
            ret.first?.let {
                val toast = Toast.makeText(root.context, it, Toast.LENGTH_LONG)
                toast.show()
            }
            val v = addWorkTimeView(ret.second, viewModelContainer)
            listener?.scrollTo(v)
        }

        dataBinding.workTimeSortButton.setOnClickListener {
            viewModelContainer.sortByDate()
        }

        return root
    }

    override fun onReorder() {
        val c = dataBinding.workTimeContentContainer
        for (v in workTimeViews)
            c.removeView(v)
        workTimeViews.clear()
        for (viewModel in viewModelContainer) {
            addWorkTimeView(viewModel, viewModelContainer)
        }
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

    private fun addWorkTimeView(viewModel: WorkTimeViewModel, viewModelContainer: WorkTimeContainerViewModel): View {
        val inflater = layoutInflater
        val container = dataBinding.workTimeContentContainer
        val workTimeDataBinding: WorkTimeLayoutBinding = WorkTimeLayoutBinding.inflate(inflater, null, false)
        workTimeDataBinding.viewModel = viewModel
        workTimeDataBinding.lifecycleOwner = activity
        workTimeViews.add(workTimeDataBinding.root)

        for(employee in viewModel.employees) {
            addEmployeeView(workTimeDataBinding.root, viewModel, employee)
        }

        workTimeDataBinding.dateContainer.setOnClickListener {
            val newFragment = DatePickerFragment()
            newFragment.date = viewModel.date
            newFragment.show(parentFragmentManager, "datePicker")
        }
        workTimeDataBinding.startContainer.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.timeString = viewModel.startTime
            newFragment.show(parentFragmentManager, "startTimePicker")
        }
        workTimeDataBinding.endContainer.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.timeString = viewModel.endTime
            newFragment.show(parentFragmentManager, "endTimePicker")
        }
        workTimeDataBinding.pausetimeContainer.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.timeString = viewModel.pauseDuration
            newFragment.show(parentFragmentManager, "pauseTimePicker")
        }
        workTimeDataBinding.drivetimeContainer.setOnClickListener {
            val newFragment = TimePickerFragment()
            newFragment.timeString = viewModel.driveTime
            newFragment.show(parentFragmentManager, "driveTimePicker")
        }
        workTimeDataBinding.workTimeAddEmployee.setOnClickListener {
            val employee = viewModel.addEmployee()
            addEmployeeView(workTimeDataBinding.root, viewModel, employee)
        }
        workTimeDataBinding.workTimeCopyButton.setOnClickListener {
            val clone = viewModelContainer.cloneWorkTime(viewModel)
            addWorkTimeView(clone, viewModelContainer)
        }
        workTimeDataBinding.workTimeDelButton.setOnClickListener { btn ->
            GlobalScope.launch(Dispatchers.Main) {
                val answer =
                    showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                if (answer == AlertDialog.BUTTON_POSITIVE) {
                    container.removeView(workTimeDataBinding.root)
                    workTimeViews.remove(workTimeDataBinding.root)
                    viewModelContainer.removeWorkTime(viewModel)
                } else {
                }
            }
        }

        val pos = container.childCount
        container.addView(workTimeDataBinding.root, pos)
        return workTimeDataBinding.root
    }

    private fun addEmployeeView(root: View, viewModel: WorkTimeViewModel, employee: MutableLiveData<String>) {
        val inflater = layoutInflater
        val employeeDataBinding: EmployeeEntryLayoutBinding = EmployeeEntryLayoutBinding.inflate(inflater, null, false)
        employeeDataBinding.employee = employee
        employeeDataBinding.lifecycleOwner = activity
        val container = root.findViewById<LinearLayout>(R.id.work_time_container)

        employeeDataBinding.workTimeDelEmployee.setOnClickListener { btn ->
            GlobalScope.launch(Dispatchers.Main) {
                val answer =
                    showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                if (answer == AlertDialog.BUTTON_POSITIVE) {
                    container.removeView(employeeDataBinding.root)
                    viewModel.removeEmployee(employee)
                } else {
                }
            }
        }

        val pos = container.childCount
        container.addView(employeeDataBinding.root, pos)
    }
}
