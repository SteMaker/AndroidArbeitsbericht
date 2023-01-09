package com.stemaker.arbeitsbericht.data.report

class DefaultValues {
    var defaultDriveTime: String = "00:00"
    var useDefaultDriveTime: Boolean = false
    var defaultDistance: Int = 0
    var useDefaultDistance: Boolean = false

    fun copyFromDb(d: DefaultValuesDb) {
        defaultDriveTime = d.defaultDriveTime
        useDefaultDriveTime = d.useDefaultDriveTime
        defaultDistance = d.defaultDistance
        useDefaultDistance = d.useDefaultDistance
    }

    fun copy(d: DefaultValues) {
        defaultDriveTime = d.defaultDriveTime
        useDefaultDriveTime = d.useDefaultDriveTime
        defaultDistance = d.defaultDistance
        useDefaultDistance = d.useDefaultDistance
    }
}