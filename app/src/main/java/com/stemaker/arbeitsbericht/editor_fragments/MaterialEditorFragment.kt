package com.stemaker.arbeitsbericht.editor_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.preferences.ConfigElement
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.FragmentMaterialEditorBinding
import com.stemaker.arbeitsbericht.databinding.MaterialLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.view_models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MaterialEditorFragment(
    private val report: ReportData,
    private val definedMaterials: ConfigElement<Set<String>>
)
    : ReportEditorSectionFragment()
{
    lateinit var dataBinding: FragmentMaterialEditorBinding
    private val elementDataBindings = mutableListOf<MaterialLayoutBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentMaterialEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.material))

        dataBinding.lifecycleOwner = viewLifecycleOwner
        val materialContainerData = report.materialContainer
        val viewModelContainer = ViewModelProvider(
            this,
            MaterialContainerViewModelFactory(viewLifecycleOwner, materialContainerData, definedMaterials))
            .get(MaterialContainerViewModel::class.java)
        dataBinding.viewModel = viewModelContainer

        for (viewModel in viewModelContainer) {
            addMaterialView(viewModel, viewModelContainer)
        }

        dataBinding.materialAddButton.setOnClickListener {
            val viewModel = viewModelContainer.addMaterial()
            val v = addMaterialView(viewModel, viewModelContainer)
            listener?.scrollTo(v)
        }
        return root
    }

    override fun onPause() {
        super.onPause()
        val newDefinedMaterials: MutableSet<String> = definedMaterials.value.toMutableSet()
        for(element in elementDataBindings) {
            val text = element.materialItem.text.toString()
            if(text != "") {
                newDefinedMaterials.add(text)
            }
        }
        definedMaterials.value = newDefinedMaterials
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.materialContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.materialContentContainer.visibility != View.GONE
    }

    private fun addMaterialView(viewModel: MaterialViewModel, viewModelContainer: MaterialContainerViewModel): View {
        val inflater = layoutInflater
        val container = dataBinding.materialContentContainer
        val materialDataBinding: MaterialLayoutBinding = MaterialLayoutBinding.inflate(inflater, null, false)
        materialDataBinding.viewModel = viewModel
        materialDataBinding.viewModelContainer = viewModelContainer
        materialDataBinding.lifecycleOwner = activity
        materialDataBinding.materialDelButton.setOnClickListener { btn ->
            GlobalScope.launch(Dispatchers.Main) {
                val answer =
                    showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                if (answer == AlertDialog.BUTTON_POSITIVE) {
                    container.removeView(materialDataBinding.root)
                    viewModelContainer.removeMaterial(viewModel)
                    elementDataBindings.remove(materialDataBinding)
                } else {
                }
            }
        }
        materialDataBinding.materialUnit.setOnFocusChangeListener { v, hasFocus -> run {
            val tv = v as AutoCompleteTextView
            if(hasFocus && tv.text.toString() == "") tv.showDropDown()
        } }
        materialDataBinding.materialItem.onFocusChangeListener = (View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus && v is EditText) {
                viewModelContainer.addToDictionary(v.text.toString())
            }
        })
        elementDataBindings.add(materialDataBinding)

        val pos = container.childCount
        container.addView(materialDataBinding.root, pos)
        return materialDataBinding.root
    }
}
