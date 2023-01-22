package com.stemaker.arbeitsbericht.editor_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.ClientSelectDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.client.ClientRepository
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentProjectBillEditorBinding
import com.stemaker.arbeitsbericht.view_models.BillViewModel
import com.stemaker.arbeitsbericht.view_models.BillViewModelFactory
import com.stemaker.arbeitsbericht.view_models.ProjectViewModel
import com.stemaker.arbeitsbericht.view_models.ProjectViewModelFactory

class ProjectBillEditorFragment(private val clientRepository: ClientRepository,
                                private val report: ReportData):
    ReportEditorSectionFragment()
{
    lateinit var dataBinding: FragmentProjectBillEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_project_bill_editor, null, false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        val viewModelBill = ViewModelProvider(this, BillViewModelFactory(viewLifecycleOwner, report.bill)).get(BillViewModel::class.java)
        dataBinding.viewModelBill = viewModelBill
        val viewModelProject = ViewModelProvider(this, ProjectViewModelFactory(viewLifecycleOwner, report.project)).get(ProjectViewModel::class.java)
        dataBinding.viewModelProject = viewModelProject
        dataBinding.clientSelectButton?.setOnClickListener {
            val clientSelectDialog = ClientSelectDialog(clientRepository)
            if (clientRepository.clients.isEmpty()) {
                val toast = Toast.makeText(root.context, "Es sind keine Kunden definiert", Toast.LENGTH_LONG)
                toast.show()
            } else {
                clientSelectDialog.setOnSelectListener { client ->
                    viewModelBill.name.value = client.name.value?:""
                    viewModelBill.street.value = client.street.value?:""
                    viewModelBill.zip.value = client.zip.value?:""
                    viewModelBill.city.value = client.city.value?:""
                    report.defaultValues.useDefaultDistance = client.useDistance.value ?: false
                    report.defaultValues.useDefaultDriveTime = client.useDriveTime.value ?: false
                    report.defaultValues.defaultDriveTime = client.driveTime.value ?: "00:00"
                    report.defaultValues.defaultDistance = client.distance.value ?: 0
                    report.project.clientId = client.id
                    if (client.useDriveTime.value == true || client.useDistance.value == true) {
                        val toast = Toast.makeText(root.context, R.string.presets_active_notification, Toast.LENGTH_LONG)
                        toast.show()
                    }
                    if (report.project.name.value== "") report.project.name.value= client.name.value!!

                }
                clientSelectDialog.show(childFragmentManager, "ClientSelectDialog")
            }
        }
        setHeadline("Projekt / Kunde / Rechnungsadresse")
        dataBinding.lifecycleOwner = viewLifecycleOwner
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