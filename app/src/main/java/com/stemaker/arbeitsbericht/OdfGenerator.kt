package com.stemaker.arbeitsbericht

import android.app.Activity
import android.os.*
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.helpers.*
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.doc.table.OdfTable
import org.odftoolkit.odfdom.dom.OdfContentDom
import org.odftoolkit.odfdom.dom.OdfMetaDom
import org.odftoolkit.odfdom.dom.element.meta.MetaUserDefinedElement
import org.odftoolkit.odfdom.dom.element.office.OfficeMetaElement
import org.odftoolkit.odfdom.dom.element.text.TextSoftPageBreakElement
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily
import org.odftoolkit.odfdom.dom.style.props.OdfGraphicProperties
import org.odftoolkit.odfdom.incubator.doc.draw.OdfDrawImage
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.pkg.OdfPackage
import org.w3c.dom.Node
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "OdfGenerator"

private const val MSG_CREATE = 0x01

private class Create(val odfFile: File, val clientSigFile: File, val employeeSigFile: File?, val cont: Continuation<CreateReturn>)
private class CreateReturn(val success: Boolean, val error: String = "")

class OdfGenerator(val activity: Activity, val report: ReportData) {

    fun isOdfUpToDate(odfFile: File, report: ReportData): Boolean {
        lateinit var doc: OdfTextDocument
        try {
            doc = getDoc(odfFile)
        } catch(e:Exception) {
            return false
        }
        val metaDom = doc.metaDom
        lateinit var node: Node
        try {
            node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='reportChksum']", metaDom, XPathConstants.NODE) as Node
        } catch (e: XPathExpressionException) {
            return false
        } catch (e: Exception) {
            return false
        }

