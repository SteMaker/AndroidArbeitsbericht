package com.stemaker.arbeitsbericht.data.base

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/* The DataElement / DataObject / DataContainer trio is the foundation the whole report
 * model is built from, and every change notification the editor UI reacts to flows through
 * it. These tests pin down the value handling and the event propagation.
 */
class DataBaseClassesTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /* A DataObject with a couple of primitive elements, used as the subject below. */
    private class Sample : DataObject("sample") {
        val text = DataElement<String>("text") { "initial" }
        val number = DataElement<Int>("number") { 7 }
        override val elements = listOf<DataBasicIf>(text, number)
    }

    /* Records the events a modification LiveData produces.
     *
     * dataModificationEvent is a mapped/mediator LiveData, so it only produces anything
     * while it is observed, and subscribing itself produces events: LiveData replays a
     * value that is already set, and DataObject/DataContainer wire up their sources on
     * first observation, which primes one ELEMENT_CHANGE per element. Tests therefore
     * subscribe, call baseline() to drop that noise, and only then act.
     */
    private class Events(source: LiveData<DataModificationEvent>) {
        private val seen = mutableListOf<DataModificationEvent>()

        init {
            source.observeForever { seen.add(it) }
        }

        fun baseline(): Events = apply { seen.clear() }

        val size get() = seen.size
        val types get() = seen.map { it.type }
        val elements get() = seen.map { it.elem }
        val single get() = seen.single()
    }

    @Test
    fun `DataElement takes its initial value from the init function`() {
        val element = DataElement<String>("name") { "Hans" }

        assertThat(element.value).isEqualTo("Hans")
        assertThat(element.elementName).isEqualTo("name")
    }

    @Test
    fun `DataElement init function is evaluated once, not per read`() {
        var calls = 0
        val element = DataElement<Int>("counter") { calls++; 42 }

        assertThat(element.value).isEqualTo(42)
        assertThat(element.value).isEqualTo(42)
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `DataElement copy takes the value of the origin`() {
        val origin = DataElement<String>("a") { "source" }
        val target = DataElement<String>("b") { "target" }

        target.copy(origin)

        assertThat(target.value).isEqualTo("source")
        // Copying values must not rename the target
        assertThat(target.elementName).isEqualTo("b")
    }

    @Test
    fun `observing a DataElement replays its current value as an event`() {
        // Documents the priming behaviour the other tests have to work around: a fresh
        // observer is told about the state that already exists.
        val element = DataElement<String>("field") { "preset" }

        val events = Events(element.dataModificationEvent)

        assertThat(events.size).isEqualTo(1)
        assertThat(events.single.type).isEqualTo(DataModificationEvent.Type.ELEMENT_CHANGE)
    }

    @Test
    fun `changing a DataElement emits an ELEMENT_CHANGE event for that element`() {
        val element = DataElement<String>("field") { "" }
        val events = Events(element.dataModificationEvent).baseline()

        element.value = "typed"

        assertThat(events.size).isEqualTo(1)
        assertThat(events.single.type).isEqualTo(DataModificationEvent.Type.ELEMENT_CHANGE)
        assertThat(events.single.elem).isSameInstanceAs(element)
    }

    @Test
    fun `DataObject forwards events from each of its elements`() {
        val sample = Sample()
        val events = Events(sample.dataModificationEvent).baseline()

        sample.text.value = "changed"
        sample.number.value = 9

        assertThat(events.types).containsExactly(
            DataModificationEvent.Type.ELEMENT_CHANGE,
            DataModificationEvent.Type.ELEMENT_CHANGE)
        assertThat(events.elements).containsExactly(sample.text, sample.number).inOrder()
    }

    @Test
    fun `DataObject prefixes its elements with the object name`() {
        val sample = Sample()

        // The prefixing happens lazily when dataModificationEvent is first touched
        sample.dataModificationEvent

        assertThat(sample.text.elementName).isEqualTo("sample.text")
        assertThat(sample.number.elementName).isEqualTo("sample.number")
    }

    @Test
    fun `DataContainer starts out empty`() {
        val container = DataContainer<DataElement<String>>("items")

        assertThat(container.getSize()).isEqualTo(0)
        assertThat(container.items).isEmpty()
        assertThat(container.toList()).isEmpty()
    }

    @Test
    fun `adding to a DataContainer emits CONTAINER_ADD and grows the container`() {
        val container = DataContainer<DataElement<String>>("items")
        val events = Events(container.dataModificationEvent).baseline()
        val element = DataElement<String>("entry") { "one" }

        container.add(element)

        assertThat(container.getSize()).isEqualTo(1)
        assertThat(events.types).contains(DataModificationEvent.Type.CONTAINER_ADD)
        val added = events.elements[events.types.indexOf(DataModificationEvent.Type.CONTAINER_ADD)]
        assertThat(added).isSameInstanceAs(element)
    }

    @Test
    fun `removing from a DataContainer emits CONTAINER_REMOVE and shrinks the container`() {
        val container = DataContainer<DataElement<String>>("items")
        val element = DataElement<String>("entry") { "one" }
        container.add(element)
        val events = Events(container.dataModificationEvent).baseline()

        container.remove(element)

        assertThat(container.getSize()).isEqualTo(0)
        assertThat(events.types).containsExactly(DataModificationEvent.Type.CONTAINER_REMOVE)
    }

    @Test
    fun `clearing a DataContainer emits CONTAINER_CLEAR and drops every item`() {
        val container = DataContainer<DataElement<String>>("items")
        container.add(DataElement<String>("a") { "1" })
        container.add(DataElement<String>("b") { "2" })
        val events = Events(container.dataModificationEvent).baseline()

        container.clear()

        assertThat(container.getSize()).isEqualTo(0)
        assertThat(container.items).isEmpty()
        assertThat(events.types).containsExactly(DataModificationEvent.Type.CONTAINER_CLEAR)
    }

    @Test
    fun `DataContainer forwards changes made to the items it holds`() {
        val container = DataContainer<DataElement<String>>("items")
        val element = DataElement<String>("entry") { "" }
        container.add(element)
        val events = Events(container.dataModificationEvent).baseline()

        element.value = "edited"

        assertThat(events.types).containsExactly(DataModificationEvent.Type.ELEMENT_CHANGE)
        assertThat(events.single.elem).isSameInstanceAs(element)
    }

    @Test
    fun `a removed item no longer reports through the container`() {
        val container = DataContainer<DataElement<String>>("items")
        val element = DataElement<String>("entry") { "" }
        container.add(element)
        container.remove(element)
        val events = Events(container.dataModificationEvent).baseline()

        element.value = "edited after removal"

        assertThat(events.size).isEqualTo(0)
    }

    @Test
    fun `a cleared item no longer reports through the container`() {
        val container = DataContainer<DataElement<String>>("items")
        val element = DataElement<String>("entry") { "" }
        container.add(element)
        container.clear()
        val events = Events(container.dataModificationEvent).baseline()

        element.value = "edited after clear"

        assertThat(events.size).isEqualTo(0)
    }

    @Test
    fun `DataContainer iterates its items in insertion order`() {
        val container = DataContainer<DataElement<String>>("items")
        val first = DataElement<String>("a") { "1" }
        val second = DataElement<String>("b") { "2" }
        val third = DataElement<String>("c") { "3" }
        container.add(first)
        container.add(second)
        container.add(third)

        assertThat(container.toList()).containsExactly(first, second, third).inOrder()
        assertThat(container.map { it.value }).containsExactly("1", "2", "3").inOrder()
    }

    @Test
    fun `re-adding items after a clear keeps the container consistent`() {
        // The report model clears and refills containers on every load from the database,
        // so this round trip has to leave the container in a usable state.
        val container = DataContainer<DataElement<String>>("items")
        container.add(DataElement<String>("a") { "1" })
        container.clear()
        val reAdded = DataElement<String>("b") { "2" }
        container.add(reAdded)

        assertThat(container.getSize()).isEqualTo(1)
        assertThat(container.toList()).containsExactly(reAdded)

        val events = Events(container.dataModificationEvent).baseline()
        reAdded.value = "changed"
        assertThat(events.types).containsExactly(DataModificationEvent.Type.ELEMENT_CHANGE)
    }
}
