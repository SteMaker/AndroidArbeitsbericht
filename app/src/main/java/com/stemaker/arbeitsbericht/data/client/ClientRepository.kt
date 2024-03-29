package com.stemaker.arbeitsbericht.data.client

import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.helpers.ListObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    var visible = false

    fun createClientDb(): ClientDb = ClientDb(id, name.value?:"", street.value?:"", zip.value?:"", city.value?:"",
            distance.value?:0, useDistance.value?:false, driveTime.value?:"00:00",
            useDriveTime.value?:false, notes.value?:"")

    fun equal(other: Client): Boolean = other.id == id
}

class ClientRepository(
    private val dao: ClientDao,
    private val prefs: AbPreferences)
{
    private val observers = mutableListOf<ListObserver<Client>>()
    val clients = mutableListOf<Client>()
    private val addedClients = mutableListOf<Client>()
    private val removedClients = mutableListOf<Client>()

    suspend fun initialize() {
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
        val c = Client(prefs.allocateClientId())
        c.visible = true
        clients.add(c)
        for (obs in observers)
            obs.elementAdded(c, clients.size - 1)
        addedClients.add(c)
    }

    fun removeClient(c: Client) {
        val idx = clients.indexOfFirst { it.equal(c) }
        if (idx == -1) return
        clients.remove(c)
        for (obs in observers)
            obs.elementRemoved(c, idx)
        removedClients.add(c)
    }

    private val mutex = Mutex()
    suspend fun store() {
        mutex.withLock {
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
}