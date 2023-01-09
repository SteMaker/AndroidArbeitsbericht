package com.stemaker.arbeitsbericht.editor_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentLumpSumEditorBinding
import com.stemaker.arbeitsbericht.databinding.LumpSumEditLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.view_models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LumpSumEditorFragment(private val report: ReportData):
    ReportEditorSectionFragment()
{
    lateinit var dataBinding: FragmentLumpSumEditorBinding

    /* TODO: In case a lump sum has been deleted that is still used here we should
     * Option A) Add this one still as an option (not good?)
     * Option B) Make a TextView out of this drop down list and write it there (+hint that it is mssing)
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentLumpSumEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.lump_sum))

        dataBinding.lifecycleOwner = viewLifecycleOwner
        val lumpSumContainerData = report.lumpSumContainer
        val viewModelContainer = ViewModelProvider(this, LumpSumContainerViewModelFactory(viewLifecycleOwner, lumpSumContainerData)).get(LumpSumContainerViewModel::class.java)
        dataBinding.viewModel = viewModelContainer

        for (viewModel in viewModelContainer) {
            addLumpSumView(viewModel, viewModelContainer)
        }

        dataBinding.lumpSumAddButton.setOnClickListener {
            val viewModel = viewModelContainer.addLumpSum()
            addLumpSumView(viewModel, viewModelContainer)
        }

        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.lumpSumContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.lumpSumContentContainer.visibility != View.GONE
    }

    private fun addLumpSumView(viewModel: LumpSumViewModel, viewModelContainer: LumpSumContainerViewModel) {
        val inflater = layoutInflater
        val container = dataBinding.lumpSumContentContainer
        val lumpSumDataBinding: LumpSumEditLayoutBinding = LumpSumEditLayoutBinding.inflate(inflater, null, false)
        lumpSumDataBinding.viewModel = viewModel
        lumpSumDataBinding.viewModelContainer = viewModelContainer
        lumpSumDataBinding.lifecycleOwner = activity
        lumpSumDataBinding.lumpSumDelButton.setOnClickListener { btn ->
            GlobalScope.launch(Dispatchers.Main) {
                val answer =
                    showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                if (answer == AlertDialog.BUTTON_POSITIVE) {
                    container.removeView(lumpSumDataBinding.root)
                    viewModelContainer.removeLumpSum(viewModel)
                } else {
                }
            }
        }

        val pos = container.childCount
        container.addView(lumpSumDataBinding.root, pos)

        // TODO: Scroll to new element. Should use ListView instead of LinearLayout
    }

}
