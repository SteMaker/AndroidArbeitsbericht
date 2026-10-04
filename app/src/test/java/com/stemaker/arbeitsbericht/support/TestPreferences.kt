package com.stemaker.arbeitsbericht.support

import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.data.preferences.ConfigElement
import io.mockk.every
import io.mockk.mockk

/* AbPreferences needs a Context for SharedPreferences and for the Android keystore backing
 * EncryptedSharedPreferences, neither of which a plain JVM test has. Everything under test
 * here only reads a handful of ConfigElements off it, so we hand out real ConfigElements
 * (they are ordinary MutableLiveData) from a mock.
 */
fun <T> configElementOf(value: T): ConfigElement<T> = ConfigElement({ value }, { })

class FakePreferences {
    var nextReportId = 1
    var nextClientId = 1

    val employeeName = configElementOf("")
    val deviceName = configElementOf("")
    val reportIdPattern = configElementOf("%p-%y-%6c")
    val activeReportId = configElementOf(-1)
    val filterProjectName = configElementOf("")
    val filterProjectExtra = configElementOf("")
    val filterStates = configElementOf(0b0111)
    val lumpSums = configElementOf(emptySet<String>())
    val workItemDictionary = configElementOf(emptySet<String>())
    val materialDictionary = configElementOf(emptySet<String>())

    val mock: AbPreferences = mockk(relaxed = true) {
        every { employeeName } returns this@FakePreferences.employeeName
        every { deviceName } returns this@FakePreferences.deviceName
        every { reportIdPattern } returns this@FakePreferences.reportIdPattern
        every { activeReportId } returns this@FakePreferences.activeReportId
        every { filterProjectName } returns this@FakePreferences.filterProjectName
        every { filterProjectExtra } returns this@FakePreferences.filterProjectExtra
        every { filterStates } returns this@FakePreferences.filterStates
        every { lumpSums } returns this@FakePreferences.lumpSums
        every { workItemDictionary } returns this@FakePreferences.workItemDictionary
        every { materialDictionary } returns this@FakePreferences.materialDictionary
        every { allocateReportId() } answers { this@FakePreferences.nextReportId++ }
        every { allocateClientId() } answers { this@FakePreferences.nextClientId++ }
        every { currentId } answers { this@FakePreferences.nextReportId }
    }
}

/* Convenience for the common case of only caring about the id pattern inputs. */
fun fakePreferences(
    employeeName: String = "",
    deviceName: String = "",
    reportIdPattern: String = "%p-%y-%6c",
): AbPreferences = FakePreferences().apply {
    this.employeeName.value = employeeName
    this.deviceName.value = deviceName
    this.reportIdPattern.value = reportIdPattern
}.mock
