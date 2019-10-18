package com.stemaker.arbeitsbericht.data

import com.stemaker.arbeitsbericht.*
import kotlinx.serialization.Serializable

@Serializable
class SignatureDataSerialized {
    var employeeSignatureSvg: String = ""
    var clientSignatureSvg: String = ""

    fun copyFromData(s: SignatureData) {
        employeeSignatureSvg = s.employeeSignatureSvg.value!!
        clientSignatureSvg = s.clientSignatureSvg.value!!
    }
}

@Serializable
class PhotoContainerDataSerialized() {
    val items = mutableListOf<PhotoDataSerialized>()

    fun copyFromData(p: PhotoContainerData) {
        items.clear()
        for(i in 0 until p.items.size) {
            val item = PhotoDataSerialized()
            item.copyFromData(p.items[i])
            items.add(item)
        }
    }
}

@Serializable
class PhotoDataSerialized() {
    var file: String = ""
    var description: String = ""

    fun copyFromData(p: PhotoData) {
        file = p.file.value!!
        description = p.description.value!!
    }
}

@Serializable
class LumpSumDataSerialized() {
    var item: String = ""

    fun copyFromData(l: LumpSumData) {
        item = l.item.value!!
    }
}

@Serializable
class LumpSumContainerDataSerialized() {
    val items = mutableListOf<LumpSumDataSerialized>()

    fun copyFromData(l: LumpSumContainerData) {
        items.clear()
        for(i in 0 until l.items.size) {
            val item = LumpSumDataSerialized()
            item.copyFromData(l.items[i])
            items.add(item)
        }
    }
}

@Serializable
class MaterialDataSerialized() {
    var item: String = ""
    var amount: Int = 0

    fun copyFromData(w: MaterialData) {
        item = w.item.value!!
        amount = w.amount.value!!
    }
}

@Serializable
class MaterialContainerDataSerialized() {
    val items = mutableListOf<MaterialDataSerialized>()

    fun copyFromData(m: MaterialContainerData) {
        items.clear()
        for(i in 0 until m.items.size) {
            val item = MaterialDataSerialized()
            item.copyFromData(m.items[i])
            items.add(item)
        }
    }
}

@Serializable
class WorkItemDataSerialized() {
    var item: String = ""
    fun copyFromData(w: WorkItemData) {
        item = w.item.value!!
    }
}

@Serializable
class WorkItemContainerDataSerialized() {
    val items = mutableListOf<WorkItemDataSerialized>()

    fun copyFromData(w: WorkItemContainerData) {
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkItemDataSerialized()
            item.copyFromData(w.items[i])
            items.add(item)
        }
    }
}

@Serializable
class WorkTimeDataSerialized() {
    var date: String = ""
    var employee: String = storageHandler().configuration.employeeName
    var duration: String = "00:00"
    var driveTime: String = "00:00"
    var distance: Int = 0


    fun copyFromData(w: WorkTimeData) {
        date = w.date.value!!
        employee = w.employee.value!!
        duration = w.duration.value!!
        driveTime = w.driveTime.value!!
        distance = w.distance.value!!
    }
}

@Serializable
class WorkTimeContainerDataSerialized() {
    val items = mutableListOf<WorkTimeDataSerialized>()

    fun copyFromData(w: WorkTimeContainerData) {
        items.clear()
        for(i in 0 until w.items.size) {
            val item = WorkTimeDataSerialized()
            item.copyFromData(w.items[i])
            items.add(item)
        }
    }

}

@Serializable
class BillDataSerialized {
    var name: String = ""
    var street: String = ""
    var zip: String = ""
    var city: String = ""

    fun copyFromData(b: BillData) {
        name = b.name.value!!
        street = b.street.value!!
        zip = b.zip.value!!
        city = b.city.value!!
    }

}

@Serializable
class ProjectDataSerialized {
    var name: String = ""
    var extra1: String = ""

    fun copyFromData(p: ProjectData) {
        name = p.name.value!!
        extra1 = p.extra1.value!!
    }

}

@Serializable
class ReportDataSerialized() {
    var id: Int = 0
    var create_date: String = "00:00"
    var change_date: String = "00:00"
    var project = ProjectDataSerialized()
    var bill = BillDataSerialized()
    val workTimeContainer = WorkTimeContainerDataSerialized()
    val workItemContainer = WorkItemContainerDataSerialized()
    val materialContainer = MaterialContainerDataSerialized()
    val lumpSumContainer = LumpSumContainerDataSerialized()
    val photoContainer = PhotoContainerDataSerialized()
    val signatureData = SignatureDataSerialized()

    fun copyFromData(r: ReportData) {
        id = r.id.value!!
        create_date = r.create_date.value!!
        change_date = r.change_date.value!!
        project.copyFromData(r.project)
        bill.copyFromData(r.bill)
        workTimeContainer.copyFromData(r.workTimeContainer)
        workItemContainer.copyFromData(r.workItemContainer)
        materialContainer.copyFromData(r.materialContainer)
        lumpSumContainer.copyFromData(r.lumpSumContainer)
        photoContainer.copyFromData(r.photoContainer)
        signatureData.copyFromData(r.signatureData)
    }

}