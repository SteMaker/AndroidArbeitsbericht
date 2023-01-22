package com.stemaker.arbeitsbericht.helpers
import android.util.Base64
import com.stemaker.arbeitsbericht.data.calendarToDateString
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.data.report.ReportData
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val TAG = "HtmlReport"

class HtmlReport(
    private val prefs: AbPreferences,
    private val report: ReportData,
    private val dir: File)
{

    private fun readFileToBytes(f: File): ByteArray {
        val size = f.length().toInt()
        val bytes = ByteArray(size)
        val tmpBuff = ByteArray(size)
        val fis= FileInputStream(f)
        try {
            var read = fis.read(bytes, 0, size)
            if (read < size) {
                var remain = size - read
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain)
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                    remain -= read
                }
            }
        }  catch (e: IOException){
            throw e
        } finally {
            fis.close()
        }
        return bytes
    }

    private fun alignmentToCss(a: AbPreferences.Alignment): String {
       return when(a) {
           AbPreferences.Alignment.CENTER -> "margin-left:auto;margin-right:auto;display:block;"
           AbPreferences.Alignment.LEFT -> "margin-right:auto;display:block;"
           AbPreferences.Alignment.RIGHT -> "margin-left:auto;display:block;"
       }
    }
    private fun logoCssClass(): String {
        return ".logo {" +
                "height:${prefs.pdfLogoWidthPercent}%;" +
                "width: ${prefs.pdfLogoWidthPercent}%;" +
                alignmentToCss(prefs.pdfLogoAlignment.value) +
                "}"
    }

    private fun footerCssClass(): String {
        return ".footer {" +
                "height:${prefs.pdfFooterWidthPercent}%;" +
                "width: ${prefs.pdfFooterWidthPercent}%;" +
                alignmentToCss(prefs.pdfFooterAlignment.value) +
                "}"
    }

    fun encodeReport(inclSignatures: Boolean = true): String {
        val fs = "font-size:${prefs.fontSize}px"
        var html: String =
            "<!DOCTYPE html>" +
                    "<html lang=\"de\">" +
                    "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head>" +
                    "<style type=\"text/css\">" +
                    ".nobreak {" +
                    "page-break-inside: avoid;" +
                    "}" +
                    logoCssClass() +
                    footerCssClass() +
                    "</style>" +
                    "<body>"
        if(prefs.logoFile.value != "" && prefs.pdfUseLogo.value) {
            val logoFileContent = readFileToBytes(File(dir, prefs.logoFile.value))
            val logo = Base64.encodeToString(logoFileContent, Base64.DEFAULT)
            html += "<img src=\"data:image/jpg;base64,${logo}\" class=\"logo\"/>"
        }
        html +=     "<h1>Arbeitsbericht Nr. ${report.id.value}</h1>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Kunde / Projekt</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Rechnungsadresse</th>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${report.project.name.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">${report.bill.name.value}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${report.project.extra1.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">${report.bill.street.value}</td>" +
                    "</tr>" +
                    "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"></td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">${report.bill.zip.value} ${report.bill.city.value}</td>" +
                    "</tr>" +
                "</table><hr>"


        // Work times table
        if(report.workTimeContainer.items.size > 0) {
            html += "<div class=\"nobreak\">"
            html += "<h2>Arbeits-/fahrzeiten und Fahrstrecken</h2>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Datum</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Mitarbeiter</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeits-anfang</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeits-ende</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahrzeit<br>[h:m]</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Fahr-strecke [km]</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Pause<br>[h:m]</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeits-zeit [h:m]</th>" +
                    "</tr>"
            report.workTimeContainer.items.forEach {
                html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${calendarToDateString(it.date.value)}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\">"
                for (empl in it.employees)
                    html += "${empl.value!!}<br>"
                html += "</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.startTime.value} Uhr</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.endTime.value} Uhr</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.driveTime.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.distance.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.pauseDuration.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.workDuration.value}</td>" +
                        "</tr>"
            }
            html += "</table><hr>"
            html += "</div>"
        }

        // Work items table
        if(report.workItemContainer.items.size > 0) {
            html += "<div class=\"nobreak\">"
            html += "<h2>Durchgeführte Arbeiten</h2>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Arbeit</th>" +
                    "</tr>"
            report.workItemContainer.items.forEach {
                html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "</tr>"
            }
            html += "</table><hr>"
            html += "</div>"
        }

        if(report.lumpSumContainer.items.size > 0) {
            // Lump sums
            html += "<div class=\"nobreak\">"
            html += "<h2>Pauschalen</h2>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">" +
                    "<tr>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Pauschale</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                    "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Bemerkung</th>" +
                    "</tr>"
            report.lumpSumContainer.items.forEach {
                html += "<tr>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                        "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.comment.value}</td>" +
                        "</tr>"
            }
            html += "</table><hr>"
            html += "</div>"
        }

        // Material table
        if(report.materialContainer.items.size > 0) {
            html += "<div class=\"nobreak\">"
            html += "<h2>Material</h2>" +
                    "<table style=\"border: 2px solid black;border-collapse: collapse;\">"
            if (report.materialContainer.isAnyMaterialUnitSet()) {
                html += "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Material</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Einheit</th>" +
                        "</tr>"
                report.materialContainer.items.forEach {
                    html += "<tr>" +
                            "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                            "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                            "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.unit.value}</td>" +
                            "</tr>"
                }
            } else {
                html += "<tr>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Material</th>" +
                        "<th style=\"padding: 15px;$fs;text-align:left;border: 2px solid black;border-collapse: collapse;\">Anzahl</th>" +
                        "</tr>"
                report.materialContainer.items.forEach {
                    html += "<tr>" +
                            "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.item.value}</td>" +
                            "<td style=\"padding: 10px;$fs;border: 2px solid black;border-collapse: collapse;\"> ${it.amount.value}</td>" +
                            "</tr>"
                }
            }
            html += "</table><hr>"
            html += "</div>"
        }

        if(report.photoContainer.items.size != 0) {
            html += "<h2>Fotos</h2>"
            report.photoContainer.items.forEach {
                val fileName = File(it.file.value!!).name // old version stored full path
                html += "<div class=\"nobreak\">"
                html += "<img src=\"content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtPhotos/$fileName\" style=\"max-width:100%\">"
                html += "${it.description.value}"
                html += "</div>"
            }
            html += "<hr>"
        }
        if(inclSignatures) {
            // Employee signature
            html += "<div class=\"nobreak\">"
            html += "<h2>Unterschriften</h2>"
            if (report.signatureData.clientSignatureSvg.value == "" && report.signatureData.employeeSignatureSvg.value == "") {
                html += "Keine Unterschriften vorhanden<hr>"
            } else {
                html += "<table>" +
                        "<tr>" +
                        "<th>Auftragnehmer</th>" +
                        "<th>Auftraggeber</th>" +
                        "</tr>" +
                        "<tr>"
                if(report.signatureData.employeeSignatureSvg.value != "" && report.signatureData.employeeSignaturePngFile != null)
                    html += "<th><img src=\"content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtSignatures/${report.signatureData.employeeSignaturePngFile!!.name}\" style=\"max-width:50%\"></th>"
                else
                    html += "<th>Keine Unterschrift</th>"
                if(report.signatureData.clientSignatureSvg.value != "" && report.signatureData.clientSignaturePngFile != null)
                    html += "<th><img src=\"content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtSignatures/${report.signatureData.clientSignaturePngFile!!.name}\" style=\"max-width:50%\"></th>"
                else
                    html += "<th>Keine Unterschrift</th>"
                html += "</tr>" +
                        "</table><hr>"
            }
            html += "</div>"
        }
        if(prefs.footerFile.value != "" && prefs.pdfUseFooter.value) {
            val footerFileContent = readFileToBytes(File(dir, prefs.footerFile.value))
            val footer = Base64.encodeToString(footerFileContent, Base64.DEFAULT)
            html += "<img src=\"data:image/jpg;base64,${footer}\" class=\"footer\"/>"
        }

        html += "</body></html>"

        return html
    }
}
