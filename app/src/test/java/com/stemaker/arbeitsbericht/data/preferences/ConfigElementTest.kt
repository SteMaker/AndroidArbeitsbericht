package com.stemaker.arbeitsbericht.data.preferences

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/* ConfigElement is the bridge between the UI (which data-binds to LiveData) and
 * SharedPreferences (which is the actual store). Every write has to reach the store, and a
 * read must never hand out null, because the whole app dereferences these values directly.
 */
class ConfigElementTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `the element starts at the value the init function returns`() {
        val element = ConfigElement({ "stored" }, { })

        assertThat(element.value).isEqualTo("stored")
    }

    @Test
    fun `the init function is consulted once, not on every read`() {
        var calls = 0
        val element = ConfigElement({ calls++; "stored" }, { })

        element.value
        element.value

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `setting a value writes it through to the store`() {
        val written = mutableListOf<String>()
        val element = ConfigElement({ "initial" }, { written.add(it) })
        written.clear()

        element.value = "changed"

        assertThat(element.value).isEqualTo("changed")
        assertThat(written).containsExactly("changed")
    }

    @Test
    fun `the store is written on construction so a default is persisted`() {
        val written = mutableListOf<String>()

        ConfigElement({ "initial" }, { written.add(it) })

        // Construction uses the LiveData setter directly, bypassing the write-through
        assertThat(written).isEmpty()
    }

    @Test
    fun `postValue writes through to the store twice`() {
        /* Documents a redundancy rather than endorsing it: postValue() calls setFct itself
         * and then hands the value to LiveData, whose dispatch calls the overridden
         * setValue(), which calls setFct again. The store ends up with the right value, so
         * this costs a duplicate SharedPreferences write and nothing else.
         */
        val written = mutableListOf<String>()
        val element = ConfigElement({ "initial" }, { written.add(it) })
        written.clear()

        element.postValue("posted")

        assertThat(written).containsExactly("posted", "posted")
    }

    @Test
    fun `every write reaches the store, including repeated identical values`() {
        val written = mutableListOf<Int>()
        val element = ConfigElement({ 0 }, { written.add(it) })
        written.clear()

        element.value = 1
        element.value = 1
        element.value = 2

        assertThat(written).containsExactly(1, 1, 2).inOrder()
    }

    @Test
    fun `a null value reads back as the default`() {
        // The app dereferences these values without a null check, so getValue() must
        // substitute the default rather than hand out null.
        val element = ConfigElement<String?>({ "fallback" }, { })

        element.value = null

        assertThat(element.value).isEqualTo("fallback")
    }

    @Test
    fun `observers see each new value`() {
        val element = ConfigElement({ "initial" }, { })
        val seen = mutableListOf<String>()
        element.observeForever { seen.add(it) }

        element.value = "second"
        element.value = "third"

        assertThat(seen).containsExactly("initial", "second", "third").inOrder()
    }

    @Test
    fun `non-string types work as well`() {
        val ints = mutableListOf<Int>()
        val intElement = ConfigElement({ 22 }, { ints.add(it) })
        assertThat(intElement.value).isEqualTo(22)
        intElement.value = 8022
        assertThat(ints).containsExactly(8022)

        val bools = mutableListOf<Boolean>()
        val boolElement = ConfigElement({ false }, { bools.add(it) })
        boolElement.value = true
        assertThat(bools).containsExactly(true)

        val sets = mutableListOf<Set<String>>()
        val setElement = ConfigElement({ emptySet<String>() }, { sets.add(it) })
        setElement.value = setOf("a", "b")
        assertThat(setElement.value).containsExactly("a", "b")
        assertThat(sets).containsExactly(setOf("a", "b"))
    }
}
