package com.stemaker.arbeitsbericht.helpers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.stemaker.arbeitsbericht.ClientViewModel
import com.stemaker.arbeitsbericht.data.client.Client
import com.stemaker.arbeitsbericht.databinding.ClientSelectLayoutBinding

class ClientSelectListAdapter(val lcOwner: LifecycleOwner, private val clientViewModel: ClientViewModel):
    RecyclerView.Adapter<ClientSelectListAdapter.ClientSelectViewHolder>(), ListObserver<Client> {

    private lateinit var recyclerView: RecyclerView
    private var listener: (client: Client) ->Unit = {}
    init {
        clientViewModel.addListObserver(this)
    }

    class ClientSelectViewHolder(val binding: ClientSelectLayoutBinding, private val lcOwner: LifecycleOwner):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(c: Client?, onSelected: () -> Unit) {
            if(c == null) return
            binding.client = c
            binding.lifecycleOwner = lcOwner
            binding.clientSelectCardTop.setOnClickListener {
                    onSelected()
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientSelectViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context);
        val itemBinding = ClientSelectLayoutBinding.inflate(layoutInflater, parent, false);
        return ClientSelectViewHolder(itemBinding, lcOwner)
    }

    override fun onBindViewHolder(holder: ClientSelectViewHolder, position: Int) {
        holder.bind(clientViewModel.clients[position]) {
            listener(clientViewModel.clients[position])
        }
    }

    override fun elementsAdded(elements: List<Client>, startPos: Int, endPos: Int) {
        notifyDataSetChanged()
    }

    override fun elementAdded(element: Client, pos: Int) {
        notifyItemInserted(pos)
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

    fun setOnSelectListener(listener: (client: Client) -> Unit) {
        this.listener = listener
    }
}