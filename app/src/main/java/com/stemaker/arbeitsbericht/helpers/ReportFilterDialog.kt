package com.stemaker.arbeitsbericht.helpers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.databinding.ReportFilterLayoutBinding

class ReportFilterDialog(val filter: ReportFilter): DialogFragment() {
    lateinit var dataBinding: ReportFilterLayoutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.report_filter_layout, container, false)
        dataBinding.lifecycleOwner = this
        dataBinding.inWork.isChecked = filter.inWork
        dataBinding.onHold.isChecked = filter.onHold
        dataBinding.done.isChecked = filter.done
        dataBinding.archived.isChecked = filter.archived
        dataBinding.projectName.setText(filter.projectName)
        dataBinding.projectExtra.setText(filter.projectExtra)
        dataBinding.applyButton.setOnClickListener {
            filter.blockUpdate = true
            filter.inWork = dataBinding.inWork.isChecked
            filter.onHold = dataBinding.onHold.isChecked
            filter.done = dataBinding.done.isChecked
            filter.archived = dataBinding.archived.isChecked
            filter.projectName = dataBinding.projectName.text.toString()
            filter.projectExtra = dataBinding.projectExtra.text.toString()
            filter.blockUpdate = false
            dismiss()
        }
        dataBinding.cancelButton.setOnClickListener {
            dismiss()
        }
        dataBinding.projectClearButton.setOnClickListener {
            dataBinding.projectName.setText("")
        }
        dataBinding.extraClearButton.setOnClickListener {
            dataBinding.projectExtra.setText("")
        }

        return dataBinding.root
    }
}