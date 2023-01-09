package com.stemaker.arbeitsbericht.helpers

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.ReportCardInterface
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.data.report.ReportRepository
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding

private const val TAG = "ReportListAdapter"

class ReportListAdapter(private val reportCardInterface: ReportCardInterface,
                        private val reportRepository: ReportRepository,
                        val activity: AppCompatActivity) :
    RecyclerView.Adapter<ReportListAdapter.ReportViewHolder>() {

    private lateinit var recyclerView: RecyclerView

    private val mapState2MenuItemId = mapOf(ReportData.ReportState.IN_WORK to R.id.status_in_work,
        ReportData.ReportState.ON_HOLD to R.id.status_on_hold,
        ReportData.ReportState.DONE to R.id.status_done,
        ReportData.ReportState.ARCHIVED to R.id.status_archived)
    private var bottomView: View? = null

    init {
        reportRepository.live.observe(activity) {
            reportRepository.live.value?.let {
                when (it.type) {
                    ReportRepository.ReportListChangeEvent.Type.LIST_ADD -> notifyReportAdded(it.reportUid, it.pos)
                    ReportRepository.ReportListChangeEvent.Type.LIST_REMOVE -> notifyReportRemoved(it.reportUid, it.pos)
                    ReportRepository.ReportListChangeEvent.Type.LIST_CHANGE -> notifyReportListChanged()
                }
            }
        }
    }

    class ReportViewHolder(val binding: ReportCardLayoutBinding, private val lcOwner: LifecycleOwner) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: ReportData) {
            binding.reportData = report
            binding.lifecycleOwner = lcOwner
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ReportCardLayoutBinding.inflate(layoutInflater, parent, false)
        return ReportViewHolder(itemBinding, activity as LifecycleOwner)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        // We only register the on click listeners once the report was loaded from the database to make sure nothing can be done with it before we are ready
        val actionsOnReportLoaded = { report: ReportData ->
            // Bind to the event of clicking the report
            holder.binding.reportCardTop.setOnClickListener {
                reportCardInterface.onClickReport(report.cnt)
            }

            // Bind to the event of clicking the context menu
            holder.binding.reportCardMenuButton.setOnClickListener {
                PopupMenu(activity.applicationContext, it).apply {
                    setOnMenuItemClickListener { item ->
                        when (item?.itemId) {
                            R.id.delete -> {
                                reportCardInterface.onClickDeleteReport(report)
                                true
                            }
                            R.id.status_in_work -> {
                                reportCardInterface.onSetReportState(report, position, ReportData.ReportState.IN_WORK)
                                true
                            }
                            R.id.status_on_hold -> {
                                reportCardInterface.onSetReportState(report, position, ReportData.ReportState.ON_HOLD)
                                true
                            }
                            R.id.status_done -> {
                                reportCardInterface.onSetReportState(report, position, ReportData.ReportState.DONE)
                                true
                            }
                            R.id.status_archived -> {
                                reportCardInterface.onSetReportState(report, position, ReportData.ReportState.ARCHIVED)
                                true
                            }
                            R.id.duplicate-> {
                                reportCardInterface.onClickDuplicateReport(report)
                                true
                            }
                            else -> false
                        }
                    }
                    inflate(R.menu.report_actions_menu)
                    menu.findItem(mapState2MenuItemId[report.state.value!!]!!).isVisible = false
                    show()
                }
            }
        }

        // Bind data
        val report = reportRepository.getReportByIndex(position, actionsOnReportLoaded)

        holder.bind(report)
        // Ensure there is enough space so that Floating Action Button doesn't hide parts of the LAST report
        if (position + 1 == itemCount) {
            setBottomMargin(holder.itemView, (100 * Resources.getSystem().displayMetrics.density).toInt())
            bottomView = holder.itemView
        } else {
            setBottomMargin(holder.itemView, 0)
        }

    }

    private fun notifyReportAdded(uid: Int, pos: Int) {
        notifyItemInserted(pos)
        recyclerView.scrollToPosition(pos)
    }

    private fun notifyReportListChanged() {
        notifyDataSetChanged()
    }

    private fun notifyReportRemoved(uid: Int, pos: Int) {
        notifyItemRemoved(pos)
    }

    private fun setBottomMargin(view: View, bottomMargin: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin)
            view.requestLayout()
        }
    }

    fun jumpTop() {
        recyclerView.scrollToPosition(0)
    }

    override fun getItemCount(): Int {
        return reportRepository.amountOfReports
    }
}
