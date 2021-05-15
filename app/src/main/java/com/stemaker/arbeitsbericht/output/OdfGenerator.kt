package com.stemaker.arbeitsbericht.output

import android.app.Activity
import android.os.Environment
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.configuration
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.doc.table.OdfTable
import org.odftoolkit.odfdom.dom.OdfContentDom
import org.odftoolkit.odfdom.dom.OdfMetaDom
import org.odftoolkit.odfdom.dom.element.meta.MetaUserDefinedElement
import org.odftoolkit.odfdom.dom.element.office.OfficeMetaElement
import org.odftoolkit.odfdom.dom.element.text.TextSoftPageBreakElement
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily
import org.odftoolkit.odfdom.dom.style.props.OdfGraphicProperties
import org.odftoolkit.odfdom.incubator.doc.draw.OdfDrawFrame
import org.odftoolkit.odfdom.incubator.doc.draw.OdfDrawImage
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph
import org.odftoolkit.odfdom.pkg.OdfElement
import org.odftoolkit.odfdom.pkg.OdfPackage
import org.odftoolkit.odfdom.type.Color
import org.w3c.dom.Node
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException

private const val TAG = "OdfGenerator"

class OdfGenerator(activity: Activity, report: ReportData, progressBar: ProgressBar?, textView: TextView?) :
    ReportGenerator(activity, report, progressBar, textView) {

    override fun getHash(files: Array<File>): String? {
        val odfFile = files[0]
        lateinit var doc: OdfTextDocument
        try {
            doc = getDoc(odfFile)
        } catch(e:Exception) {
            return null
        }
        val metaDom = doc.metaDom
        lateinit var node: Node
        try {
            node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='reportChksum']", metaDom, XPathConstants.NODE) as Node
        } catch (e: XPathExpressionException) {
            return null
        } catch (e: Exception) {
            return null
        }

        node as MetaUserDefinedElement
        Log.d(TAG, "Hash of odf is ${node.textContent}")
        return node.textContent
    }

    override val filePostFixExt: Array<Pair<String, String>>
        get() = arrayOf(Pair("", "odt"))


    var odfFile: File? = null
    var clientSigFile: File? = null
    var employeeSigFile: File? = null

    override fun createDoc(files: Array<File>, done: (success: Boolean) -> Unit) {
        odfFile = files[0]
        clientSigFile = files[1]
        employeeSigFile = files[2]

        try {
            progressInfo(10, activity.getString(R.string.status_add_pics))
            addImagesToPackage()

            val doc = getDoc(odfFile!!)
            val metaDom = doc.metaDom
            val contentDom = doc.contentDom
            progressInfo(15, activity.getString(R.string.status_add_base))
            setBaseData(metaDom, contentDom)
            progressInfo(20, activity.getString(R.string.status_add_bill))
            setBillData(metaDom, contentDom)
            progressInfo(30, activity.getString(R.string.status_add_project))
            setProjectData(metaDom, contentDom)
            progressInfo(40, activity.getString(R.string.status_add_worktime))
            setWorkTime(contentDom)
            progressInfo(50, activity.getString(R.string.status_add_workitem))
            setWorkItem(contentDom)
            progressInfo(60, activity.getString(R.string.status_add_lumpsum))
            setLumpSum(contentDom)
            progressInfo(70, activity.getString(R.string.status_add_material))
            setMaterial(contentDom)
            progressInfo(80, activity.getString(R.string.status_add_photo))
            setPhoto(contentDom)
            progressInfo(90, activity.getString(R.string.status_add_signature))
            setSignatures(contentDom)

            // Create a checksum so that we can check later if the report has been changed compared to the ODF
            val parent = metaDom.xPath.evaluate("//office:document-meta/office:meta", metaDom, XPathConstants.NODE) as OfficeMetaElement
            parent.newMetaUserDefinedElement("reportChksum", "string").newTextNode(report.lastStoreHash.toString())

            // Save document
            doc.save(odfFile)
            done(true)
        } catch (e:Exception) {
            done(false)
        }
    }

    private fun getDoc(odfFile: File): OdfTextDocument {
        return OdfTextDocument.loadDocument(odfFile)
    }

    private fun addImagesToPackage(): OdfPackage {
        val templateFile: InputStream
        if(configuration().odfTemplateFile != "" && File(activity.filesDir, configuration().odfTemplateFile).exists())
            templateFile = FileInputStream(File(activity.filesDir, configuration().odfTemplateFile))
        else
            templateFile = activity.assets.open("output_template.ott")

        val pkg = OdfPackage.loadPackage(templateFile)
        report.photoContainer.items.forEachIndexed { index, elem ->
            val tmpFile = File(elem.file.value)
            val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), tmpFile.name)
            if(file.exists()) {
                pkg.insert(file.toURI(), "Pictures/photo$index.jpg", "image/jpg")
            }
        }
        val csf = clientSigFile
        if (csf != null && csf.exists()) {
            pkg.insert(csf.toURI(), "Pictures/clientSig.png", "image/png")
        }
        val esf = employeeSigFile
        if (esf != null && esf.exists()) {
            pkg.insert(esf.toURI(), "Pictures/employeeSig.png", "image/png")
        }

        // Save document
        pkg.save(odfFile)
        return pkg
    }

    private fun setBaseData(metaDom: OdfMetaDom, contentDom: OdfContentDom) {
        // Report ID
        val repId = report.id
        val node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='report_id']", metaDom, XPathConstants.NODE) as Node
        val reportIdNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "report_id"
            metaValueTypeAttribute = "string"
            newTextNode(repId)
        }
        node.parentNode.replaceChild(reportIdNode, node)
        setTextUserDefinedNode(contentDom, "report_id", repId)

        // Creation date
        val node2 = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='create_date']", metaDom, XPathConstants.NODE) as Node
        val reportIdNode2 = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "create_date"
            metaValueTypeAttribute = "string"
            newTextNode(report.create_date.value)
        }
        node2.parentNode.replaceChild(reportIdNode2, node2)
        setTextUserDefinedNode(contentDom, "create_date", report.create_date.value!!)
    }

    private fun setBillData(metaDom: OdfMetaDom, contentDom: OdfContentDom) {
        var node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_name']", metaDom, XPathConstants.NODE) as Node
        var newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_name"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.name.value)
        }
        node.parentNode.replaceChild(newNode, node)
        setTextUserDefinedNode(contentDom, "bill_name", report.bill.name.value!!)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_street']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_street"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.street.value)
        }
        node.parentNode.replaceChild(newNode, node)
        setTextUserDefinedNode(contentDom, "bill_street", report.bill.street.value!!)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_zip']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_zip"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.zip.value.toString())
        }
        node.parentNode.replaceChild(newNode, node)
        setTextUserDefinedNode(contentDom, "bill_zip", report.bill.zip.value!!)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='bill_city']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "bill_city"
            metaValueTypeAttribute = "string"
            newTextNode(report.bill.city.value)
        }
        node.parentNode.replaceChild(newNode, node)
        setTextUserDefinedNode(contentDom, "bill_city", report.bill.city.value!!)
    }

    private fun setProjectData(metaDom: OdfMetaDom, contentDom: OdfContentDom) {
        var node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='project_name']", metaDom, XPathConstants.NODE) as Node
        var newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "project_name"
            metaValueTypeAttribute = "string"
            newTextNode(report.project.name.value)
        }
        node.parentNode.replaceChild(newNode, node)
        setTextUserDefinedNode(contentDom, "project_name", report.project.name.value!!)

        node = metaDom.xPath.evaluate("//office:document-meta/office:meta/meta:user-defined[@meta:name='project_extra1']", metaDom, XPathConstants.NODE) as Node
        newNode = MetaUserDefinedElement(metaDom).apply {
            metaNameAttribute = "project_extra1"
            metaValueTypeAttribute = "string"
            newTextNode(report.project.extra1.value)
        }
        node.parentNode.replaceChild(newNode, node)
        setTextUserDefinedNode(contentDom, "project_extra1", report.project.extra1.value!!)
    }

    private fun fillTableHeads(table: OdfTable, heads: Array<String>) {
        heads.forEachIndexed { index, head ->
            val cell = table.getCellByPosition(index,0)
            cell.setDisplayText(head)
            cell.cellBackgroundColor = Color.SILVER
        }
    }

    private fun setWorkTime(contentDom: OdfContentDom) {
        val node = getParagraphOfTaggedNode(contentDom, "worktimetag")
        val parent = node.parentNode as OdfElement
        if(report.workTimeContainer.items.size == 0) {
            parent.removeChild(node)
            return
        }
        val table = OdfTable.newTable(parent, report.workTimeContainer.items.size, 6, 1, 0)
        fillTableHeads(table, arrayOf("Datum", "Mitarbeiter", "Arbeitsanfang", "Arbeitsende", "Fahrzeit [h:m]", "Fahrstrecke [km]", "Pause [h:m]", "Arbeitszeit [h:m]"))
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
            table.getCellByPosition(6, idx).setDisplayText(item.pauseDuration.value)
            table.getCellByPosition(7, idx).setDisplayText(item.workDuration.value)
            idx++
        }
        parent.insertBefore(table.odfElement.cloneNode(true), node)
        parent.removeChild(table.odfElement)
        parent.removeChild(node)
    }

    private fun setWorkItem(contentDom: OdfContentDom) {
        val node = getParagraphOfTaggedNode(contentDom, "workitemtag")
        val parent = node.parentNode as OdfElement
        if(report.workItemContainer.items.size == 0) {
            parent.removeChild(node)
            return
        }
        val table = OdfTable.newTable(parent, report.workItemContainer.items.size, 1, 1, 0)
        fillTableHeads(table, arrayOf("Arbeit"))
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
        val node = getParagraphOfTaggedNode(contentDom, "lumpsumtag")
        val parent = node.parentNode as OdfElement
        if(report.lumpSumContainer.items.size == 0) {
            parent.removeChild(node)
            return
        }
        val table = OdfTable.newTable(parent, report.lumpSumContainer.items.size, 3, 1, 0)
        fillTableHeads(table, arrayOf("Pauschale", "Anzahl", "Bemerkung"))
        var idx = 1
        for(item in report.lumpSumContainer.items) {
            table.getCellByPosition(0, idx).apply {
                setDisplayText(item.item.value)
                cellBackgroundColor = Color.WHITE
            }
            table.getCellByPosition(1, idx).apply {
                setDisplayText(item.amount.value.toString())
                cellBackgroundColor = Color.WHITE
            }
            table.getCellByPosition(2, idx).apply {
                setDisplayText(item.comment.value)
                cellBackgroundColor = Color.WHITE
            }
            idx++
        }
        parent.insertBefore(table.odfElement.cloneNode(true), node)
        parent.removeChild(table.odfElement)
        parent.removeChild(node)
    }


    private fun setMaterial(contentDom: OdfContentDom) {
        val node = getParagraphOfTaggedNode(contentDom, "materialtag")
        val parent = node.parentNode as OdfElement
        if(report.materialContainer.items.size == 0) {
            parent.removeChild(node)
            return
        }
        if(report.materialContainer.isAnyMaterialUnitSet()) {
            val table = OdfTable.newTable(parent, report.materialContainer.items.size, 3, 1, 0)
            fillTableHeads(table, arrayOf("Material", "Anzahl", "Einheit"))
            var idx = 1
            for(item in report.materialContainer.items) {
                table.getCellByPosition(0, idx).setDisplayText(item.item.value)
                table.getCellByPosition(1, idx).setDisplayText(item.amount.value.toString())
                table.getCellByPosition(2, idx).setDisplayText(item.unit.value.toString())
                idx++
            }
            parent.insertBefore(table.odfElement.cloneNode(true), node)
            parent.removeChild(table.odfElement)
            parent.removeChild(node)
        } else {
            val table = OdfTable.newTable(parent, report.materialContainer.items.size, 2, 1, 0)
            fillTableHeads(table, arrayOf("Material", "Anzahl"))
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
    }

    private fun setPhoto(contentDom: OdfContentDom) {
        val node = getTaggedNode(contentDom, "phototag")
        val parent = getParentParagraphOfNode(node) // this is the level we want the text:p to be located
        if(report.photoContainer.items.size == 0) {
            parent.removeChild(node)
            return
        }
        val container = parent.parentNode // this is one level above so the one that shall take the new text:p

        /* Hierarchy from outer to inner
         * container (existent)
         *   text:p -> variable name outerTextP
         *     draw:frame (anchor-type=paragraph) -> variable name outerDrawF
         *       draw:text-box -> variable name drawTextB
         *         text:p -> variable name innerTextP
         *           draw:frame (anchor-type=as-char) -> variable name innerDrawF
         *             draw:image (the jpg/png) -> variable name drawI
         *           newline
         *           caption (text node)
         */
        report.photoContainer.items.forEachIndexed { index, photoData ->
            val outerTextP = OdfTextParagraph(contentDom)
            val outerDrawF = outerTextP.newDrawFrameElement() as OdfDrawFrame
            outerDrawF.textAnchorTypeAttribute = "as-char"
            val drawTextB = outerDrawF.newDrawTextBoxElement()
            val innerTextP = drawTextB.newTextPElement() as OdfTextParagraph
            val innerDrawF = innerTextP.newDrawFrameElement() as OdfDrawFrame
            innerDrawF.svgWidthAttribute = "17cm"
            outerDrawF.svgWidthAttribute = "17cm"
            val ratio = photoData.imageHeight.toFloat() / photoData.imageWidth.toFloat()
            innerDrawF.svgHeightAttribute = "${17.0*ratio}cm"
            innerDrawF.textAnchorTypeAttribute = "as-char"
            outerDrawF.svgHeightAttribute = "${17.0*ratio}cm"
            val drawI = innerDrawF.newDrawImageElement() as OdfDrawImage
            drawI.setImagePath("Pictures/photo$index.jpg")
            outerTextP.newTextNode("${activity.getString(R.string.figure)} ${index + 1}: ${photoData.description.value}")
            outerTextP.newTextLineBreakElement()
            outerTextP.newTextSoftPageBreakElement()
            container.insertBefore(outerTextP, parent)
        }
        container.removeChild(parent)
    }

    private fun setSignatures(contentDom: OdfContentDom) {
        val style = contentDom.orCreateAutomaticStyles.newStyle(OdfStyleFamily.Graphic)
        style.setProperty(OdfGraphicProperties.Wrap, "none")
        style.setAttribute("style:name", "signatureStyle")
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
            drawImage.setImagePath("Pictures/clientSig.png")
            val pbreak = TextSoftPageBreakElement(contentDom)
            textP.appendChild(pbreak)
            container.insertBefore(textP, parent)
            container.removeChild(parent)
        }

        if(employeeSigFile != null) {
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

    private fun setTextUserDefinedNode(contentDom: OdfContentDom, fieldName: String, value: String) {
        try {
            val node = contentDom.xPath.evaluate("//text:user-defined[@text:name='${fieldName}']", contentDom, XPathConstants.NODE) as Node
            node.textContent = value
        } catch(e: Exception) {}
    }
}