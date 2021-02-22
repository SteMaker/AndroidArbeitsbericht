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
import androidx.recyclerview.widget.SortedList
import com.stemaker.arbeitsbericht.*
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding

private const val TAG = "ReportListAdapter"

class ReportListAdapter(val reportCardInterface: ReportCardInterface, val activity: AppCompatActivity) :
    RecyclerView.Adapter<ReportListAdapter.ReportViewHolder>() {

    private lateinit var recyclerView: RecyclerView

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
        Log.d("ReportListAdapter", "onBindViewHolder")
        // Bind data
        val report = sortedList.get(position)
        holder.bind(report)
        // Ensure there is enough space so that Floating Action Button doesn't hide parts of the LAST report
        if(position + 1 == itemCount) {
            Log.d(TAG, "adding margin to position $position, item = ${holder.itemView}")
            setBottomMargin(holder.itemView, (100 * Resources.getSystem().displayMetrics.density).toInt());
            bottomView = holder.itemView
        } else {
            Log.d(TAG, "removing margin at position $position")
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
                setOnMenuItemClickListener { item ->
                    when (item?.itemId) {
                        R.id.delete -> {
                            reportCardInterface.onClickDeleteReport(report) {
                                sortedList.removeItemAt(position)
                            }
                            true
                        }
                        R.id.status_in_work -> {
                            reportCardInterface.onSetReportState(report, ReportData.ReportState.IN_WORK)
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
                        R.id.status_archived -> {
                            reportCardInterface.onSetReportState(report, ReportData.ReportState.ARCHIVED)
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

    private fun setBottomMargin(view: View, bottomMargin: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin);
            view.requestLayout();
        }
    }

    private val sortedList: SortedList<ReportData> = SortedList<ReportData>(ReportData::class.java, object : SortedList.Callback<ReportData>() {
        override fun compare(o1: ReportData?, o2: ReportData?): Int {
            if(o1 == null || o2 == null) return 0
            val result = (o1.id.compareTo(o2.id))*-1
            //Log.d("ReportListAdapter", "compare ${o1} ${o2} -> $result")
            return result
        }
        override fun onInserted(position: Int, count: Int) {
            //Log.d("ReportListAdapter", "onInserted")
            notifyItemRangeInserted(position, count)
        }
        override fun onRemoved(position: Int, count: Int) {
            //Log.d("ReportListAdapter", "onRemoved")
            notifyItemRangeRemoved(position, count)
        }
        override fun onMoved(fromPosition: Int, toPosition: Int) {
            //Log.d("ReportListAdapter", "onMoved")
            notifyItemMoved(fromPosition, toPosition)
        }
        override fun onChanged(position: Int, count: Int) {
            //Log.d("ReportListAdapter", "onChanged")
            notifyItemRangeChanged(position, count)
        }
        override fun areContentsTheSame(oldItem: ReportData?, newItem: ReportData?): Boolean {
            //Log.d("ReportListAdapter", "areContentsTheSame ${oldItem} ${newItem}")
            return oldItem.hashCode() == newItem.hashCode()
        }
        override fun areItemsTheSame(item1: ReportData?, item2: ReportData?): Boolean {
            //Log.d("ReportListAdapter", "areItemsTheSame ${item1} ${item2}")
            if(item1 == null || item2 == null) return false
            return item1.id == item2.id
        }
    })

    fun replaceAll(reports: List<ReportData>) {
        sortedList.beginBatchedUpdates()
        sortedList.clear()
        sortedList.addAll(reports)
        sortedList.endBatchedUpdates()
    }

    fun add(reports: List<ReportData>) {
        if(bottomView != null) {
            Log.d(TAG, "clearing margin of item = ${bottomView}")
            setBottomMargin(bottomView!!, 0);
            bottomView = null
        }
        sortedList.addAll(reports)
    }

    fun add(report: ReportData) {
        if(bottomView != null) {
            Log.d(TAG, "clearing margin of item = ${bottomView}, when adding report with id ${report.id}")
            setBottomMargin(bottomView!!, 0);
            bottomView = null
        }
        sortedList.add(report)
    }

    fun remove(report: ReportData) {
        sortedList.remove(report)
    }

    override fun getItemCount(): Int {
        return sortedList.size()
    }
}
