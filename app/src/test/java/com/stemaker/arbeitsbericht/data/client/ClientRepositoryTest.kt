package com.stemaker.arbeitsbericht.data.client

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.helpers.ListObserver
import com.stemaker.arbeitsbericht.support.FakePreferences
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/* The client list is edited in place and only written to the database when the activity
 * saves, so the repository has to keep track of what was added, changed and removed in
 * between. Getting that bookkeeping wrong either loses edits or resurrects deleted clients.
 */
class ClientRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeClientDao : ClientDao {
        val rows = linkedMapOf<Int, ClientDb>()
        var insertCount = 0
        var updateCount = 0
        var deleteCount = 0

        // "SELECT * FROM ClientDb ORDER BY name ASC"
        override suspend fun getClients(): List<ClientDb> = rows.values.sortedBy { it.name }

        override suspend fun insert(clientDb: ClientDb) {
            insertCount++
            rows[clientDb.id] = clientDb
        }

        override suspend fun update(clientDb: ClientDb) {
            updateCount++
            rows[clientDb.id] = clientDb
        }

        override fun deleteTable() = rows.clear()

        override suspend fun deleteById(id: Int) {
            deleteCount++
            rows.remove(id)
        }
    }

    private class RecordingObserver : ListObserver<Client> {
        val added = mutableListOf<Pair<Client, Int>>()
        val removed = mutableListOf<Pair<Client, Int>>()
        val bulkAdded = mutableListOf<Triple<Int, Int, Int>>() // size, start, end
        var clearedCount = 0

        override fun elementAdded(element: Client, pos: Int) {
            added.add(element to pos)
        }

        override fun elementsAdded(elements: List<Client>, startPos: Int, endPos: Int) {
            bulkAdded.add(Triple(elements.size, startPos, endPos))
        }

        override fun elementRemoved(element: Client, oldPos: Int) {
            removed.add(element to oldPos)
        }

        override fun cleared() {
            clearedCount++
        }
    }

    private lateinit var prefs: FakePreferences
    private lateinit var dao: FakeClientDao

    @Before
    fun setUp() {
        prefs = FakePreferences()
        dao = FakeClientDao()
    }

    private fun repository() = ClientRepository(dao, prefs.mock)

    private fun row(id: Int, name: String) = ClientDb(id, name, "Str $id", "1234$id", "City $id",
        distance = id * 10, useDistance = true, driveTime = "00:1$id", useDriveTime = true,
        notes = "Notes $id")

    // ---------------------------------------------------------------- loading

    @Test
    fun `initialize loads the stored clients`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        dao.rows[2] = row(2, "Albrecht")
        val repo = repository()

        repo.initialize()

        assertThat(repo.clients).hasSize(2)
        // The query orders by name
        assertThat(repo.clients.map { it.name.value }).containsExactly("Albrecht", "Bauer").inOrder()
    }

    @Test
    fun `initialize maps every stored field onto the client`(): Unit = runBlocking {
        dao.rows[7] = row(7, "Bauer")
        val repo = repository()

        repo.initialize()

        val c = repo.clients.single()
        assertThat(c.id).isEqualTo(7)
        assertThat(c.name.value).isEqualTo("Bauer")
        assertThat(c.street.value).isEqualTo("Str 7")
        assertThat(c.zip.value).isEqualTo("12347")
        assertThat(c.city.value).isEqualTo("City 7")
        assertThat(c.distance.value).isEqualTo(70)
        assertThat(c.useDistance.value).isTrue()
        assertThat(c.driveTime.value).isEqualTo("00:17")
        assertThat(c.useDriveTime.value).isTrue()
        assertThat(c.notes.value).isEqualTo("Notes 7")
    }

    @Test
    fun `initialize on an empty database yields no clients`(): Unit = runBlocking {
        val repo = repository()

        repo.initialize()

        assertThat(repo.clients).isEmpty()
    }

    // ---------------------------------------------------------------- observers

    @Test
    fun `a new observer is told about the clients that already exist`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        dao.rows[2] = row(2, "Albrecht")
        val repo = repository()
        repo.initialize()
        val observer = RecordingObserver()

        repo.addListObserver(observer)

        assertThat(observer.bulkAdded).containsExactly(Triple(2, 0, 1))
    }

    @Test
    fun `adding a client notifies the observers with its position`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val observer = RecordingObserver()
        repo.addListObserver(observer)

        repo.addClient()

        assertThat(observer.added).hasSize(1)
        assertThat(observer.added[0].second).isEqualTo(0)
        assertThat(repo.clients).hasSize(1)
    }

    @Test
    fun `removing a client notifies the observers with its old position`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        dao.rows[2] = row(2, "Albrecht")
        val repo = repository()
        repo.initialize()
        val observer = RecordingObserver()
        repo.addListObserver(observer)
        val second = repo.clients[1]

        repo.removeClient(second)

        assertThat(observer.removed).hasSize(1)
        assertThat(observer.removed[0].first).isSameInstanceAs(second)
        assertThat(observer.removed[0].second).isEqualTo(1)
    }

    @Test
    fun `a removed observer stops being notified`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val observer = RecordingObserver()
        repo.addListObserver(observer)

        repo.removeListObserver(observer)
        repo.addClient()

        assertThat(observer.added).isEmpty()
    }

    // ---------------------------------------------------------------- ids

    @Test
    fun `each added client gets a fresh id from the preferences`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()

        repo.addClient()
        repo.addClient()

        assertThat(repo.clients.map { it.id }).containsExactly(1, 2).inOrder()
    }

    // ---------------------------------------------------------------- storing

    @Test
    fun `store inserts clients that were added`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        repo.addClient()
        repo.clients[0].name.value = "Neukunde"

        repo.store()

        assertThat(dao.insertCount).isEqualTo(1)
        assertThat(dao.rows.values.single().name).isEqualTo("Neukunde")
    }

    @Test
    fun `store does not insert the same client twice`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        repo.addClient()

        repo.store()
        repo.store()

        assertThat(dao.insertCount).isEqualTo(1)
    }

    @Test
    fun `store writes back a client that was touched`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        val repo = repository()
        repo.initialize()
        val client = repo.clients.single()
        client.name.value = "Bauer GmbH"
        client.touched = true

        repo.store()

        assertThat(dao.updateCount).isEqualTo(1)
        assertThat(dao.rows[1]!!.name).isEqualTo("Bauer GmbH")
    }

    @Test
    fun `store leaves untouched clients alone`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        val repo = repository()
        repo.initialize()

        repo.store()

        assertThat(dao.updateCount).isEqualTo(0)
    }

    @Test
    fun `store deletes clients that were removed`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        val repo = repository()
        repo.initialize()

        repo.removeClient(repo.clients.single())
        repo.store()

        assertThat(dao.deleteCount).isEqualTo(1)
        assertThat(dao.rows).doesNotContainKey(1)
    }

    @Test
    fun `store does not delete the same client twice`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        val repo = repository()
        repo.initialize()
        repo.removeClient(repo.clients.single())

        repo.store()
        repo.store()

        assertThat(dao.deleteCount).isEqualTo(1)
    }

    @Test
    fun `removing a client that is not in the list does nothing`(): Unit = runBlocking {
        dao.rows[1] = row(1, "Bauer")
        val repo = repository()
        repo.initialize()

        repo.removeClient(Client(999, "Ghost"))
        repo.store()

        assertThat(repo.clients).hasSize(1)
        assertThat(dao.deleteCount).isEqualTo(0)
    }

    @Test
    fun `an added and then removed client does not survive the store`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        repo.addClient()
        val added = repo.clients.single()

        repo.removeClient(added)
        repo.store()

        assertThat(repo.clients).isEmpty()
        assertThat(dao.rows).isEmpty()
    }

    // ---------------------------------------------------------------- mapping

    @Test
    fun `createClientDb carries every field`() {
        val client = Client(3, "Name", "Street", "12345", "City", 42, true, "01:30", true, "Notes")

        val db = client.createClientDb()

        assertThat(db).isEqualTo(
            ClientDb(3, "Name", "Street", "12345", "City", 42, true, "01:30", true, "Notes"))
    }

    @Test
    fun `clients are considered equal when their ids match`() {
        assertThat(Client(1, "A").equal(Client(1, "B"))).isTrue()
        assertThat(Client(1, "A").equal(Client(2, "A"))).isFalse()
    }
}
