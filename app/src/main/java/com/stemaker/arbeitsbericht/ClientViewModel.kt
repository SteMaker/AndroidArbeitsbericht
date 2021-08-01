package com.stemaker.arbeitsbericht

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.helpers.ListObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ClientViewModel(private val repository: ClientRepository): ViewModel() {
    val clients = repository.clients
    fun addClient() = repository.addClient()

    fun removeClient(c: Client) = repository.removeClient(c)

    fun store() {
        GlobalScope.launch(Dispatchers.Main) {
            repository.store()
        }
    }

    fun addListObserver(obs: ListObserver<Client>) = repository.addListObserver(obs)

    fun removeListObserver(obs: ListObserver<Client>) = repository.removeListObserver(obs)
}

class ClientViewModelFactory(private val repository: ClientRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
