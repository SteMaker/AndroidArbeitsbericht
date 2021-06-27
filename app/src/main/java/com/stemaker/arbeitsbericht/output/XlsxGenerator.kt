package com.stemaker.arbeitsbericht

import android.app.Activity
import android.os.Environment
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.output.ReportGenerator
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.util.IOUtils
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.math.roundToInt

private const val TAG = "XlsxGenerator"

class XlsxGenerator(activity: Activity, report: ReportData, progressBar: ProgressBar?, textView: TextView?):
    ReportGenerator(activity, report, progressBar, textView) {

    enum class StyleTypes { HEAD1, HEAD2, HEAD3, NORMAL,
        TABLEHEAD_LEFT, TABLEHEAD_MIDDLE, TABLEHEAD_RIGHT, TABLEHEAD_1COL,
        TABLEBOTTOM_LEFT, TABLEBOTTOM_MIDDLE, TABLEBOTTOM_RIGHT, TABLEBOTTOM_1COL,
        TABLE_LEFT, TABLE_MIDDLE, TABLE_RIGHT, TABLE_1COL
    }
    val styles = mutableMapOf<StyleTypes, CellStyle>()
    class TableCoordinates(val startRow: Int, val endRow: Int, val startCol: Int, val endCol: Int)
    override val filePostFixExt: Array<Pair<String, String>>
        get() = arrayOf(Pair("", "xlsx"))

    // TODO: Find out how to store hash in xlsx custom data to prevent unneeded recreation of file
    override fun getHash(files: Array<File>): String? = null

    override fun createDoc(files: Array<File>, done: (success: Boolean) -> Unit) {
        val xlsxFile = files[0]
        val clientSigFile = files[1]
        val employeeSigFile = files[2]

        val wb = XSSFWorkbook()
        createStyles(wb)
        val sheetNameGeneral = WorkbookUtil.createSafeSheetName("Allgemein")
        val sheetGeneral = wb.createSheet(sheetNameGeneral);
        val sheetNameData = WorkbookUtil.createSafeSheetName("Daten")
        val sheetData = wb.createSheet(sheetNameData);
        var rown = 0
        sheetGeneral.setColumnWidth(0, (12.5f*256).toInt())

        if(configuration().logoFile != "" && configuration().xlsxUseLogo)
            rown = rown.plus(setLogo(wb, sheetGeneral, rown)+1)
        rown = rown.plus(setHeadline(sheetGeneral, rown)+1)
        rown = rown.plus(setBaseData(sheetGeneral, rown)+1)
        rown =  rown.plus(setBillData(sheetGeneral, rown)+1)
        rown = rown.plus(setSignatures(wb, sheetGeneral, rown+1, clientSigFile, employeeSigFile)+1)
        if(configuration().footerFile != "" && configuration().xlsxUseFooter)
            rown = rown.plus(setFooter(wb, sheetGeneral, rown)+1)

        rown = setWorkTime(sheetData, 0) + 1
        rown = rown.plus(setWorkItem(sheetData, rown)+1)
        rown = rown.plus(setLumpSum(sheetData, rown)+1)
        rown = rown.plus(setMaterial(sheetData, rown)+1)
        setPhoto(wb)

        val colWidths = doubleArrayOf(12.5, 15.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0,)
        colWidths.forEachIndexed { index, width ->
            sheetData.setColumnWidth(index, (width*256).toInt())
        }
        try {
            val fileOut = FileOutputStream(xlsxFile)
            wb.write(fileOut)
            done(true)
        } catch (e:FileNotFoundException) {
            Log.e(TAG, "Failed to open xlsx file")
            done(false)
        }
    }

    private fun createStyles(wb: XSSFWorkbook) {
        styles.put(StyleTypes.HEAD1, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(16*20)
            font.bold = true
            it.setFont(font)
        })
        styles.put(StyleTypes.HEAD2, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(14*20)
            font.bold = true
            it.setFont(font)
        })
        styles.put(StyleTypes.HEAD3, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            font.bold = true
            it.setFont(font)
            it.wrapText = true
        })
        styles.put(StyleTypes.NORMAL, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
        })
        styles.put(StyleTypes.TABLEHEAD_LEFT, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            font.bold = true
            it.setFont(font)
            it.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            it.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            it.setBorderTop(BorderStyle.THICK)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THICK)
            it.setBorderRight(BorderStyle.THIN)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEHEAD_MIDDLE, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            font.bold = true
            it.setFont(font)
            it.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            it.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            it.setBorderTop(BorderStyle.THICK)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THIN)
            it.setBorderRight(BorderStyle.THIN)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEHEAD_RIGHT, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            font.bold = true
            it.setFont(font)
            it.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            it.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            it.setBorderTop(BorderStyle.THICK)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THIN)
            it.setBorderRight(BorderStyle.THICK)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEHEAD_1COL, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            font.bold = true
            it.setFont(font)
            it.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            it.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            it.setBorderTop(BorderStyle.THICK)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THICK)
            it.setBorderRight(BorderStyle.THICK)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEBOTTOM_LEFT, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THICK)
            it.setBorderLeft(BorderStyle.THICK)
            it.setBorderRight(BorderStyle.THIN)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEBOTTOM_MIDDLE, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THICK)
            it.setBorderLeft(BorderStyle.THIN)
            it.setBorderRight(BorderStyle.THIN)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEBOTTOM_RIGHT, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THICK)
            it.setBorderLeft(BorderStyle.THIN)
            it.setBorderRight(BorderStyle.THICK)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLEBOTTOM_1COL, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THICK)
            it.setBorderLeft(BorderStyle.THICK)
            it.setBorderRight(BorderStyle.THICK)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLE_LEFT, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THICK)
            it.setBorderRight(BorderStyle.THIN)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLE_MIDDLE, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THIN)
            it.setBorderRight(BorderStyle.THIN)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLE_RIGHT, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THIN)
            it.setBorderRight(BorderStyle.THICK)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
        styles.put(StyleTypes.TABLE_1COL, wb.createCellStyle().also {
            val font = wb.createFont()
            font.setFontHeight(12*20)
            it.setFont(font)
            it.setBorderTop(BorderStyle.THIN)
            it.setBorderBottom(BorderStyle.THIN)
            it.setBorderLeft(BorderStyle.THICK)
            it.setBorderRight(BorderStyle.THICK)
            it.setTopBorderColor(IndexedColors.BLACK.index)
            it.setBottomBorderColor(IndexedColors.BLACK.index)
            it.setLeftBorderColor(IndexedColors.BLACK.index)
            it.setRightBorderColor(IndexedColors.BLACK.index)
            it.wrapText = true
        })
    }

    private fun setLogo(wb: XSSFWorkbook, sheet: XSSFSheet, rown: Int): Int {
        // The factor of 1.1 is found by trail and error :(
        val logoWidth = (configuration().xlsxLogoWidth * Units.EMU_PER_CENTIMETER / 10f / 1.1f).roundToInt() // in EMU
        var width = 0
        var columns = 0
        while(width < logoWidth) {
            width += Units.columnWidthToEMU(sheet.getColumnWidth(columns))
            columns++
        }
        val dx2 = logoWidth-width
        try {
            val iStream = FileInputStream("${activity.filesDir}/${configuration().logoFile}")
            val bytes = IOUtils.toByteArray(iStream)
            val picIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
            iStream.close()
            val helper = wb.creationHelper
            val drawing = sheet.createDrawingPatriarch()
            val anchor = helper.createClientAnchor()
            anchor.anchorType = ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE
            val row = sheet.createRow(rown)
            anchor.setRow1(rown)
            anchor.setRow2(rown + 1)
            anchor.setCol1(0)
            anchor.setCol2(columns)
            anchor.dx2 = dx2
            val picture = drawing.createPicture(anchor, picIdx)
            val rowHeight = (logoWidth * 20 / configuration().logoRatio)/Units.EMU_PER_POINT
            row.height = rowHeight.toInt().toShort()
            return 1
        } catch(e: Exception) {}
        return 0
    }
    private fun setFooter(wb: XSSFWorkbook, sheet: XSSFSheet, rown: Int): Int {
            // The factor of 1.1 is found by trail and error :(
            val footerWidth = (configuration().xlsxFooterWidth * Units.EMU_PER_CENTIMETER / 10f / 1.1f).roundToInt() // in EMU
            var width = 0
            var columns = 0
            while(width < footerWidth) {
                width += Units.columnWidthToEMU(sheet.getColumnWidth(columns))
                columns++
            }
            val dx2 = footerWidth-width
            try {
                val iStream = FileInputStream("${activity.filesDir}/${configuration().footerFile}")
                val bytes = IOUtils.toByteArray(iStream)
                val picIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
                iStream.close()
                val helper = wb.creationHelper
                val drawing = sheet.createDrawingPatriarch()
                val anchor = helper.createClientAnchor()
                anchor.anchorType = ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE
                val row = sheet.createRow(rown)
                anchor.setRow1(rown)
                anchor.setRow2(rown + 1)
                anchor.setCol1(0)
                anchor.setCol2(columns)
                anchor.dx2 = dx2
                val picture = drawing.createPicture(anchor, picIdx)
                val rowHeight = (footerWidth * 20 / configuration().footerRatio)/Units.EMU_PER_POINT
                row.height = rowHeight.toInt().toShort()
                return 1
            } catch(e: Exception) {}
        return 0
    }

    private fun setHeadline(sheet: XSSFSheet, rown: Int): Int {
        /* Headline */
        val row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD1])
            it.setCellValue("Arbeitsbericht ${report.id.value} vom ${report.create_date.value}")
        }
        return 1
    }

    private fun setBaseData(sheet: XSSFSheet, rown: Int): Int {
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Projekt")
        }
        // Projektname
        row = sheet.createRow(rown+1)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD3])
            it.setCellValue("Projektname")
        }
        row.createCell(1).also {
            it.setCellStyle(styles[StyleTypes.NORMAL])
            it.setCellValue("${report.project.name.value}")
        }
        // Projektzusatz
        row = sheet.createRow(rown+2)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD3])
            it.setCellValue("Projekt-zusatz")
        }
        row.createCell(1).also {
            it.setCellStyle(styles[StyleTypes.NORMAL])
            it.setCellValue("${report.project.extra1.value}")
        }
        // Berichtsnummer
        row = sheet.createRow(rown+3)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD3])
            it.setCellValue("Berichts-nummer")
        }
        row.createCell(1).also {
            it.setCellStyle(styles[StyleTypes.NORMAL])
            it.setCellValue(report.id.value)
        }
        // Erstellungsdatum
        row = sheet.createRow(rown+4)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD3])
            it.setCellValue("Erstellungs-datum")
        }
        row.createCell(1).also {
            it.setCellStyle(styles[StyleTypes.NORMAL])
            it.setCellValue("${report.create_date.value}")
        }
        return 5
    }

    private fun setBillData(sheet: XSSFSheet, rown: Int): Int {
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Rechnungsadresse")
        }
        val rows = arrayOf(Pair("Name", report.bill.name.value!!),
            Pair("Strasse+Nr.", report.bill.street.value!!),
            Pair("PLZ", report.bill.zip.value!!),
            Pair("Ort", report.bill.city.value!!),
        )
        rows.forEachIndexed { index, pair ->
            row = sheet.createRow(rown+1+index)
            row.createCell(0).also {
                it.setCellStyle(styles[StyleTypes.NORMAL])
                it.setCellValue(pair.first)
            }
            row.createCell(1).also {
                it.setCellStyle(styles[StyleTypes.NORMAL])
                it.setCellValue(pair.second)
            }
        }
        return 5
    }

    private fun fillTableHeads(row: XSSFRow, heads: Array<String>) {
        heads.forEachIndexed { index, head ->
            row.createCell(index).also {
                it.setCellStyle(selectTableStyle(TableCoordinates(0, 0, 0, heads.size-1), 0, index))
                it.setCellValue(head)
            }
        }
    }

    private fun selectTableStyle(table: TableCoordinates, row: Int, col: Int): CellStyle? {
        when(row) {
            table.startRow -> {
                if(table.startCol == table.endCol)
                    return styles[StyleTypes.TABLEHEAD_1COL]
                return when (col) {
                    table.startCol -> styles[StyleTypes.TABLEHEAD_LEFT]
                    table.endCol -> styles[StyleTypes.TABLEHEAD_RIGHT]
                    else -> styles[StyleTypes.TABLEHEAD_MIDDLE]
                }
            }
            table.endRow -> {
                if(table.startCol == table.endCol)
                    return styles[StyleTypes.TABLEBOTTOM_1COL]
                return when (col) {
                    table.startCol -> styles[StyleTypes.TABLEBOTTOM_LEFT]
                    table.endCol -> styles[StyleTypes.TABLEBOTTOM_RIGHT]
                    else -> styles[StyleTypes.TABLEBOTTOM_MIDDLE]
                }
            }
            else -> {
                if(table.startCol == table.endCol)
                    return styles[StyleTypes.TABLE_1COL]
                return when (col) {
                    table.startCol -> styles[StyleTypes.TABLE_LEFT]
                    table.endCol -> styles[StyleTypes.TABLE_RIGHT]
                    else -> styles[StyleTypes.TABLE_MIDDLE]
                }
            }
        }
    }

    private fun setWorkTime(sheet: XSSFSheet, startRow: Int): Int {
        val rown = startRow
        val table = TableCoordinates(startRow+1, startRow+1+report.workTimeContainer.items.size, 0, 7)
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Arbeits- / Fahrzeiten und Fahrstrecken")
        }

        row = sheet.createRow(rown+1)
        fillTableHeads(row, arrayOf("Datum", "Mitarbeiter", "Arbeits-anfang", "Arbeits-ende", "Fahrzeit [h:m]", "Fahr-strecke [km]", "Pause [h:m]", "Arbeitszeit [h:m]"))
        report.workTimeContainer.items.forEachIndexed { rowIdx, item ->
            row = sheet.createRow(rown+2+rowIdx)
            var employees = ""
            for(e in item.employees) employees += "${e.value}\n"
            val columns = arrayOf(item.date.value, employees, item.startTime.value, item.endTime.value, item.driveTime.value, item.distance.value.toString(), item.pauseDuration.value, item.workDuration.value)
            columns.forEachIndexed { colIdx, e ->
                row.createCell(colIdx).also {
                    it.setCellStyle(selectTableStyle(table, startRow+2+rowIdx, colIdx))
                    it.setCellValue(e)
                }
            }
        }
        return 2+report.workTimeContainer.items.size
    }

    private fun setWorkItem(sheet: XSSFSheet, startRow: Int): Int {
        val rown = startRow
        val table = TableCoordinates(startRow+1, startRow+1+report.workItemContainer.items.size, 0, 0)
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Durchgeführte Arbeiten")
        }
        row = sheet.createRow(rown+1)
        fillTableHeads(row, arrayOf("Arbeit"))
        report.workItemContainer.items.forEachIndexed { rowIdx, item ->
            row = sheet.createRow(rown+2+rowIdx)
            row.createCell(0).also {
                it.setCellStyle(selectTableStyle(table, startRow+2+rowIdx, 0))
                it.setCellValue(item.item.value)
            }
        }
        return 2+report.workItemContainer.items.size
    }

    private fun setLumpSum(sheet: XSSFSheet, startRow: Int): Int {
        val rown = startRow
        val table = TableCoordinates(startRow+1, startRow+1+report.lumpSumContainer.items.size, 0, 2)
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Pauschalen")
        }
        row = sheet.createRow(rown+1)
        fillTableHeads(row, arrayOf("Pauschale", "Bemerkung", "Anzahl"))
        report.lumpSumContainer.items.forEachIndexed { rowIdx, item ->
            row = sheet.createRow(rown+2+rowIdx)
            val columns = arrayOf(item.item.value, item.comment.value, item.amount.value.toString())
            columns.forEachIndexed { colIdx, e ->
                row.createCell(colIdx).also {
                    it.setCellStyle(selectTableStyle(table, startRow+2+rowIdx, colIdx))
                    it.setCellValue(e)
                }
            }
        }
        return 2+report.lumpSumContainer.items.size
    }

    private fun setMaterial(sheet: XSSFSheet, startRow: Int): Int {
        val rown = startRow
        val table = TableCoordinates(startRow+1, startRow+1+report.materialContainer.items.size, 0, 1)
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Material")
        }
        row = sheet.createRow(rown+1)
        fillTableHeads(row, arrayOf("Material", "Anzahl"))
        report.materialContainer.items.forEachIndexed { rowIdx, item ->
            row = sheet.createRow(rown+2+rowIdx)
            val columns = arrayOf(item.item.value, item.amount.value.toString())
            columns.forEachIndexed { colIdx, e ->
                row.createCell(colIdx).also {
                    it.setCellStyle(selectTableStyle(table, startRow+2+rowIdx, colIdx))
                    it.setCellValue(e)
                }
            }
        }
        return 2+report.materialContainer.items.size
    }

    private fun setPhoto(wb: XSSFWorkbook) {
        report.photoContainer.items.forEachIndexed { index, photoData ->
            photoData.file.value?.also {
                try {
                    val tmpFile = File(it) // because old app version stored the path here as well
                    val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), tmpFile.name)
                    val safeName = WorkbookUtil.createSafeSheetName("Foto $index")
                    val sheet = wb.createSheet(safeName)
                    val iStream = FileInputStream(file)
                    val bytes = IOUtils.toByteArray(iStream)
                    val picIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG)
                    iStream.close()

                    val helper = wb.creationHelper
                    val drawing = sheet.createDrawingPatriarch()

                    var row = sheet.createRow(0)
                    val ratio = photoData.imageHeight.toFloat() / photoData.imageWidth.toFloat()
                    sheet.setColumnWidth(0, 60*256)
                    val widthInPt = Units.columnWidthToEMU(60*256) / Units.EMU_PER_POINT
                    row.height = (widthInPt*ratio*20f).toInt().toShort()

                    val anchor = helper.createClientAnchor()
                    anchor.setCol1(0)
                    anchor.setCol2(1)
                    anchor.setRow1(0)
                    anchor.setRow2(1)

                    val picture = drawing.createPicture(anchor, picIdx)
                    //picture.resize(1.0, 1.0)

                    row = sheet.createRow(1)
                    row.createCell(0).also { cell ->
                        cell.setCellValue(photoData.description.value)
                    }
                } catch(e: Exception) {
                    Log.e(TAG, "Could not add picture $it")
                }
            }
        }
    }

    private fun addSig(wb: XSSFWorkbook, sheet: XSSFSheet, rown: Int, sigFile: File) {
        val iStream = FileInputStream(sigFile)
        val bytes = IOUtils.toByteArray(iStream)
        val picIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
        iStream.close()
        val helper = wb.creationHelper
        val drawing = sheet.createDrawingPatriarch()
        val anchor = helper.createClientAnchor()
        anchor.setRow1(rown)
        anchor.setRow2(rown+2)
        anchor.setCol1(0)
        anchor.setCol2(2)
        val picture = drawing.createPicture(anchor, picIdx)
    }

    private fun setSignatures(wb: XSSFWorkbook, sheet: XSSFSheet, startRow: Int, clientSigFile: File, employeeSigFile: File): Int {
        val rown = startRow
        // Headline
        var row = sheet.createRow(rown)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Unterschrift Auftraggeber")
        }
        addSig(wb, sheet, rown+1, clientSigFile)

        // Headline
        row = sheet.createRow(rown+4)
        row.createCell(0).also {
            it.setCellStyle(styles[StyleTypes.HEAD2])
            it.setCellValue("Unterschrift Auftragnehmer")
        }
        addSig(wb, sheet, rown+5, employeeSigFile)

        return 7
    }
}