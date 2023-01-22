package com.stemaker.arbeitsbericht.helpers

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.stemaker.arbeitsbericht.ClientViewModel
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.client.Client
import com.stemaker.arbeitsbericht.databinding.ClientCardLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "ClientListAdapter"

class ClientListAdapter(val activity: AppCompatActivity, private val clientViewModel: ClientViewModel):
    RecyclerView.Adapter<ClientListAdapter.ClientViewHolder>(), ListObserver<Client> {

    private lateinit var recyclerView: RecyclerView
    init {
        clientViewModel.addListObserver(this)
    }

    class ClientViewHolder(val binding: ClientCardLayoutBinding, private val lcOwner: LifecycleOwner, private val clientViewModel: ClientViewModel):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(c: Client?, onSelected: () -> Unit) {
            if(c == null) return
            binding.client = c
            binding.lifecycleOwner = lcOwner
            binding.editGroup.visibility = when(c.visible) {
                true -> View.VISIBLE
                false -> View.GONE
            }
            binding.clientCardTop.setOnClickListener {
                if(binding.editGroup.visibility == View.GONE) {
                    binding.expandContentPic.rotation = 180.toFloat()
                    binding.editGroup.visibility = View.VISIBLE
                    c.visible = true
                    c.touched = true
                    onSelected()
                } else {
                    binding.expandContentPic.rotation = 0.toFloat()
                    binding.editGroup.visibility = View.GONE
                    c.visible = false
                }
            }

            binding.delButton.setOnClickListener { btn ->
                GlobalScope.launch(Dispatchers.Main) {
                    val answer = showConfirmationDialog(btn.context.getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        clientViewModel.removeClient(c)
                    }
                }
            }
            binding.drivetimeContainer.setOnClickListener {
                val activity = lcOwner as AppCompatActivity
                val newFragment = TimePickerFragment()
                newFragment.timeString = c.driveTime
                newFragment.show(activity.supportFragmentManager, "timePicker")
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = ClientCardLayoutBinding.inflate(layoutInflater, parent, false)
        return ClientViewHolder(itemBinding, activity as LifecycleOwner, clientViewModel)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(clientViewModel.clients[position]) {
            recyclerView.scrollToPosition(position)
            Handler(Looper.getMainLooper()).postDelayed({ recyclerView.smoothScrollToPosition(position) }, 250) //sometime not working, need some delay
        }
    }

    override fun elementsAdded(elements: List<Client>, startPos: Int, endPos: Int) {
        notifyDataSetChanged()
    }

    override fun elementAdded(element: Client, pos: Int) {
        notifyItemInserted(pos)
        recyclerView.scrollToPosition(pos)
        Handler(Looper.getMainLooper()).postDelayed({ recyclerView.smoothScrollToPosition(pos) }, 250) //sometime not working, need some delay
    }

    override fun elementRemoved(element: Client, oldPos: Int) {
        notifyItemRemoved(oldPos)
    }

    override fun cleared() {
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return clientViewModel.clients.size
    }
}
