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
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.LumpSumContainerData
import com.stemaker.arbeitsbericht.data.LumpSumData
import com.stemaker.arbeitsbericht.databinding.FragmentLumpSumEditorBinding
import com.stemaker.arbeitsbericht.databinding.LumpSumEditLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LumpSumEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnLumpSumEditorInteractionListener? = null
    var lumpSumContainerData: LumpSumContainerData? = null
    lateinit var dataBinding: FragmentLumpSumEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","LumpSumEditorFragment.onCreate called")
        lumpSumContainerData = listener!!.getLumpSumContainerData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","LumpSumEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentLumpSumEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.lump_sum))

        dataBinding.lifecycleOwner = this
        dataBinding.lumpSumContainerData = lumpSumContainerData!!

        for(ls in lumpSumContainerData!!.items) {
            addLumpSumView(ls)
        }

        dataBinding.root.findViewById<ImageButton>(R.id.lump_sum_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val ls = lumpSumContainerData!!.addLumpSum()
                addLumpSumView(ls)
            }
        })

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context, this)
        if (context is OnLumpSumEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnLumpSumEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.lump_sum_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    override fun getVisibility(): Boolean {
        return dataBinding.root.findViewById<LinearLayout>(R.id.lump_sum_content_container).visibility != View.GONE
    }

    interface OnLumpSumEditorInteractionListener {
        fun getLumpSumContainerData(): LumpSumContainerData
    }

    fun addLumpSumView(ls: LumpSumData) {
        val inflater = layoutInflater
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.lump_sum_content_container)
        val lumpSumDataBinding: LumpSumEditLayoutBinding = LumpSumEditLayoutBinding.inflate(inflater, null, false)
        lumpSumDataBinding.lumpSum = ls
        lumpSumDataBinding.lumpSumContainer = lumpSumContainerData
        lumpSumDataBinding.lifecycleOwner = activity
        lumpSumDataBinding.root.findViewById<ImageButton>(R.id.lump_sum_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.LumpSumEditorFragment.lump_sum_del_button.onClick", "deleting work item element")
                        container.removeView(lumpSumDataBinding.root)
                        lumpSumContainerData!!.removeLumpSum(ls)
                    } else {
                        Log.d("Arbeitsbericht.WorkTimeEditorFragment.work_time_del_button.onClick", "cancelled deleting work item element")
                    }
                }
            }
        })

        val pos = container.getChildCount()
        Log.d("Arbeitsbericht", "Adding work item card $pos to UI")
        container.addView(lumpSumDataBinding.root, pos)

        // TODO: Scroll to new element. Should use ListView instead of LinearLayout
    }

}
