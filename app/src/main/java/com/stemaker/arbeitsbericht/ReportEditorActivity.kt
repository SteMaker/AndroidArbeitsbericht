package com.stemaker.arbeitsbericht

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_report_editor.*
import android.content.Intent
import android.content.Context
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import android.R.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ReportEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht.ReportEditorActivity.onCreate", "start")
        setContentView(R.layout.activity_report_editor)

        // If no lump sums are defined in the configuration yet, then we present a hint
        // and disable the add button
        if(storageHandler().configuration.lumpSums.size == 0) {
            findViewById<TextView>(R.id.lump_sum_undef_hint).visibility=View.VISIBLE
            findViewById<ImageButton>(R.id.lump_sum_add_button).visibility=View.GONE
        }

        if(savedInstanceState != null) {
            Log.d("Arbeitsbericht.ReportEditorActivity.onCreate", "restoring active report ${savedInstanceState.getInt("activeReport").toString()}")
            storageHandler().selectReportById(savedInstanceState.getInt("activeReport"))
        }

    }

    override fun onResume() {
        super.onResume()
        loadReport()
    }

    /*****************/
    /* General stuff */
    /*****************/
    fun saveAndBackToMain() {
        saveReport()
        val intent = Intent(this, MainActivity::class.java).apply {}
        Log.d("Arbeitsbericht", "Switching to main activity")
        startActivity(intent)
    }

    fun onClickBack(@Suppress("UNUSED_PARAMETER") backButton: View) {
        Log.d("Arbeitsbericht.ReportEditorActivity.onClickBack", "called")
        saveAndBackToMain()
    }

    override fun onBackPressed() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onBackPressed", "called")
        saveAndBackToMain()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("activeReport", storageHandler().activeReport.id)
    }

    override fun onPause() {
        super.onPause()
        saveReport()
    }

    fun onClickSummary(@Suppress("UNUSED_PARAMETER") btn: View) {
        val intent = Intent(this, SummaryActivity::class.java).apply {}
        startActivity(intent)
    }

    fun loadReport() {
        val report = storageHandler().getReport()

        findViewById<EditText>(R.id.client_name).setText(report.client_name)
        findViewById<EditText>(R.id.client_extra1).setText(report.client_extra1)
        findViewById<EditText>(R.id.bill_address_name).setText(if (report.bill_address_name == "") report.client_name else report.bill_address_name)
        findViewById<EditText>(R.id.bill_address_street).setText(report.bill_address_street)
        findViewById<EditText>(R.id.bill_address_zip).setText(report.bill_address_zip)
        findViewById<EditText>(R.id.bill_address_city).setText(report.bill_address_city)

        report.work_times.forEach {
            addWorkTimeView(it)
        }
        report.work_items.forEach {
            addWorkItemView(it)
        }
        report.lump_sums.forEach {
            addLumpSumView(it)
        }
        report.material.forEach {
            addMaterialView(it)
        }
    }

    fun saveReport() {
        val report = storageHandler().getReport()

        report.client_name = findViewById<EditText>(R.id.client_name).getText().toString()
        if (report.client_name == "") report.client_name = getString(R.string.unknown)
        report.client_extra1 = findViewById<EditText>(R.id.client_extra1).getText().toString()
        report.bill_address_name = findViewById<EditText>(R.id.bill_address_name).getText().toString()
        report.bill_address_street = findViewById<EditText>(R.id.bill_address_street).getText().toString()
        report.bill_address_zip = findViewById<EditText>(R.id.bill_address_zip).getText().toString()
        report.bill_address_city = findViewById<EditText>(R.id.bill_address_city).getText().toString()

        for (i in 0 until worktimes_content_container.getChildCount()) {
            val v: View = worktimes_content_container.getChildAt(i)
            if (v.getId() == R.id.work_time_card_top) {
                val wt: WorkTime = v.getTag(R.id.TAG_WORKTIME) as WorkTime
                wt.date = v.findViewById<TextView>(R.id.work_time_date).getText().toString()
                wt.employee = v.findViewById<TextView>(R.id.work_time_employee).getText().toString()
                wt.duration = v.findViewById<TextView>(R.id.work_time_work_duration).getText().toString()
                wt.driveTime = v.findViewById<TextView>(R.id.work_time_drive_duration).getText().toString()
                wt.distance = v.findViewById<TextView>(R.id.work_time_distance).getText().toString().toInt()
            }
        }

        for (i in 0 until workitems_content_container.getChildCount()) {
            val v: View = workitems_content_container.getChildAt(i)
            if (v.getId() == R.id.work_item_card_top) {
                val wi: WorkItem = v.getTag(R.id.TAG_WORKITEM) as WorkItem
                wi.item = v.findViewById<TextView>(R.id.work_item_item).getText().toString()
                storageHandler().addToWorkItemDictionary(wi.item)
            }
        }

        for (i in 0 until lump_sum_content_container.getChildCount()) {
            val v: View = lump_sum_content_container.getChildAt(i)
            if (v.getId() == R.id.lump_sum_edit_card_top) {
                val ls: LumpSum = v.getTag(R.id.TAG_LUMP_SUM) as LumpSum
                val selItem = v.findViewById<Spinner>(R.id.lump_sum_item).getSelectedItem()
                if(selItem == null) {
                    Log.d("Arbeitsbericht.ReportEditorActivity.saveReport", "Lump sum is null, replacing with empty string")
                    ls.item = ""
                } else {
                    ls.item = selItem.toString()
                }
                Log.d("Arbeitsbericht", "Saved ${ls.item}")
            }
        }

        for (i in 0 until material_content_container.getChildCount()) {
            val v: View = material_content_container.getChildAt(i)
            if (v.getId() == R.id.material_card_top) {
                val ma: Material = v.getTag(R.id.TAG_MATERIAL) as Material
                ma.item = v.findViewById<TextView>(R.id.material_item).getText().toString()
                storageHandler().addToMaterialDictionary(ma.item)
                ma.amount = v.findViewById<TextView>(R.id.material_amount).getText().toString().toInt()
            }
        }

        storageHandler().saveReportToFile(report, getApplicationContext())
        storageHandler().saveMaterialDictionaryToFile(getApplicationContext())
        storageHandler().saveWorkItemDictionaryToFile(getApplicationContext())
    }

    /*******************/
    /* Project section */
    /*******************/
    fun onClickExpandProjectButton(expandProjectButton: View) {
        if (project_content_container.getVisibility() == View.GONE) {
            expandProjectButton.rotation = 180.toFloat()
            project_content_container.setVisibility(View.VISIBLE)
        } else {
            expandProjectButton.rotation = 0.toFloat()
            project_content_container.setVisibility(View.GONE)
        }
    }

    /*********************/
    /* Bill data section */
    /*********************/
    fun onClickExpandBillDataButton(expandBillDataButton: View) {
        if (billdata_content_container.getVisibility() == View.GONE) {
            expandBillDataButton.rotation = 180.toFloat()
            billdata_content_container.setVisibility(View.VISIBLE)
        } else {
            expandBillDataButton.rotation = 0.toFloat()
            billdata_content_container.setVisibility(View.GONE)
        }
    }

    /**********************/
    /* Work times section */
    /**********************/
    fun onClickExpandWorktimesButton(expandWorktimesButton: View) {
        if (worktimes_content_container.getVisibility() == View.GONE) {
            expandWorktimesButton.rotation = 180.toFloat()
            worktimes_content_container.setVisibility(View.VISIBLE)
        } else {
            expandWorktimesButton.rotation = 0.toFloat()
            worktimes_content_container.setVisibility(View.GONE)
        }
    }

    fun onClickAddWorkTime(@Suppress("UNUSED_PARAMETER") btn: View) {
        val report = storageHandler().getReport()
        val wt = WorkTime()
        report.work_times.add(wt)
        addWorkTimeView(wt)

    }

    fun onClickDelWorkTime(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@ReportEditorActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelWorkTime", "deleting work time element")
                val cV = btn.getTag(R.id.TAG_CARDVIEW) as CardView
                storageHandler().getReport().work_times.remove(cV.getTag(R.id.TAG_WORKTIME))
                worktimes_content_container.removeView(cV)
            } else {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelWorkTime", "cancelled deleting work time element")
            }
        }
    }

    fun onClickWorkTimeDate(btn: View) {
        val newFragment = DatePickerFragment(btn.getTag(R.id.TAG_DATETEXTVIEW) as TextView)
        newFragment.show(supportFragmentManager, "datePicker")
    }

    fun onClickWorkTimeDuration(btn: View) {
        val newFragment = TimePickerFragment(btn.getTag(R.id.TAG_TIMETEXTVIEW) as TextView)
        newFragment.show(supportFragmentManager, "timePicker")
    }

    fun onClickWorkTimeDriveDuration(btn: View) {
        val newFragment = TimePickerFragment(btn.getTag(R.id.TAG_TIMETEXTVIEW) as TextView)
        newFragment.show(supportFragmentManager, "timePicker")
    }

    fun addWorkTimeView(wt: WorkTime) {
        // Prepare a work_time_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.work_time_layout, null) as CardView

        // Fill in the data
        val tvDate = cV.findViewById<TextView>(R.id.work_time_date)
        tvDate.setText(wt.date)
        cV.findViewById<TextView>(R.id.work_time_employee).setText(wt.employee)
        cV.findViewById<TextView>(R.id.work_time_work_duration).setText(wt.duration)
        cV.findViewById<TextView>(R.id.work_time_drive_duration).setText(wt.driveTime)
        cV.findViewById<EditText>(R.id.work_time_distance).setText(wt.distance.toString())

        // set a TAG to the delete button to identify the card view (cV here)
        cV.findViewById<View>(R.id.work_time_del_button).setTag(R.id.TAG_CARDVIEW, cV)
        // set TAGs to the work time dialog buttons to identify the respective TextView
        cV.findViewById<View>(R.id.work_time_date_change).setTag(R.id.TAG_DATETEXTVIEW, tvDate)
        cV.findViewById<View>(R.id.work_time_work_duration_change)
            .setTag(R.id.TAG_TIMETEXTVIEW, cV.findViewById<TextView>(R.id.work_time_work_duration))
        cV.findViewById<View>(R.id.work_time_drive_duration_change)
            .setTag(R.id.TAG_TIMETEXTVIEW, cV.findViewById<TextView>(R.id.work_time_drive_duration))
        // set a TAG to the card view to link with the work time data
        cV.setTag(R.id.TAG_WORKTIME, wt)

        // In the distance field select the whole text any time it gets clicked, so that
        // the user is not required to delete but directly overwrites
        val distET = cV.findViewById<EditText>(R.id.work_time_distance)
        distET.setSelectAllOnFocus(true);
        cV.findViewById<EditText>(R.id.work_time_distance).setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View) {
                val et = v as EditText
                if(et.isFocused()){
                    et.clearFocus();
                    et.requestFocus();
                }else{
                    et.requestFocus();
                    et.clearFocus();
                }
            }
        })

        val pos = worktimes_content_container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work time card $pos to UI")
        worktimes_content_container.addView(cV, pos)
    }

    /**********************/
    /* Work items section */
    /**********************/
    fun onClickExpandWorkitemsButton(expandWorkitemsButton: View) {
        if (workitems_content_container.getVisibility() == View.GONE) {
            expandWorkitemsButton.rotation = 180.toFloat()
            workitems_content_container.setVisibility(View.VISIBLE)
        } else {
            expandWorkitemsButton.rotation = 0.toFloat()
            workitems_content_container.setVisibility(View.GONE)
        }
    }

    fun onClickAddWorkItem(@Suppress("UNUSED_PARAMETER") btn: View) {
        val report = storageHandler().getReport()
        val wi = WorkItem()
        report.work_items.add(wi)
        addWorkItemView(wi)

    }

    fun onClickDelWorkItem(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@ReportEditorActivity)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelWorkItem", "deleting work item element")
                val cV = btn.getTag(R.id.TAG_CARDVIEW) as CardView
                storageHandler().getReport().work_items.remove(cV.getTag(R.id.TAG_WORKITEM))
                workitems_content_container.removeView(cV)
            } else {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelWorkItem", "cancelled deleting a work item element")
            }
        }
    }

    fun addWorkItemView(wi: WorkItem) {
        // Prepare a work_time_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.work_item_layout, null) as CardView

        // Fill in the data
        val textView = cV.findViewById(R.id.work_item_item) as AutoCompleteTextView
        // Get the string array
        val workItemStrings: List<String> = storageHandler().workItemDictionary.items.toList()
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String>(this, layout.simple_list_item_1, workItemStrings).also { adapter ->
            textView.setAdapter(adapter)
        }

        textView.setText(wi.item)

        // set a TAG to the delete button to identify the card view (cV here)
        cV.findViewById<View>(R.id.work_item_del_button).setTag(R.id.TAG_CARDVIEW, cV)
        // set a TAG to the card view to link with the work time data
        cV.setTag(R.id.TAG_WORKITEM, wi)

        val pos = workitems_content_container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work item card $pos to UI")
        workitems_content_container.addView(cV, pos)
    }

    /********************/
    /* Lump sum section */
    /********************/
    fun onClickExpandLumpSumButton(expandLumpSumButton: View) {
        if (lump_sum_content_container.getVisibility() == View.GONE) {
            expandLumpSumButton.rotation = 180.toFloat()
            lump_sum_content_container.setVisibility(View.VISIBLE)
        } else {
            expandLumpSumButton.rotation = 0.toFloat()
            lump_sum_content_container.setVisibility(View.GONE)
        }
    }
    fun onClickAddLumpSum(btn: View) {
        val report = storageHandler().getReport()
        val ls = LumpSum()
        report.lump_sums.add(ls)
        addLumpSumView(ls)
    }

    fun onClickDelLumpSumItem(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@ReportEditorActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelLumpSumItem", "deleting lump-sum element")
                val cV = btn.getTag(R.id.TAG_CARDVIEW) as CardView
                storageHandler().getReport().lump_sums.remove(cV.getTag(R.id.TAG_LUMP_SUM))
                lump_sum_content_container.removeView(cV)
            } else {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelLumpSumItem", "Cancelled deleting a lump-sum entry")
            }
        }
    }

    fun addLumpSumView(ls: LumpSum) {
        // Prepare a lump_sum_edit_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.lump_sum_edit_layout, null) as CardView

        val spinner = cV.findViewById(R.id.lump_sum_item) as Spinner
        // Get the string array
        var lumpSumStrings: List<String> = storageHandler().configuration.lumpSums.toList()
        // Add Unbekannt if the element stored in the report doesn't fit any of the pre-defined ones
        // i.e. it has been deleted
        if(ls.item != "" && !storageHandler().configuration.lumpSums.contains(ls.item)) {
            lumpSumStrings = lumpSumStrings.plus("Entfernt")
            ls.item = "Entfernt"
        }
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String>(this, layout.simple_list_item_1, lumpSumStrings).also { adapter ->
            spinner.setAdapter(adapter)
        }

        // Fill in the data
        val idx = lumpSumStrings.indexOf(ls.item)
        if(idx >= 0) {
            spinner.setSelection(idx)
        }

        // set a TAG to the delete button to identify the card view (cV here)
        cV.findViewById<View>(R.id.lump_sum_item_del_button).setTag(R.id.TAG_CARDVIEW, cV)
        // set a TAG to the card view to link with the lump-sum data
        cV.setTag(R.id.TAG_LUMP_SUM, ls)

        val pos = lump_sum_content_container.getChildCount()
        Log.d("Arbeitsbericht", "Adding lump sum card $pos to UI")
        lump_sum_content_container.addView(cV, pos)
    }

    /********************/
    /* Material section */
    /********************/
    fun onClickExpandMaterialButton(expandMaterialButton: View) {
        if (material_content_container.getVisibility() == View.GONE) {
            expandMaterialButton.rotation = 180.toFloat()
            material_content_container.setVisibility(View.VISIBLE)
        } else {
            expandMaterialButton.rotation = 0.toFloat()
            material_content_container.setVisibility(View.GONE)
        }
    }

    fun onClickAddMaterial(@Suppress("UNUSED_PARAMETER") btn: View) {
        val report = storageHandler().getReport()
        val ma = Material()
        report.material.add(ma)
        addMaterialView(ma)
    }

    fun onClickDelMaterial(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@ReportEditorActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onDialogPositiveClick", "deleting material element")
                val cV = btn.getTag(R.id.TAG_CARDVIEW) as CardView
                storageHandler().getReport().material.remove(cV.getTag(R.id.TAG_MATERIAL))
                material_content_container.removeView(cV)
            } else {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelMaterial", "Cancelled deleting a material entry")
            }
        }
    }

    fun addMaterialView(ma: Material) {
        // Prepare a material_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.material_layout, null) as CardView

        val textView = cV.findViewById(R.id.material_item) as AutoCompleteTextView
        // Get the string array
        val materialStrings: List<String> = storageHandler().materialDictionary.items.toList()
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String>(this, layout.simple_list_item_1, materialStrings).also { adapter ->
            textView.setAdapter(adapter)
        }

        // Fill in the data
        textView.setText(ma.item)
        cV.findViewById<EditText>(R.id.material_amount).setText(ma.amount.toString())

        // set a TAG to the delete button to identify the card view (cV here)
        cV.findViewById<View>(R.id.material_del_button).setTag(R.id.TAG_CARDVIEW, cV)
        // set a TAG to the card view to link with the work time data
        cV.setTag(R.id.TAG_MATERIAL, ma)

        val pos = material_content_container.getChildCount()
        Log.d("Arbeitsbericht", "Adding material card $pos to UI")
        material_content_container.addView(cV, pos)
    }

}
