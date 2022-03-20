package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.ClientRepository
import com.stemaker.arbeitsbericht.ClientSelectDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.BillData
import com.stemaker.arbeitsbericht.data.ProjectData
import com.stemaker.arbeitsbericht.databinding.FragmentProjectBillEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ProjectBillEditorFragment  : ReportEditorSectionFragment() {
    lateinit var dataBinding: FragmentProjectBillEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_project_bill_editor, null, false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        GlobalScope.launch(Dispatchers.Main) {
            listener?.let { listener ->
                // There can be a race with onDetach so that the listener is already null
                val report = listener.getReportData()
                report?.let { report ->
                    dataBinding.projectData = report.project
                    dataBinding.billData = report.bill
                    ClientRepository.initJob.join()
                    dataBinding.clientSelectButton?.setOnClickListener {
                        val clientSelectDialog = ClientSelectDialog()
                        if (ClientRepository.clients.isEmpty()) {
                            val toast = Toast.makeText(root.context, "Es sind keine Kunden definiert", Toast.LENGTH_LONG)
                            toast.show()
                        } else {
                            clientSelectDialog.setOnSelectListener { client ->
                                dataBinding.billData?.name?.value = client.name.value
                                dataBinding.billData?.street?.value = client.street.value
                                dataBinding.billData?.zip?.value = client.zip.value
                                dataBinding.billData?.city?.value = client.city.value
                                report.defaultValues.useDefaultDistance = client.useDistance.value ?: false
                                report.defaultValues.useDefaultDriveTime = client.useDriveTime.value ?: false
                                report.defaultValues.defaultDriveTime = client.driveTime.value ?: "00:00"
                                report.defaultValues.defaultDistance = client.distance.value ?: 0
                                report.project.clientId = client.id
                                if (client.useDriveTime.value == true || client.useDistance.value == true) {
                                    val toast = Toast.makeText(root.context, R.string.presets_active_notification, Toast.LENGTH_LONG)
                                    toast.show()
                                }
                                if (report.project.name.value == "") report.project.name.value = client.name.value

                            }
                            clientSelectDialog.show(childFragmentManager, "ClientSelectDialog")
                        }
                    }
                }
            }
        }
        setHeadline("Projekt / Kunde / Rechnungsaddresse")

        dataBinding.lifecycleOwner = viewLifecycleOwner

        GlobalScope.launch(Dispatchers.Main) {
        }
        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.projectBillContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.projectBillContentContainer.visibility != View.GONE
    }
}