package com.stemaker.arbeitsbericht

import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.ClientDb
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.helpers.ListObserver
import kotlinx.coroutines.*

class Client(val id: Int, __name: String = "", __street: String = "", __zip: String = "", __city: String = "", __distance: Int = 0, __useDistance: Boolean = false,
             __driveTime: String = "00:00", __useDriveTime: Boolean = false, __notes: String = "") {
    var name = MutableLiveData<String>(__name)
    var street = MutableLiveData<String>(__street)
    var zip = MutableLiveData<String>(__zip)
    var city = MutableLiveData<String>(__city)
    var distance = MutableLiveData<Int>(__distance)
    var useDistance = MutableLiveData<Boolean>(__useDistance)
    var driveTime = MutableLiveData<String>(__driveTime)
    var useDriveTime = MutableLiveData<Boolean>(__useDriveTime)
    var notes = MutableLiveData<String>(__notes)
    var touched = false

    fun createClientDb(): ClientDb = ClientDb(id, name.value?:"", street.value?:"", zip.value?:"", city.value?:"",
            distance.value?:0, useDistance.value?:false, driveTime.value?:"00:00",
            useDriveTime.value?:false, notes.value?:"")

    fun equal(other: Client): Boolean = other.id == id
}

object ClientRepository {
    private val observers = mutableListOf<ListObserver<Client>>()
    val clients = mutableListOf<Client>()
    private val addedClients = mutableListOf<Client>()
    private val removedClients = mutableListOf<Client>()
    private val dao = storageHandler().db.clientDao()
    val initJob: Job = GlobalScope.launch(Dispatchers.Main) {
        val clientsDb = withContext(Dispatchers.IO) { dao.getClients() }
        for (cDb in clientsDb) {
            val c = Client(cDb.id, cDb.name, cDb.street, cDb.zip, cDb.city, cDb.distance, cDb.useDistance, cDb.driveTime, cDb.useDriveTime, cDb.notes)
            clients.add(c)
        }
        for (obs in observers)
            obs.elementsAdded(clients, 0, clients.size - 1)
    }

    fun addListObserver(obs: ListObserver<Client>) {
        observers.add(obs)
        obs.elementsAdded(clients, 0, clients.size - 1)
    }

    fun removeListObserver(obs: ListObserver<Client>) {
        observers.remove(obs)
    }

    fun addClient() {
        if (initJob.isCompleted) {
            val c = Client(configuration().currentClientId)
            configuration().currentClientId += 1
            configuration().save()
            clients.add(c)
            for (obs in observers)
                obs.elementAdded(c, clients.size - 1)
            addedClients.add(c)
        }
    }

    fun removeClient(c: Client) {
        if (initJob.isCompleted) {
            val idx = clients.indexOfFirst { it.equal(c) }
            if (idx == -1) return
            clients.remove(c)
            for (obs in observers)
                obs.elementRemoved(c, idx)
            removedClients.add(c)
        }
    }

    suspend fun store() {
        initJob.join()
        for (added in addedClients) {
            dao.insert(added.createClientDb())
        }
        addedClients.clear()
        for (modified in clients) {
            if (modified.touched)
                dao.update(modified.createClientDb())
        }
        for (removed in removedClients) {
            dao.deleteById(removed.id)
        }
        removedClients.clear()
    }
}