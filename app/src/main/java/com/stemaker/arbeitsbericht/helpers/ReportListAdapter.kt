package com.stemaker.arbeitsbericht.helpers

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.stemaker.arbeitsbericht.*
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding

class ReportListAdapter(private var reportIds: List<String>, val reportCardInterface: ReportCardInterface, val activity: AppCompatActivity) :
    RecyclerView.Adapter<ReportListAdapter.ReportViewHolder>() {

    private val mapState2MenuItemId = mapOf(ReportData.ReportState.IN_WORK to R.id.status_in_work,
        ReportData.ReportState.ON_HOLD to R.id.status_on_hold,
        ReportData.ReportState.DONE to R.id.status_done)

    class ReportViewHolder(val binding: ReportCardLayoutBinding, private val lcOwner: LifecycleOwner) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: ReportData) {
            binding.reportData = report
            binding.lifecycleOwner = lcOwner
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context);
        val itemBinding = ReportCardLayoutBinding.inflate(layoutInflater, parent, false);
        return ReportViewHolder(itemBinding, activity as LifecycleOwner);
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        Log.d("ReportListAdapter", "onBindViewHolder")
        // Bind data
        val report = storageHandler().getReportById(reportIds[position], activity.applicationContext)
        holder.bind(report)

        // Ensure there is enough space so that Floating Action Button doesn't hide parts of the LAST report
        if(position + 1 == itemCount) {
            setBottomMargin(holder.itemView, (100 * Resources.getSystem().displayMetrics.density).toInt());
        } else {
            setBottomMargin(holder.itemView, 0);
        }

        // Bind to the event of clicking the report
        holder.binding.reportCardTop.setOnClickListener {
            reportCardInterface.onClickReport(report.id)
        }

        // Bind to the event of clicking the context menu
        holder.binding.reportCardMenuButton.setOnClickListener {
            PopupMenu(activity.applicationContext, it).apply {
                Log.d("MainActivity", "onClickContext, ${report.id})")
                setOnMenuItemClickListener(object: PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(item: MenuItem?): Boolean {
                        return when (item?.itemId) {
                            R.id.delete-> {
                                reportCardInterface.onClickDeleteReport(report) {
                                    reportIds = storageHandler().getListOfReports()
                                    notifyItemRemoved(position)
                                }
                                true
                            }
                            R.id.status_in_work -> {
                                reportCardInterface.onSetReportState(report, ReportData.ReportState.IN_WORK)
                                storageHandler().saveReportToFile(activity.applicationContext, report)
                                true
                            }
                            R.id.status_on_hold -> {
                                reportCardInterface.onSetReportState(report, ReportData.ReportState.ON_HOLD)
                                true
                            }
                            R.id.status_done -> {
                                reportCardInterface.onSetReportState(report, ReportData.ReportState.DONE)
                                true
                            }
                            else -> false
                        }
                    }
                })
                inflate(R.menu.report_actions_menu)
                menu.findItem(mapState2MenuItemId[report.state.value!!]!!).isVisible = false
                show()
            }

        }
    }

    override fun getItemCount(): Int {
        return reportIds.size
    }

    private fun setBottomMargin(view: View, bottomMargin: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin);
            view.requestLayout();
        }
    }
}
