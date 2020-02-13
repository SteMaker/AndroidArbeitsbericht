package com.stemaker.arbeitsbericht.hoursreport

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.StorageHandler
import com.stemaker.arbeitsbericht.data.hoursreport.HoursReportData
import com.stemaker.arbeitsbericht.data.workreport.WorkTimeData
import com.stemaker.arbeitsbericht.databinding.ActivityHoursEditorBinding
import com.stemaker.arbeitsbericht.databinding.EmployeeEntryLayoutBinding
import com.stemaker.arbeitsbericht.databinding.HoursWorkItemLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.storageHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "HoursEditorActivity"

class HoursEditorActivity : AppCompatActivity() {
    lateinit var binding: ActivityHoursEditorBinding
    val hoursReportData = storageHandler().activeHoursReport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_hours_editor)
        binding.lifecycleOwner = this
        binding.data = hoursReportData

        for(wi in hoursReportData.workItems) {
            addWorkItemView(binding.root, hoursReportData, wi)
        }

        setSupportActionBar(findViewById(R.id.hours_report_editor_activity_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.editor)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        saveReport()
    }

    fun saveReport() {
        StorageHandler.saveActiveHoursReportToFile(getApplicationContext())
    }

    fun onClickAddWorkItem(btn: View) {
        val wi = hoursReportData.addWorkItem()
        addWorkItemView(binding.root, hoursReportData, wi)
    }

    fun addWorkItemView(root: View, hd: HoursReportData, wi: MutableLiveData<String>) {
        val inflater = layoutInflater
        val workItemDataBinding: HoursWorkItemLayoutBinding = HoursWorkItemLayoutBinding.inflate(inflater, null, false)
        workItemDataBinding.workItem = wi
        workItemDataBinding.lifecycleOwner = this
        val container = root.findViewById<LinearLayout>(R.id.work_items_container)

        workItemDataBinding.root.findViewById<ImageButton>(R.id.work_item_del_employee).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        container.removeView(workItemDataBinding.root)
                        hd.removeWorkItem(wi)
                    } else {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_employee.onClick", "cancelled deleting work time employee")
                    }
                }
            }
        })

        val pos = container.getChildCount()
        container.addView(workItemDataBinding.root, pos)
    }
}