        node as MetaUserDefinedElement
        Log.d(TAG, "Hash of odf is ${node.textContent}")
        if(node.textContent == report.lastStoreHash.toString() && report.lastStoreHash != 0) {
            Log.d(TAG, "ODF is up to date")
            return true
        } else {
            Log.d(TAG, "ODF is outdated")
            return false
        }
    }

    suspend fun getFilesForOdfGeneration(): Array<File>? {
        val path = "${Environment.getExternalStorageDirectory().absolutePath}/Arbeitsbericht"

        /* Create the Documents folder if it doesn't exist */
        val folder = File(path)
        if(!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.d(TAG, "Could not create documents directory")
                showInfoDialog(activity.getString(R.string.cannot_createdocs_dir), activity)
                return null
            }
        }

        val odfFileName = "report_${report.id}.odt"
        val odfF = File(path, odfFileName)
        val clientSigFileName = "report_client_sig_${report.id}.png"
        val clientSigF = File(path, clientSigFileName)
        val employeeSigFileName = "report_employee_sig_${report.id}.png"
        val employeeSigF = File(path, employeeSigFileName)

        try {
            // ODF file
            if(odfF.exists()) {
                Log.d(TAG, "The report did already exist")
                val answer =
                    showConfirmationDialog(activity.getString(R.string.report_exists_overwrite), activity)
                if(answer != AlertDialog.BUTTON_POSITIVE) {
                    return null
                }
            } else {
                odfF.createNewFile()
            }

            // client signature bitmap file
            if(!clientSigF.exists())
                clientSigF.createNewFile()

            // employee signature bitmap file
            if(!employeeSigF.exists())
                employeeSigF.createNewFile()
        } catch(e:SecurityException) {
            Log.d(TAG, "Permission denied on file ${odfF.toString()}")
            showInfoDialog(activity.getString(R.string.report_create_fail_perm), activity)
            return null
        } catch(e: IOException) {
            Log.d(TAG, "IOException: Could not create report file ${odfF.toString()}")
            showInfoDialog(activity.getString(R.string.report_create_fail_unknown), activity)
            return null
        }

        return arrayOf(odfF, clientSigF, employeeSigF)
    }

    suspend fun create(odfFile: File, clientSigFile: File, employeeSigFile: File?) {
        val msg = Message()
        msg.what = MSG_CREATE
        val ret = suspendCoroutine<CreateReturn> {
            msg.obj = Create(odfFile, clientSigFile, employeeSigFile, it)
            odfHandler.sendMessage(msg)
            Log.d(TAG, "create() Coroutine: suspended")
        }
        if(ret.success) return
        throw(Exception(ret.error))
    }

    /////////////////////////
    // THREAD RELATED STUFF
    /////////////////////////
    var odfFile: File? = null
    var clientSigFile: File? = null
    var employeeSigFile: File? = null

    // This class is used to handle messages within the odf thread
    private inner class OdfThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            Log.d(TAG, "handleMessage ${msg?.what?:-1} in ODF Thread")
            when(msg?.what) {
                MSG_CREATE -> handleCreate(msg)
            }
        }
    }


    /* This class is used to handle messages within the UI thread -> not used right now
    private inner class UiThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            Log.d(TAG, "handleMessage ${msg?.what?:-1} in UI Thread")
            when (msg?.what) {
                MSG_DONE -> {
                }
            }
        }
    } */

    private val odfHandlerThread = HandlerThread("ODF Thread").apply {
        start()
    }
    private val odfHandler = OdfThreadHandler(odfHandlerThread.looper)
    //private val uiHandler = UiThreadHandler(Looper.getMainLooper())

    ////////////////////////////////////////////////////////
    // BELOW FUNCTIONS ARE SUPPOSED TO RUN IN THE ODF THREAD
    ////////////////////////////////////////////////////////
    private fun handleCreate(msg: Message) {
        Log.d(TAG, "create()")
        val cm = msg.obj as Create
        // TODO: Add the template File object
        odfFile = cm.odfFile
        clientSigFile = cm.clientSigFile
        employeeSigFile = cm.employeeSigFile

        try {
            addImagesToPackage()

            val doc = getDoc(odfFile!!)
            val metaDom = doc.metaDom
            val contentDom = doc.contentDom
            setBaseData(metaDom)
            setBillData(metaDom)
            setProjectData(metaDom)
            setWorkTime(contentDom)
            setWorkItem(contentDom)
            setLumpSum(contentDom)
            setMaterial(contentDom)
            setPhoto(contentDom)
            setSignatures(contentDom)

            // Create a checksum so that we can check later if the report has been changed compared to the ODF
            val parent = metaDom.xPath.evaluate("//office:document-meta/office:meta", metaDom, XPathConstants.NODE) as OfficeMetaElement
            parent.newMetaUserDefinedElement("reportChksum", "string").newTextNode(report.lastStoreHash.toString())

            // Save document
            doc.save(odfFile)
            cm.cont.resume(CreateReturn((true)))
        } catch (e:Exception) {
            cm.cont.resume(CreateReturn(false, e.message?:activity.getString(R.string.unknown)))
        }
    }

    private fun getDoc(odfFile: File): OdfTextDocument {
        return OdfTextDocument.loadDocument(odfFile)
    }

    private fun addImagesToPackage(): OdfPackage {
        val templateFile = when(configuration().odfTemplateFile){
            "" ->  activity.assets.open("output_template.ott")
            else -> FileInputStream(File(configuration().odfTemplateFile))
        }

        val pkg = OdfPackage.loadPackage(templateFile)
        report.photoContainer.items.forEachIndexed { index, elem ->
            pkg.insert(File(elem.file.value).toURI(), "Pictures/photo$index.jpg", "image/jpg")
        }
        if (clientSigFile != null) {
            pkg.insert(clientSigFile!!.toURI(), "Pictures/clientSig.png", "image/png")
        }
        if (employeeSigFile != null) {
            pkg.insert(employeeSigFile!!.toURI(), "Pictures/employeeSig.png", "image/png")
        }

        // Save document
        pkg.save(odfFile)
        return pkg
    }

    private fun setBaseData(metaDom: OdfMetaDom) {
        // Report ID
        val node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='report_id']", metaDom, XPathConstants.NODE) as Node
        val reportIdNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "report_id"
            metaValueTypeAttribute = "string"
            newTextNode(report.id.toString())
        }
        node.parentNode.replaceChild(reportIdNode, node)

        // Creation date
        val node2 = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='create_date']", metaDom, XPathConstants.NODE) as Node
        val reportIdNode2 = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "create_date"
            metaValueTypeAttribute = "string"
            newTextNode(report.create_date.value)
        }
        node2.parentNode.replaceChild(reportIdNode2, node2)
    }

    private fun setBillData(metaDom: OdfMetaDom) {
        var node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_name']", metaDom, XPathConstants.NODE) as Node
        var newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_name"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.name.value)
        }
        node.parentNode.replaceChild(newNode, node)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_street']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_street"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.street.value)
        }
        node.parentNode.replaceChild(newNode, node)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_zip']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_zip"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.zip.value.toString())
        }
        node.parentNode.replaceChild(newNode, node)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_city']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_city"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.city.value)
        }
        node.parentNode.replaceChild(newNode, node)
    }

    private fun setProjectData(metaDom: OdfMetaDom) {
        var node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='project_name']", metaDom, XPathConstants.NODE) as Node
        var newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "project_name"
            metaValueTypeAttribute = "string"
            newTextNode(report.project.name.value)
        }
        node.parentNode.replaceChild(newNode, node)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='project_extra1']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "project_extra1"
            metaValueTypeAttribute = "string"
            newTextNode(report.project.extra1.value)
        }
        node.parentNode.replaceChild(newNode, node)
    }

    private fun setWorkTime(contentDom: OdfContentDom) {
        if(report.workTimeContainer.items.size == 0) return
        val node = getParagraphOfTaggedNode(contentDom, "worktimetag")
        val parent = node.parentNode as OdfElement
        val table = OdfTable.newTable(parent, report.workTimeContainer.items.size, 6, 1, 0)
        table.getCellByPosition(0,0).setDisplayText("Datum")
        table.getCellByPosition(1,0).setDisplayText("Mitarbeiter")
        table.getCellByPosition(2,0).setDisplayText("Arbeitsanfang")
        table.getCellByPosition(3,0).setDisplayText("Arbeitsende")
        table.getCellByPosition(4,0).setDisplayText("Fahrzeit [h:m]")
        table.getCellByPosition(5,0).setDisplayText("Fahrstrecke [km]")
        var idx = 1
        for(item in report.workTimeContainer.items) {
            table.getCellByPosition(0, idx).setDisplayText(item.date.value)
            var employees = ""
            for(e in item.employees) employees += "${e.value}\n"
            table.getCellByPosition(1, idx).setDisplayText(employees)
            table.getCellByPosition(2, idx).setDisplayText(item.startTime.value)
            table.getCellByPosition(3, idx).setDisplayText(item.endTime.value)
            table.getCellByPosition(4, idx).setDisplayText(item.driveTime.value)
            table.getCellByPosition(5, idx).setDisplayText(item.distance.value.toString())
            idx++
        }
        parent.insertBefore(table.odfElement.cloneNode(true), node)
        parent.removeChild(table.odfElement)
        parent.removeChild(node)
    }

    private fun setWorkItem(contentDom: OdfContentDom) {
        if(report.workItemContainer.items.size == 0) return
        val node = getParagraphOfTaggedNode(contentDom, "workitemtag")
        val parent = node.parentNode as OdfElement
        val table = OdfTable.newTable(parent, report.workItemContainer.items.size, 1, 1, 0)
        table.getCellByPosition(0, 0).setDisplayText("Arbeit")
        var idx = 1
        for(item in report.workItemContainer.items) {
            table.getCellByPosition(0, idx).setDisplayText(item.item.value)
            idx++
        }
        parent.insertBefore(table.odfElement.cloneNode(true), node)
        parent.removeChild(table.odfElement)
        parent.removeChild(node)
    }

    private fun setLumpSum(contentDom: OdfContentDom) {
        if(report.lumpSumContainer.items.size == 0) return
        val node = getParagraphOfTaggedNode(contentDom, "lumpsumtag")
        val parent = node.parentNode as OdfElement
        val table = OdfTable.newTable(parent, report.lumpSumContainer.items.size, 3, 1, 0)
        table.getCellByPosition(0, 0).setDisplayText("Pauschale")
        table.getCellByPosition(1, 0).setDisplayText("Anzahl")
        table.getCellByPosition(1, 0).setDisplayText("Bemerkung")
        var idx = 1
        for(item in report.lumpSumContainer.items) {
            table.getCellByPosition(0, idx).setDisplayText(item.item.value)
            table.getCellByPosition(1, idx).setDisplayText(item.amount.value.toString())
            table.getCellByPosition(2, idx).setDisplayText(item.comment.value)
            idx++
        }
        parent.insertBefore(table.odfElement.cloneNode(true), node)
        parent.removeChild(table.odfElement)
        parent.removeChild(node)
    }

    private fun setMaterial(contentDom: OdfContentDom) {
        if(report.materialContainer.items.size == 0) return
        val node = getParagraphOfTaggedNode(contentDom, "materialtag")
        val parent = node.parentNode as OdfElement
        val table = OdfTable.newTable(parent, report.materialContainer.items.size, 2, 1, 0)
        table.getCellByPosition(0, 0).setDisplayText("Material")
        table.getCellByPosition(1, 0).setDisplayText("Anzahl")
        var idx = 1
        for(item in report.materialContainer.items) {
            table.getCellByPosition(0, idx).setDisplayText(item.item.value)
            table.getCellByPosition(1, idx).setDisplayText(item.amount.value.toString())
            idx++
        }
        parent.insertBefore(table.odfElement.cloneNode(true), node)
        parent.removeChild(table.odfElement)
        parent.removeChild(node)
    }

    private fun setPhoto(contentDom: OdfContentDom) {
        if(report.photoContainer.items.size == 0) return
        val node = getTaggedNode(contentDom, "phototag")
        val parent = getParentParagraphOfNode(node) // this is the level we want the text:p to be located
        val container = parent.parentNode // this is one level above so the one that shall take the new text:p

        report.photoContainer.items.forEachIndexed { index, photoData ->
            val textP = OdfTextParagraph(contentDom)
            val frame = textP.newDrawFrameElement()
            frame.svgWidthAttribute = "17cm"
            val ratio: Float = photoData.imageHeight.toFloat() / photoData.imageWidth.toFloat()
            frame.svgHeightAttribute = "${17.0*ratio}cm"
            val drawImage = frame.newDrawImageElement() as OdfDrawImage
            drawImage.setImagePath("Pictures/photo$index.jpg")
            val pbreak = TextSoftPageBreakElement(contentDom)
            textP.appendChild(pbreak)
            container.insertBefore(textP, parent)
        }
        container.removeChild(parent)
    }

    private fun setSignatures(contentDom: OdfContentDom) {
        val style = contentDom.orCreateAutomaticStyles.newStyle(OdfStyleFamily.Graphic)
        style.setProperty(OdfGraphicProperties.Wrap, "none")
        style.setAttribute("style:name", "signatureStyle")
        if(clientSigFile != null) {
            val node = getTaggedNode(contentDom, "signatureemployeetag")
            val parent = getParentParagraphOfNode(node) // this is the level we want the text:p to be located
            val container = parent.parentNode // this is one level above so the one that shall take the new text:p

            val textP = OdfTextParagraph(contentDom)
            val frame = textP.newDrawFrameElement()
            frame.svgWidthAttribute = "7cm"
            // TODO
            val ratio = 0.3
            frame.svgHeightAttribute = "${7.0 * ratio}cm"
            frame.styleName = "signatureStyle"
            val drawImage = frame.newDrawImageElement() as OdfDrawImage
            drawImage.setImagePath("Pictures/clientSig.png")
            val pbreak = TextSoftPageBreakElement(contentDom)
            textP.appendChild(pbreak)
            container.insertBefore(textP, parent)
            container.removeChild(parent)
        }

        if(clientSigFile != null) {
            val node = getTaggedNode(contentDom, "signatureclienttag")
            val parent = getParentParagraphOfNode(node) // this is the level we want the text:p to be located
            val container = parent.parentNode // this is one level above so the one that shall take the new text:p

            val textP = OdfTextParagraph(contentDom)
            val frame = textP.newDrawFrameElement()
            frame.svgWidthAttribute = "7cm"
            // TODO
            val ratio = 0.3
            frame.svgHeightAttribute = "${7.0 * ratio}cm"
            frame.styleName = "signatureStyle"
            val drawImage = frame.newDrawImageElement() as OdfDrawImage
            drawImage.setImagePath("Pictures/employeeSig.png")
            val pbreak = TextSoftPageBreakElement(contentDom)
            textP.appendChild(pbreak)
            container.insertBefore(textP, parent)
            //frame.automaticStyle.setProperty(OdfGraphicProperties.Wrap, "none")
            container.removeChild(parent)
        }
    }

    private fun getTaggedNode(contentDom: OdfContentDom, tag: String): Node {
        return contentDom.xPath.evaluate("//text:user-field-get[@text:name='${tag}']", contentDom, XPathConstants.NODE) as Node
    }

    private fun getParentParagraphOfNode(_node: Node): OdfTextParagraph {
        var node = _node.parentNode
        while(node.nodeName != "text:p")
            node = node.parentNode
        return node as OdfTextParagraph
    }

    private fun getParagraphOfTaggedNode(contentDom: OdfContentDom, tag: String): OdfTextParagraph{
        val node = getTaggedNode(contentDom, tag)

        return getParentParagraphOfNode(node)
    }
}