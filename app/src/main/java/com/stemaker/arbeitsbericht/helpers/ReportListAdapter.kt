package com.stemaker.arbeitsbericht.helpers

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.stemaker.arbeitsbericht.*
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding

private const val TAG = "ReportListAdapter"

class ReportListAdapter(val reportCardInterface: ReportCardInterface, val activity: AppCompatActivity) :
    RecyclerView.Adapter<ReportListAdapter.ReportViewHolder>(),
    ReportListObserver {

    private lateinit var recyclerView: RecyclerView
    var reportCnts = mutableListOf<Int>()

    private val mapState2MenuItemId = mapOf(ReportData.ReportState.IN_WORK to R.id.status_in_work,
        ReportData.ReportState.ON_HOLD to R.id.status_on_hold,
        ReportData.ReportState.DONE to R.id.status_done,
        ReportData.ReportState.ARCHIVED to R.id.status_archived)
    private var bottomView: View? = null

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
        val layoutInflater = LayoutInflater.from(parent.context);
        val itemBinding = ReportCardLayoutBinding.inflate(layoutInflater, parent, false);
        return ReportViewHolder(itemBinding, activity as LifecycleOwner);
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        // Bind data
        val reportCnt = reportCnts[position]

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
                            else -> false
                        }
                    }
                    inflate(R.menu.report_actions_menu)
                    menu.findItem(mapState2MenuItemId[report.state.value!!]!!).isVisible = false
                    show()
                }
            }
        }

        val report = storageHandler().getReportByCnt(reportCnt, actionsOnReportLoaded)

        holder.bind(report)
        // Ensure there is enough space so that Floating Action Button doesn't hide parts of the LAST report
        if (position + 1 == itemCount) {
            setBottomMargin(holder.itemView, (100 * Resources.getSystem().displayMetrics.density).toInt());
            bottomView = holder.itemView
        } else {
            setBottomMargin(holder.itemView, 0);
        }

    }

    // TODO: Reuse the object from storageHandler instead of establishing another one
    override fun notifyReportAdded(cnt: Int) {
        reportCnts.add(0, cnt)
        notifyItemInserted(0)
        recyclerView.scrollToPosition(0)
    }

    override fun notifyReportListChanged(cnts: List<Int>) {
        reportCnts.clear()
        reportCnts.addAll(cnts)
        notifyDataSetChanged()
    }

    override fun notifyReportRemoved(cnt: Int) {
        val pos = reportCnts.indexOfFirst { it == cnt }
        reportCnts.removeAt(pos)
        notifyItemRemoved(pos)
    }

    private fun setBottomMargin(view: View, bottomMargin: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin);
            view.requestLayout();
        }
    }

    fun registerReportListObserver() {
        storageHandler().addReportListObserver(this)
    }

    fun jumpTop() {
        recyclerView.scrollToPosition(0)
    }

    override fun getItemCount(): Int {
        return reportCnts.size
    }
}
